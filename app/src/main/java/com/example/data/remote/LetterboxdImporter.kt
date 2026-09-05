package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class LetterboxdMovie(
    val title: String,
    val releaseYear: String? = null,
    val slug: String? = null,
    val letterboxdUri: String? = null
)

object LetterboxdImporter {

    private const val TAG = "LetterboxdImporter"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Match both film-poster container and image alt attribute
    private val FILM_POSTER_PATTERN = Pattern.compile(
        """<div[^>]*class="[^"]*film-poster[^"]*"[^>]*data-target-link="([^"]*)"[\s\S]*?<img[^>]*alt="([^"]+)"""",
        Pattern.CASE_INSENSITIVE
    )

    private val ALT_PATTERN = Pattern.compile(
        """<div[^>]*data-target-link="(/film/[^/]+/)"[\s\S]*?<img[^>]*alt="([^"]+)"""",
        Pattern.CASE_INSENSITIVE
    )

    private val PAGINATION_PATTERN = Pattern.compile(
        """/watchlist/page/(\d+)/""",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Scrapes public Letterboxd profile watchlist.
     * Paginates through up to maxPages (default 15 pages = 420 titles).
     */
    suspend fun fetchWatchlist(
        username: String,
        maxPages: Int = 15,
        onProgress: ((page: Int, totalFound: Int) -> Unit)? = null
    ): Result<List<LetterboxdMovie>> = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim().removePrefix("@").lowercase()
        if (cleanUsername.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Username cannot be empty"))
        }

        val allMovies = mutableListOf<LetterboxdMovie>()
        val seenSlugs = mutableSetOf<String>()
        var currentPage = 1
        var totalPages = maxPages

        try {
            while (currentPage <= totalPages && currentPage <= maxPages) {
                val pageUrl = if (currentPage == 1) {
                    "https://letterboxd.com/$cleanUsername/watchlist/"
                } else {
                    "https://letterboxd.com/$cleanUsername/watchlist/page/$currentPage/"
                }

                val request = Request.Builder()
                    .url(pageUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build()

                val response = client.newCall(request).execute()
                if (response.code == 404) {
                    if (currentPage == 1) {
                        return@withContext Result.failure(Exception("Letterboxd profile '@$cleanUsername' not found or watchlist is private."))
                    } else {
                        // End of pages
                        break
                    }
                }

                if (!response.isSuccessful) {
                    if (currentPage == 1) {
                        return@withContext Result.failure(Exception("Failed to access Letterboxd: HTTP ${response.code}"))
                    }
                    break
                }

                val html = response.body?.string() ?: ""
                val pageMovies = extractMoviesFromHtml(html)

                if (pageMovies.isEmpty()) {
                    // No movies found on this page -> end of watchlist
                    break
                }

                for (movie in pageMovies) {
                    val key = movie.slug ?: movie.title.lowercase()
                    if (!seenSlugs.contains(key)) {
                        seenSlugs.add(key)
                        allMovies.add(movie)
                    }
                }

                // Discover total pages on first page inspection
                if (currentPage == 1) {
                    val maxPageFound = extractMaxPage(html)
                    if (maxPageFound != null && maxPageFound < totalPages) {
                        totalPages = maxPageFound
                    }
                }

                onProgress?.invoke(currentPage, allMovies.size)
                Log.d(TAG, "Letterboxd page $currentPage for @$cleanUsername parsed. Total movies: ${allMovies.size}")

                currentPage++
            }

            Result.success(allMovies)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Letterboxd watchlist", e)
            Result.failure(e)
        }
    }

    private fun extractMoviesFromHtml(html: String): List<LetterboxdMovie> {
        val movies = mutableListOf<LetterboxdMovie>()
        
        var matcher = FILM_POSTER_PATTERN.matcher(html)
        var foundAny = false
        while (matcher.find()) {
            foundAny = true
            val targetLink = matcher.group(1) ?: ""
            val rawTitle = matcher.group(2) ?: ""
            val cleanTitle = unescapeHtml(rawTitle)
            val slug = targetLink.trim('/').substringAfterLast('/')
            val uri = if (targetLink.startsWith("http")) targetLink else "https://letterboxd.com$targetLink"
            
            movies.add(
                LetterboxdMovie(
                    title = cleanTitle,
                    slug = slug,
                    letterboxdUri = uri
                )
            )
        }

        if (!foundAny) {
            matcher = ALT_PATTERN.matcher(html)
            while (matcher.find()) {
                val targetLink = matcher.group(1) ?: ""
                val rawTitle = matcher.group(2) ?: ""
                val cleanTitle = unescapeHtml(rawTitle)
                val slug = targetLink.trim('/').substringAfterLast('/')
                val uri = "https://letterboxd.com$targetLink"
                
                movies.add(
                    LetterboxdMovie(
                        title = cleanTitle,
                        slug = slug,
                        letterboxdUri = uri
                    )
                )
            }
        }

        return movies
    }

    private fun extractMaxPage(html: String): Int? {
        val matcher = PAGINATION_PATTERN.matcher(html)
        var maxPage = 1
        var found = false
        while (matcher.find()) {
            matcher.group(1)?.toIntOrNull()?.let { pageNum ->
                if (pageNum > maxPage) {
                    maxPage = pageNum
                    found = true
                }
            }
        }
        return if (found) maxPage else null
    }

    /**
     * Parses standard Letterboxd CSV exports (e.g. watched.csv or watchlist.csv).
     */
    fun parseLetterboxdCsv(csvContent: String): List<LetterboxdMovie> {
        val reader = BufferedReader(StringReader(csvContent))
        val movies = mutableListOf<LetterboxdMovie>()

        var line = reader.readLine() ?: return emptyList()
        val headers = parseCsvLine(line).map { it.trim().lowercase() }
        val nameIdx = headers.indexOf("name").let { if (it >= 0) it else headers.indexOf("title") }
        val yearIdx = headers.indexOf("year")
        val uriIdx = headers.indexOf("letterboxd uri").let { if (it >= 0) it else headers.indexOf("uri") }

        if (nameIdx == -1) return emptyList()

        while (true) {
            line = reader.readLine() ?: break
            if (line.isBlank()) continue
            val tokens = parseCsvLine(line)
            if (tokens.size > nameIdx) {
                val title = tokens[nameIdx].trim()
                if (title.isNotEmpty()) {
                    val year = if (yearIdx != -1 && tokens.size > yearIdx) tokens[yearIdx].trim().ifBlank { null } else null
                    val uri = if (uriIdx != -1 && tokens.size > uriIdx) tokens[uriIdx].trim().ifBlank { null } else null
                    movies.add(
                        LetterboxdMovie(
                            title = title,
                            releaseYear = year,
                            letterboxdUri = uri
                        )
                    )
                }
            }
        }

        return movies
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString())
                    sb.setLength(0)
                }
                else -> sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString())
        return tokens
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .trim()
    }
}
