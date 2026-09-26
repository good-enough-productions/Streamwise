package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class MeshNodeStatus(
    val name: String,
    val host: String,
    val isOnline: Boolean,
    val latencyMs: Long = 0,
    val activeModel: String = "",
    val availableModels: List<String> = emptyList(),
    val role: String = "",
    val isUserActive: Boolean = false
)

data class MeshResolutionResult(
    val node: MeshNodeStatus,
    val apiService: OllamaApiService,
    val model: String
)

object AiMeshCoordinator {

    suspend fun probeNode(
        name: String,
        host: String,
        role: String,
        preferredModel: String? = null
    ): MeshNodeStatus = withContext(Dispatchers.IO) {
        val cleanHost = host.trim()
        if (cleanHost.isEmpty()) {
            return@withContext MeshNodeStatus(
                name = name,
                host = cleanHost,
                isOnline = false,
                role = role
            )
        }

        val t0 = System.currentTimeMillis()
        val tagsResponse = OllamaClient.getTagsSafely(cleanHost)
        val elapsed = System.currentTimeMillis() - t0

        if (tagsResponse != null && tagsResponse.models.isNotEmpty()) {
            val modelNames = tagsResponse.models.map { it.name }
            val chosenModel = when {
                preferredModel != null && modelNames.contains(preferredModel) -> preferredModel
                modelNames.any { it.contains("qwen2.5-coder") } -> modelNames.first { it.contains("qwen2.5-coder") }
                modelNames.any { it.contains("gemma4:e2b") } -> modelNames.first { it.contains("gemma4:e2b") }
                modelNames.any { it.contains("gemma4") } -> modelNames.first { it.contains("gemma4") }
                else -> modelNames.first()
            }

            // Probe Desktop Idle Sentry on port 11435 to detect if user is actively using the machine
            var isUserActive = false
            try {
                val url = java.net.URL("http://${cleanHost}:11435/activity")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 400
                conn.readTimeout = 400
                conn.requestMethod = "GET"
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    if (body.contains("\"user_active\": true") || body.contains("PAUSED_USER_ACTIVE")) {
                        isUserActive = true
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                // Sentry not running or port unreachable
            }

            MeshNodeStatus(
                name = name,
                host = cleanHost,
                isOnline = true,
                latencyMs = elapsed,
                activeModel = chosenModel,
                availableModels = modelNames,
                role = role,
                isUserActive = isUserActive
            )
        } else {
            MeshNodeStatus(
                name = name,
                host = cleanHost,
                isOnline = false,
                latencyMs = elapsed,
                role = role
            )
        }
    }

    suspend fun resolveActiveNode(
        primaryHost: String = "",
        secondaryHost: String = ""
    ): MeshResolutionResult? = withContext(Dispatchers.IO) {
        // 1. Probe Primary: Desktop / Server Node
        val primary = probeNode(
            name = "Primary Node",
            host = primaryHost,
            role = "Primary Compute Node",
            preferredModel = "qwen2.5-coder:7b"
        )
        // Only route to Primary if online AND user is NOT actively using the machine
        if (primary.isOnline && !primary.isUserActive) {
            val api = OllamaClient.getApiService(primaryHost)
            return@withContext MeshResolutionResult(primary, api, primary.activeModel)
        }

        // 2. Failover to Secondary: Laptop / Edge Node
        val secondary = probeNode(
            name = "Secondary Node",
            host = secondaryHost,
            role = "Edge Failover Unit",
            preferredModel = "gemma4:e2b"
        )
        if (secondary.isOnline) {
            val api = OllamaClient.getApiService(secondaryHost)
            return@withContext MeshResolutionResult(secondary, api, secondary.activeModel)
        }

        null
    }

    suspend fun probeEntireMesh(
        primaryHost: String,
        secondaryHost: String
    ): Pair<MeshNodeStatus, MeshNodeStatus> = coroutineScope {
        val primaryDeferred = async {
            probeNode(
                name = "Primary Node",
                host = primaryHost,
                role = "Primary Compute Node",
                preferredModel = "qwen2.5-coder:7b"
            )
        }
        val secondaryDeferred = async {
            probeNode(
                name = "Secondary Node",
                host = secondaryHost,
                role = "Edge Failover Unit",
                preferredModel = "gemma4:e2b"
            )
        }
        Pair(primaryDeferred.await(), secondaryDeferred.await())
    }
}
