package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

import java.util.concurrent.ConcurrentHashMap

object OllamaClient {
    private val serviceCache = ConcurrentHashMap<String, OllamaApiService>()

    /**
     * Fast non-blocking reachability test for the local Ollama daemon before executing queries.
     * Prevents worker/coroutine thread blocking when a node is asleep or off-network.
     */
    suspend fun isHostReachable(host: String, timeoutMs: Int = 2000): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanHost = host.removePrefix("http://")
                .removePrefix("https://")
                .substringBefore(":")
                .substringBefore("/")
                .trim()
            if (cleanHost.isEmpty()) return@withContext false
            val port = 11434
            Socket().use { socket ->
                socket.connect(InetSocketAddress(cleanHost, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Query /api/tags to detect models and verify API readiness with a quick timeout.
     */
    suspend fun getTagsSafely(host: String): OllamaTagsResponse? = withContext(Dispatchers.IO) {
        try {
            val api = getApiService(host)
            api.getTags()
        } catch (e: Exception) {
            null
        }
    }

    fun getApiService(host: String): OllamaApiService {
        val normalizedHost = if (host.startsWith("http")) host else "http://$host:11434/"
        val finalHost = if (normalizedHost.endsWith("/")) normalizedHost else "$normalizedHost/"

        serviceCache[finalHost]?.let { return it }

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(finalHost)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        val service = retrofit.create(OllamaApiService::class.java)
        serviceCache[finalHost] = service
        return service
    }
}
