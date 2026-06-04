package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object OllamaClient {
    private var currentHost: String? = null
    private var apiService: OllamaApiService? = null

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
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
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
