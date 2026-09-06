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

object OllamaClient {
    private var currentHost: String? = null
    private var apiService: OllamaApiService? = null

    /**
     * Fast non-blocking reachability test for the local Ollama daemon before executing queries.
     * Prevents worker/coroutine thread blocking when laptop is asleep or off-network.
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

    fun getApiService(host: String): OllamaApiService {
        val normalizedHost = if (host.startsWith("http")) host else "http://$host:11434/"
        val finalHost = if (normalizedHost.endsWith("/")) normalizedHost else "$normalizedHost/"

        if (currentHost == finalHost && apiService != null) {
            return apiService!!
        }

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

        currentHost = finalHost
        apiService = retrofit.create(OllamaApiService::class.java)
        return apiService!!
    }
}
