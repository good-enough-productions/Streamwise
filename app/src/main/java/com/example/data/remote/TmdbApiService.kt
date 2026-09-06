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
    @Json(name = "release_date") val releaseDate: String?,
    @Json(name = "genre_ids") val genreIds: List<Int>? = null
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

@JsonClass(generateAdapter = true)
data class TmdbKeywordsResponse(
    @Json(name = "keywords") val keywords: List<TmdbKeyword>
)

@JsonClass(generateAdapter = true)
data class TmdbKeyword(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class TmdbCreditsResponse(
    @Json(name = "cast") val cast: List<TmdbCastMember>
)

@JsonClass(generateAdapter = true)
data class TmdbCastMember(
    @Json(name = "name") val name: String,
    @Json(name = "character") val character: String
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

    @GET("movie/{movie_id}/keywords")
    suspend fun getKeywords(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbKeywordsResponse

    @GET("movie/{movie_id}/credits")
    suspend fun getCredits(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbCreditsResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbMovieDetails

    @GET("genre/movie/list")
    suspend fun getGenreList(
        @Query("api_key") apiKey: String
    ): TmdbGenreListResponse

    @GET("search/tv")
    suspend fun searchTv(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("first_air_date_year") year: String? = null
    ): TmdbTvSearchResponse

    @GET("tv/{tv_id}/watch/providers")
    suspend fun getTvWatchProviders(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): TmdbWatchProvidersResponse

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): TmdbTvDetails

    @GET("tv/{tv_id}/credits")
    suspend fun getTvCredits(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): TmdbCreditsResponse

    @GET("movie/{movie_id}/release_dates")
    suspend fun getMovieReleaseDates(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbMovieReleaseDatesResponse
}

@JsonClass(generateAdapter = true)
data class TmdbMovieDetails(
    @Json(name = "id") val id: Int,
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbGenreListResponse(
    @Json(name = "genres") val genres: List<TmdbGenre>
)

@JsonClass(generateAdapter = true)
data class TmdbGenre(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class TmdbTvSearchResponse(
    @Json(name = "results") val results: List<TmdbTvSearchResult>
)

@JsonClass(generateAdapter = true)
data class TmdbTvSearchResult(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "overview") val overview: String?,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "vote_average") val voteAverage: Double?,
    @Json(name = "first_air_date") val firstAirDate: String?,
    @Json(name = "genre_ids") val genreIds: List<Int>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbTvDetails(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String? = null,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int? = null,
    @Json(name = "number_of_episodes") val numberOfEpisodes: Int? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "in_production") val inProduction: Boolean? = null,
    @Json(name = "next_episode_to_air") val nextEpisodeToAir: TmdbNextEpisode? = null
)

@JsonClass(generateAdapter = true)
data class TmdbNextEpisode(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "air_date") val airDate: String? = null,
    @Json(name = "season_number") val seasonNumber: Int? = null,
    @Json(name = "episode_number") val episodeNumber: Int? = null
)

@JsonClass(generateAdapter = true)
data class TmdbMovieReleaseDatesResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "results") val results: List<TmdbCountryReleaseDates> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbCountryReleaseDates(
    @Json(name = "iso_3166_1") val countryCode: String,
    @Json(name = "release_dates") val releaseDates: List<TmdbReleaseDateItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbReleaseDateItem(
    @Json(name = "certification") val certification: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "type") val type: Int = 3
)
