package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class OllamaModelItem(
    val name: String,
    val model: String? = null,
    val modified_at: String? = null,
    val size: Long? = null
)

@JsonClass(generateAdapter = true)
data class OllamaTagsResponse(
    val models: List<OllamaModelItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OllamaChatRequest(
    val model: String = "qwen2.5-coder:7b",
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
    val model: String? = null,
    val message: OllamaChatMessage
)

interface OllamaApiService {
    @GET("api/tags")
    suspend fun getTags(): OllamaTagsResponse

    @POST("api/chat")
    suspend fun chat(@Body request: OllamaChatRequest): OllamaChatResponse
}
