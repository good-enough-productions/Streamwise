package com.example.data.remote

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = "user",
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiSystemInstruction(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

@JsonClass(generateAdapter = true)
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {

    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val apiService: GeminiApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(okHttpClient)
        .build()
        .create(GeminiApiService::class.java)

    /**
     * High-level helper to generate text using Gemini 2.0 Flash.
     */
    suspend fun generateContent(
        apiKey: String,
        prompt: String,
        systemInstruction: String? = null,
        chatHistory: List<OllamaChatMessage> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank() || cleanKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("No valid Gemini API key configured."))
        }

        try {
            val contents = mutableListOf<GeminiContent>()
            
            // Add prior chat turns if any
            for (msg in chatHistory) {
                val role = if (msg.role == "assistant" || msg.role == "model") "model" else "user"
                contents.add(GeminiContent(role = role, parts = listOf(GeminiPart(text = msg.content))))
            }
            
            // Add current turn
            contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt))))

            val sysInstruction = systemInstruction?.let {
                GeminiSystemInstruction(parts = listOf(GeminiPart(text = it)))
            }

            val request = GeminiRequest(
                contents = contents,
                systemInstruction = sysInstruction
            )

            val response = apiService.generateContent(cleanKey, request)
            val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (!candidateText.isNullOrBlank()) {
                Result.success(candidateText)
            } else {
                val errMsg = response.error?.message ?: "Empty response from Gemini 2.0 Flash."
                Log.w(TAG, "Gemini API error response: $errMsg")
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API network/serialization failure", e)
            Result.failure(e)
        }
    }
}
