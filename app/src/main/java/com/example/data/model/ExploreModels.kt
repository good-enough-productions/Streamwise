package com.example.data.model

data class GeminiAnalysisResult(
    val headline: String,
    val narrative: String,
    val themes: List<String>,
    val auteurConnections: List<String>,
    val recommendations: List<Pair<String, String>>
)

data class PodcastEpisode(
    val id: String,
    val showTitle: String,
    val episodeTitle: String,
    val duration: String,
    val description: String,
    val date: String,
    val podcastUrl: String
)

data class MovieNewsItem(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val source: String,
    val date: String
)
