package com.example.ui

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL

data class CastDevice(
    val name: String,
    val ip: String,
    val location: String, // XML description URL or connection endpoint
    val type: String // "FireTV", "Cast", "SmartTV"
)

object CastingManager {
    private const val TAG = "CastingManager"
    private const val SSDP_IP = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val SSDP_TIMEOUT_MS = 2500

    private val _discoveredDevices = MutableStateFlow<List<CastDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<CastDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var multicastLock: WifiManager.MulticastLock? = null

    /**
     * Discovers Smart TVs, Fire TVs, and Google Cast devices on the local Wi-Fi.
     * Uses a high-speed hybrid engine:
     * 1. Concurrent Subnet Port Sweep (bypasses router multicast isolation).
     * 2. UDP SSDP/DIAL Multicast Query.
     */
    suspend fun discoverDevices(context: Context) = withContext(Dispatchers.IO) {
        if (_isScanning.value) return@withContext
        _isScanning.value = true
        Log.d(TAG, "Starting hybrid device discovery (Subnet Port Sweep + SSDP)...")

        val currentDevices = _discoveredDevices.value.toMutableList()

        try {
            coroutineScope {
                // 1. High-Speed Subnet Port Sweep
                launch {
                    val prefix = getLocalSubnetPrefix(context)
                    Log.d(TAG, "Probing local subnet: ${prefix}0/24")

                    // Priority targets: Danny's Fire TV, saved IP, and active smart devices
                    val priorityIps = listOf("192.168.86.202", "${prefix}202", "${prefix}29", "${prefix}38", "${prefix}214").distinct()
                    priorityIps.map { ip ->
                        async { probeDeviceOnSubnet(ip) }
                    }.awaitAll().filterNotNull().forEach { dev ->
                        synchronized(currentDevices) {
                            if (currentDevices.none { it.ip == dev.ip }) {
                                currentDevices.add(dev)
                                _discoveredDevices.value = currentDevices.toList()
                                Log.d(TAG, "Priority discovered: ${dev.name} at ${dev.ip} (${dev.type})")
                            }
                        }
                    }

                    val allIps = (1..254).map { "$prefix$it" }.filterNot { priorityIps.contains(it) }
                    // Process in chunks of 32 concurrent probes
                    allIps.chunked(32).forEach { batch ->
                        batch.map { ip ->
                            async {
                                probeDeviceOnSubnet(ip)
                            }
                        }.awaitAll().filterNotNull().forEach { dev ->
                            synchronized(currentDevices) {
                                if (currentDevices.none { it.ip == dev.ip }) {
                                    currentDevices.add(dev)
                                    _discoveredDevices.value = currentDevices.toList()
                                    Log.d(TAG, "Discovered device on subnet: ${dev.name} at ${dev.ip} (${dev.type})")
                                }
                            }
                        }
                    }
                }

                // 2. Parallel SSDP Multicast
                launch {
                    discoverViaSsdp(context, currentDevices)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Discovery error: ${e.message}")
        } finally {
            _isScanning.value = false
            Log.d(TAG, "Device discovery completed. Found ${currentDevices.size} total device(s).")
        }
    }

    private fun probeDeviceOnSubnet(ip: String): CastDevice? {
        val hasAdb = isPortOpen(ip, 5555, 600)
        val hasCastPort = isPortOpen(ip, 8009, 600)
        val hasDialPort = isPortOpen(ip, 8008, 600)
        val hasCompanion = isPortOpen(ip, 8998, 600)

        // 1. Probe Fire TV ADB Port 5555
        if (hasAdb) {
            val name = if (ip == "192.168.86.202") "Danny's Fire TV" else "Amazon Fire TV ($ip)"
            Log.d(TAG, "Identified Fire TV via ADB port 5555: $name ($ip)")
            return CastDevice(
                name = name,
                ip = ip,
                location = "adb://$ip:5555",
                type = "FireTV"
            )
        }

        // 2. Probe Fire TV Companion Receiver Port 8998
        if (hasCompanion) {
            val name = if (ip == "192.168.86.202") "Danny's Fire TV" else "Fire TV Companion ($ip)"
            Log.d(TAG, "Identified Fire TV via Companion port 8998: $name ($ip)")
            return CastDevice(
                name = name,
                ip = ip,
                location = "http://$ip:8998",
                type = "FireTV"
            )
        }

        // 3. Known Fire TV responding on Cast port 8009
        if (ip == "192.168.86.202" && hasCastPort) {
            Log.d(TAG, "Identified Danny's Fire TV via port 8009")
            return CastDevice(
                name = "Danny's Fire TV",
                ip = ip,
                location = "http://$ip:8009",
                type = "FireTV"
            )
        }

        // 4. Probe Google Cast / DIAL Port 8008 or 8009
        if (hasDialPort || hasCastPort) {
            var friendlyName: String? = null
            if (hasDialPort) {
                try {
                    val url = URL("http://$ip:8008/setup/eureka_info")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 500
                    conn.readTimeout = 500
                    if (conn.responseCode == 200) {
                        val json = conn.inputStream.bufferedReader().readText()
                        val match = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").find(json)
                        if (match != null) {
                            friendlyName = match.groupValues[1]
                        }
                    }
                } catch (e: Exception) {
                    // Ignore eureka failure
                }
            }

            // Fallback to DIAL / SSDP xml
            if (friendlyName == null) {
                try {
                    val url = URL("http://$ip:8008/ssdp/device-desc.xml")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 500
                    conn.readTimeout = 500
                    if (conn.responseCode == 200) {
                        val xml = conn.inputStream.bufferedReader().readText()
                        val match = Regex("<friendlyName>(.*?)</friendlyName>", RegexOption.IGNORE_CASE).find(xml)
                        if (match != null) {
                            friendlyName = match.groupValues[1]
                        }
                    }
                } catch (e: Exception) {
                    // Ignore xml failure
                }
            }

            return CastDevice(
                name = friendlyName ?: "Smart TV / Cast ($ip)",
                ip = ip,
                location = "http://$ip:8008",
                type = "Cast"
            )
        }

        return null
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun discoverViaSsdp(context: Context, currentDevices: MutableList<CastDevice>) {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (multicastLock == null && wifi != null) {
            multicastLock = wifi.createMulticastLock("StreamwiseCastingLock")
        }

        try {
            multicastLock?.acquire()
        } catch (e: Exception) {
            Log.w(TAG, "MulticastLock acquisition warning: ${e.message}")
        }

        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = SSDP_TIMEOUT_MS

            val query = """
                M-SEARCH * HTTP/1.1
                HOST: $SSDP_IP:$SSDP_PORT
                MAN: "ssdp:discover"
                MX: 2
                ST: ssdp:all
                
            """.trimIndent().replace("\n", "\r\n") + "\r\n"

            val group = InetAddress.getByName(SSDP_IP)
            val packet = DatagramPacket(query.toByteArray(), query.length, group, SSDP_PORT)
            socket.send(packet)

            val receiveBuffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < SSDP_TIMEOUT_MS) {
                try {
                    val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                    socket.receive(receivePacket)

                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val hostAddr = receivePacket.address?.hostAddress ?: ""
                    val device = parseSsdpResponse(response, hostAddr)

                    if (device != null) {
                        synchronized(currentDevices) {
                            if (currentDevices.none { it.ip == device.ip }) {
                                currentDevices.add(device)
                                _discoveredDevices.value = currentDevices.toList()
                                Log.d(TAG, "SSDP Discovered: ${device.name} at ${device.ip}")
                            }
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SSDP Discovery error: ${e.message}")
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Failed closing socket: ${e.message}")
            }
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock?.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed releasing multicastLock: ${e.message}")
            }
        }
    }

    private fun parseSsdpResponse(response: String, ip: String): CastDevice? {
        val lines = response.lines()
        val locationLine = lines.find { it.startsWith("LOCATION:", ignoreCase = true) } ?: return null
        val location = locationLine.substring(locationLine.indexOf(':') + 1).trim()
        val st = lines.find { it.startsWith("ST:", ignoreCase = true) }?.substringAfter(":")?.trim() ?: ""
        val server = lines.find { it.startsWith("SERVER:", ignoreCase = true) }?.substringAfter(":")?.trim() ?: ""

        val isFireTv = response.contains("Amazon", ignoreCase = true) ||
                response.contains("FireTV", ignoreCase = true) ||
                response.contains("AFT", ignoreCase = true) ||
                server.contains("Amazon", ignoreCase = true)

        val isDial = st.contains("dial-multiscreen", ignoreCase = true) || location.contains("dial", ignoreCase = true)
        val isRenderer = st.contains("device:MediaRenderer", ignoreCase = true)

        if (!isFireTv && !isDial && !isRenderer) {
            return null
        }

        var friendlyName = when {
            isFireTv -> if (ip == "192.168.86.202") "Danny's Fire TV" else "Amazon Fire TV ($ip)"
            isDial -> "Smart TV ($ip)"
            else -> "Smart TV / Media Player"
        }

        try {
            if (location.startsWith("http")) {
                val url = URL(location)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 600
                conn.readTimeout = 600
                conn.requestMethod = "GET"
                if (conn.responseCode == 200) {
                    val xml = conn.inputStream.bufferedReader().readText()
                    val match = Regex("<friendlyName>(.*?)</friendlyName>", RegexOption.IGNORE_CASE).find(xml)
                    if (match != null) {
                        friendlyName = match.groupValues[1]
                    }
                }
            }
        } catch (e: Exception) {
            // Keep fallback name
        }

        return CastDevice(name = friendlyName, ip = ip, location = location, type = if (isFireTv) "FireTV" else "SmartTV")
    }

    private fun getLocalSubnetPrefix(context: Context): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            val lastDot = host.lastIndexOf('.')
                            if (lastDot > 0) {
                                return host.substring(0, lastDot + 1)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving subnet prefix: ${e.message}")
        }
        return "192.168.86." // default fallback for current subnet
    }

    /**
     * Placeholder for launching a URL on a discovered device.
     */
    suspend fun castUrl(device: CastDevice, url: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Casting URL: $url to ${device.name} (${device.ip})")
    }
}
