package com.example.ui

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

data class CastDevice(
    val name: String,
    val ip: String,
    val location: String, // XML description URL
    val type: String
)

object CastingManager {
    private const val TAG = "CastingManager"
    private const val SSDP_IP = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val DISCOVER_TIMEOUT_MS = 3000

    private val _discoveredDevices = MutableStateFlow<List<CastDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<CastDevice>> = _discoveredDevices.asStateFlow()

    private var multicastLock: WifiManager.MulticastLock? = null

    /**
     * Scans local network for UPnP/DLNA and DIAL compatible devices.
     */
    suspend fun discoverDevices(context: Context) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting device discovery (SSDP)...")
        
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (multicastLock == null) {
            multicastLock = wifi.createMulticastLock("StreamwiseCastingLock")
        }
        
        multicastLock?.acquire()
        
        val devices = mutableListOf<CastDevice>()
        var socket: DatagramSocket? = null

        try {
            socket = DatagramSocket()
            socket.soTimeout = DISCOVER_TIMEOUT_MS

            val query = """
                M-SEARCH * HTTP/1.1
                HOST: $SSDP_IP:$SSDP_PORT
                MAN: "ssdp:discover"
                MX: 3
                ST: ssdp:all
                
            """.trimIndent().replace("\n", "\r\n") + "\r\n"

            val group = InetAddress.getByName(SSDP_IP)
            val packet = DatagramPacket(query.toByteArray(), query.length, group, SSDP_PORT)
            socket.send(packet)

            val receiveBuffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < DISCOVER_TIMEOUT_MS) {
                try {
                    val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                    socket.receive(receivePacket)
                    
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val hostAddress = receivePacket.address?.hostAddress ?: ""
                    val device = parseSsdpResponse(response, hostAddress)
                    
                    if (device != null && devices.none { it.ip == device.ip }) {
                        devices.add(device)
                        Log.d(TAG, "Discovered device: ${device.name} at ${device.ip}")
                    }
                } catch (e: SocketTimeoutException) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SSDP Discovery error: ${e.message}")
        } finally {
            socket?.close()
            multicastLock?.release()
        }

        _discoveredDevices.value = devices
    }

    private fun parseSsdpResponse(response: String, ip: String): CastDevice? {
        val lines = response.lines()
        val location = lines.find { it.startsWith("LOCATION:", ignoreCase = true) }?.substringAfter(":")?.trim() ?: return null
        val st = lines.find { it.startsWith("ST:", ignoreCase = true) }?.substringAfter(":")?.trim() ?: ""
        
        // Filter for interesting devices: Renderers, Smart TVs, DIAL servers
        if (!st.contains("device:MediaRenderer", ignoreCase = true) && 
            !st.contains("dial-multiscreen", ignoreCase = true)) {
            return null
        }

        // Basic naming heuristic
        val name = when {
            st.contains("MediaRenderer") -> "Smart TV / Media Player"
            st.contains("dial") -> "DIAL Cast Target"
            else -> "Network Device"
        }

        return CastDevice(name = name, ip = ip, location = location, type = st)
    }

    /**
     * Placeholder for launching a URL on a discovered device.
     * Real implementation would involve sending a POST request to the device's DIAL or UPnP service.
     */
    suspend fun castUrl(device: CastDevice, url: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Casting URL: $url to ${device.name} (${device.ip})")
        // Implementation logic:
        // 1. Fetch XML description from device.location
        // 2. Find the Casting/Rendering service
        // 3. Send Play/Launch action
    }
}
