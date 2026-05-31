package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class TmdbSearchResponse(
    @Json(name = "results") val results: List<TmdbSearchResult>
)

@JsonClass(generateAdapter = true)
data class TmdbSearchResult(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "overview") val overview: String?,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "vote_average") val voteAverage: Double?,
    @Json(name = "release_date") val releaseDate: String?
)

@JsonClass(generateAdapter = true)
data class TmdbWatchProvidersResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "results") val results: Map<String, CountryProviders>?
)

@JsonClass(generateAdapter = true)
data class CountryProviders(
    @Json(name = "link") val link: String? = null,
    @Json(name = "flatrate") val flatrate: List<TmdbProvider>? = null,
    @Json(name = "free") val free: List<TmdbProvider>? = null,
    @Json(name = "ads") val ads: List<TmdbProvider>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbProvider(
    @Json(name = "provider_id") val providerId: Int,
    @Json(name = "provider_name") val providerName: String,
    @Json(name = "logo_path") val logoPath: String? = null
)

interface TmdbApiService {
    @GET("search/movie")
    suspend fun searchMovie(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("year") year: String? = null
    ): TmdbSearchResponse

    @GET("movie/{movie_id}/watch/providers")
    suspend fun getWatchProviders(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbWatchProvidersResponse
}
