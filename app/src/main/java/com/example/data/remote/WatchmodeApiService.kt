package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class WatchmodeSearchResponse(
    @Json(name = "title_results") val results: List<WatchmodeSearchResult>
)

@JsonClass(generateAdapter = true)
data class WatchmodeSearchResult(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String,
    @Json(name = "year") val year: Int?
)

@JsonClass(generateAdapter = true)
data class WatchmodeSource(
    @Json(name = "source_id") val sourceId: Int,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String, // "sub", "free", "rent", "buy"
    @Json(name = "region") val region: String
)

interface WatchmodeApiService {
    @GET("search/")
    suspend fun searchTitle(
        @Query("apiKey") apiKey: String,
        @Query("search_field") searchField: String = "name",
        @Query("search_value") searchValue: String,
        @Query("types") types: String = "movie"
    ): WatchmodeSearchResponse

    @GET("title/{title_id}/sources/")
    suspend fun getTitleSources(
        @Path("title_id") titleId: Int,
        @Query("apiKey") apiKey: String,
        @Query("regions") regions: String = "US"
    ): List<WatchmodeSource>
}
