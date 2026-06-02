package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class OllamaChatRequest(
    val model: String = "gemma2",
    val messages: List<OllamaChatMessage>,
    val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class OllamaChatMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OllamaChatResponse(
    val message: OllamaChatMessage
)

interface OllamaApiService {
    @POST("api/chat")
    suspend fun chat(@Body request: OllamaChatRequest): OllamaChatResponse
}
