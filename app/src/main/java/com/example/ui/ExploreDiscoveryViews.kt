package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.DarkBorderOutline
import com.example.ui.theme.DarkInputBackground
import java.util.Locale

@Composable
fun NextWatchSpinAndHistorySection(
    selectedSeed: MediaItem?,
    seedOptions: List<MediaItem>,
    historyRecommendations: List<NextWatchRecommendation>,
    isLoadingHistoryRecs: Boolean,
    tonightQueueMatches: List<NextWatchRecommendation>,
    spunNextWatch: NextWatchRecommendation?,
    allProviders: List<StreamingProvider>,
    customPlaylists: List<CuratedList>,
    onSpinNextWatch: (Boolean) -> Unit,
    onSelectSeed: (MediaItem) -> Unit,
    onShuffleSeed: () -> Unit,
    onAddToWatchlist: (NextWatchRecommendation, Boolean) -> Unit,
    onMarkWatched: (NextWatchRecommendation) -> Unit,
    onWatchClick: (MediaItem) -> Unit,
    onMovieClick: (MediaItem) -> Unit,
    onAddToPlaylist: (CuratedListItem) -> Unit,
    onJumpToSearch: () -> Unit,
    onJumpToPlaylists: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Primary Hero CTA: Spin My Next Watch
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("next_watch_hero_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "FIND YOUR NEXT WATCH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Indecisive? Spin a match from your taste profile, queue & playlists.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSpinNextWatch(false) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Spin Next Watch", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = { onSpinNextWatch(true) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("On My Subs", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }
                }

                if (spunNextWatch != null) {
                    NextWatchRecCard(
                        rec = spunNextWatch,
                        allProviders = allProviders,
                        onAddToWatchlist = { star -> onAddToWatchlist(spunNextWatch, star) },
                        onMarkWatched = { onMarkWatched(spunNextWatch) },
                        onWatchClick = { spunNextWatch.matchingMediaItem?.let(onWatchClick) },
                        onMovieClick = {
                            val item = spunNextWatch.matchingMediaItem ?: MediaItem(
                                title = spunNextWatch.title,
                                tmdbId = spunNextWatch.tmdbId,
                                imageUrl = spunNextWatch.posterUrl,
                                rating = spunNextWatch.rating,
                                overview = spunNextWatch.overview,
                                releaseDate = spunNextWatch.year?.toString(),
                                importSource = spunNextWatch.sourceSeed,
                                status = MediaStatus.WATCHLIST.name
                            )
                            onMovieClick(item)
                        },
                        onAddToPlaylist = {
                            onAddToPlaylist(
                                CuratedListItem(
                                    title = spunNextWatch.title,
                                    year = spunNextWatch.year,
                                    tmdbId = spunNextWatch.tmdbId,
                                    note = spunNextWatch.reason
                                )
                            )
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onJumpToSearch,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Movies & Actors", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onJumpToPlaylists,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("14+ Playlists", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        // 2. Because You Watched... (Live Watch-History Recommendations)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "BECAUSE YOU WATCHED...",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = selectedSeed?.let { "Live picks inspired by ${it.title}" }
                            ?: "Tap any watched movie to pull fresh recommendations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = onShuffleSeed,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Shuffle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (seedOptions.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(seedOptions, key = { it.id }) { seed ->
                        val isSelected = selectedSeed?.id == seed.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectSeed(seed) },
                            label = {
                                Text(
                                    seed.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }

            if (isLoadingHistoryRecs) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Finding movies similar to ${selectedSeed?.title ?: "your history"}...", style = MaterialTheme.typography.bodySmall)
                }
            } else if (historyRecommendations.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(historyRecommendations, key = { (it.tmdbId ?: "") + it.title }) { rec ->
                        NextWatchPosterTile(
                            rec = rec,
                            onAddToWatchlist = { onAddToWatchlist(rec, false) },
                            onMarkWatched = { onMarkWatched(rec) },
                            onMovieClick = {
                                val item = rec.matchingMediaItem ?: MediaItem(
                                    title = rec.title,
                                    tmdbId = rec.tmdbId,
                                    imageUrl = rec.posterUrl,
                                    rating = rec.rating,
                                    overview = rec.overview,
                                    releaseDate = rec.year?.toString(),
                                    importSource = rec.reason,
                                    status = MediaStatus.WATCHLIST.name
                                )
                                onMovieClick(item)
                            }
                        )
                    }
                }
            }
        }

        // 3. Tonight's Best Matches (From Queue & Playlists)
        if (tonightQueueMatches.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "TONIGHT'S BEST MATCHES (QUEUE & PLAYLISTS)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                tonightQueueMatches.take(6).forEach { rec ->
                    NextWatchRecCard(
                        rec = rec,
                        allProviders = allProviders,
                        onAddToWatchlist = { star -> onAddToWatchlist(rec, star) },
                        onMarkWatched = { onMarkWatched(rec) },
                        onWatchClick = { rec.matchingMediaItem?.let(onWatchClick) },
                        onMovieClick = {
                            val item = rec.matchingMediaItem ?: MediaItem(
                                title = rec.title,
                                tmdbId = rec.tmdbId,
                                imageUrl = rec.posterUrl,
                                rating = rec.rating,
                                overview = rec.overview,
                                releaseDate = rec.year?.toString(),
                                importSource = rec.sourceSeed,
                                status = MediaStatus.WATCHLIST.name
                            )
                            onMovieClick(item)
                        },
                        onAddToPlaylist = {
                            onAddToPlaylist(
                                CuratedListItem(
                                    title = rec.title,
                                    year = rec.year,
                                    tmdbId = rec.tmdbId,
                                    note = rec.reason
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NextWatchPosterTile(
    rec: NextWatchRecommendation,
    onAddToWatchlist: () -> Unit,
    onMarkWatched: () -> Unit,
    onMovieClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(134.dp)
            .clickable { onMovieClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!rec.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = rec.posterUrl,
                        contentDescription = rec.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.outline)
                    }
                }

                if ((rec.rating ?: 0.0) > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.78f),
                        modifier = Modifier
                            .padding(5.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            "★ ${String.format(Locale.US, "%.1f", rec.rating)}",
                            color = Color(0xFFFFD700),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (rec.isInWatchlist) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.68f),
                        modifier = Modifier
                            .size(26.dp)
                            .clickable { onAddToWatchlist() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (rec.isInWatchlist) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                                contentDescription = "Watchlist",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    rec.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    rec.year?.toString() ?: "Film",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun NextWatchRecCard(
    rec: NextWatchRecommendation,
    allProviders: List<StreamingProvider>,
    onAddToWatchlist: (Boolean) -> Unit,
    onMarkWatched: () -> Unit,
    onWatchClick: () -> Unit,
    onMovieClick: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMovieClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!rec.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = rec.posterUrl,
                    contentDescription = rec.title,
                    modifier = Modifier
                        .size(width = 52.dp, height = 76.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(width = 52.dp, height = 76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎬", fontSize = 20.sp)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (rec.year != null) "${rec.title} (${rec.year})" else rec.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if ((rec.rating ?: 0.0) > 0.0) {
                        Text(
                            "★ ${String.format(Locale.US, "%.1f", rec.rating)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                Text(
                    rec.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!rec.overview.isNullOrBlank()) {
                    Text(
                        rec.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (rec.matchingMediaItem != null && rec.availableProviders.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = onWatchClick,
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            val pName = allProviders.find { it.id == rec.availableProviders.first() }?.name ?: "Watch"
                            Text(pName, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { onAddToWatchlist(false) },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                if (rec.isInWatchlist) Icons.Default.BookmarkAdded else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (rec.isInWatchlist) "In Queue" else "+ Watchlist", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to Playlist", modifier = Modifier.size(16.dp))
                    }

                    IconButton(onClick = onMarkWatched, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = "Mark Watched", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UniversalSearchExploreView(
    query: String,
    mode: String,
    movieResults: List<UniversalMovieResult>,
    personResults: List<PersonSearchCandidate>,
    selectedPersonFilmography: PersonFilmographyResult?,
    isSearching: Boolean,
    isLoadingFilmography: Boolean,
    onQueryAndSearch: (String, String) -> Unit,
    onModeChange: (String) -> Unit,
    onSelectPerson: (PersonSearchCandidate) -> Unit,
    onClearPersonFilmography: () -> Unit,
    onAddMovieToWatchlist: (title: String, tmdbId: String?, posterUrl: String?, releaseDate: String?, rating: Double?, overview: String?, starPriority: Boolean) -> Unit,
    onMarkMovieWatched: (title: String, tmdbId: String?, posterUrl: String?, releaseDate: String?, rating: Double?, overview: String?) -> Unit,
    onAddToPlaylist: (CuratedListItem) -> Unit,
    onMovieClick: (MediaItem) -> Unit
) {
    var localQuery by remember(query) { mutableStateOf(query) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("universal_search_view")
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode chips: Movies & TV vs Actors & Directors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mode == "movies",
                onClick = { onModeChange("movies") },
                label = { Text("🎬 Movies & TV", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = mode == "actors",
                onClick = { onModeChange("actors") },
                label = { Text("🎭 Actors & Directors", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
        }

        // Search input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = localQuery,
                onValueChange = { localQuery = it },
                placeholder = {
                    Text(
                        if (mode == "actors") "Search actor or director (e.g. Ryan Gosling, Fincher)..."
                        else "Search any movie or show...",
                        fontSize = 12.sp
                    )
                },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (localQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            localQuery = ""
                            onQueryAndSearch("", mode)
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    onQueryAndSearch(localQuery, mode)
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = DarkBorderOutline,
                    focusedContainerColor = DarkInputBackground,
                    unfocusedContainerColor = DarkInputBackground
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    keyboardController?.hide()
                    onQueryAndSearch(localQuery, mode)
                },
                enabled = localQuery.trim().length >= 2,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Text("Go", fontWeight = FontWeight.Bold)
            }
        }

        if (isSearching || isLoadingFilmography) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Empty state with quick-search starter chips
        if (localQuery.isBlank() && selectedPersonFilmography == null && movieResults.isEmpty() && personResults.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (mode == "actors") "Explore Any Actor or Director" else "Search Any Movie or Show",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (mode == "actors")
                            "See an actor or director's full filmography, your % watched completion, and their exact age at release for every movie."
                        else
                            "Look up any film across TMDB and your library, check whether you've watched it, and add it to your Watchlist or Playlists in 1 tap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val starters = if (mode == "actors") {
                        listOf("Denis Villeneuve", "Emma Stone", "Michael Mann", "Colin Farrell", "David Fincher")
                    } else {
                        listOf("Heat", "Sicario", "Children of Men", "Thief", "Past Lives", "Anora")
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        starters.forEach { sample ->
                            AssistChip(
                                onClick = {
                                    localQuery = sample
                                    onQueryAndSearch(sample, mode)
                                },
                                label = { Text(sample, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        } else if (mode == "actors") {
            if (selectedPersonFilmography != null) {
                PersonFilmographyView(
                    filmography = selectedPersonFilmography,
                    onBack = onClearPersonFilmography,
                    onAddMovieToWatchlist = onAddMovieToWatchlist,
                    onMarkMovieWatched = onMarkMovieWatched,
                    onAddToPlaylist = onAddToPlaylist,
                    onMovieClick = onMovieClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(personResults, key = { it.id }) { person ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPerson(person) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!person.profileUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = person.profileUrl,
                                        contentDescription = person.name,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(52.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Person, contentDescription = null)
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(person.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    if (!person.department.isNullOrBlank()) {
                                        Text(person.department, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (person.knownForSummary.isNotBlank()) {
                                        Text(
                                            "Known for: ${person.knownForSummary}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(movieResults, key = { (it.tmdbId ?: "") + "_" + it.title + "_" + (it.year ?: 0) }) { movie ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val item = movie.matchingMediaItem ?: MediaItem(
                                    title = movie.title,
                                    tmdbId = movie.tmdbId,
                                    imageUrl = movie.posterUrl,
                                    rating = movie.rating,
                                    overview = movie.overview,
                                    releaseDate = movie.releaseDate,
                                    genres = movie.genres,
                                    status = MediaStatus.WATCHLIST.name
                                )
                                onMovieClick(item)
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (!movie.posterUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = movie.posterUrl,
                                    contentDescription = movie.title,
                                    modifier = Modifier
                                        .size(width = 56.dp, height = 84.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(width = 56.dp, height = 84.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) { Text("🎬", fontSize = 22.sp) }
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (movie.year != null) "${movie.title} (${movie.year})" else movie.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if ((movie.rating ?: 0.0) > 0.0) {
                                        Text(
                                            "★ ${String.format(Locale.US, "%.1f", movie.rating)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (movie.isWatched) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF2E7D32).copy(alpha = 0.25f)) {
                                            Text("✓ Watched", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                        }
                                    } else if (movie.isInWatchlist) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                            Text(if (movie.isStarred) "⭐ Priority" else "📌 Watchlist", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text(movie.mediaType, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                    }
                                }

                                if (!movie.overview.isNullOrBlank()) {
                                    Text(
                                        movie.overview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (!movie.isWatched) {
                                        FilledTonalButton(
                                            onClick = {
                                                onAddMovieToWatchlist(movie.title, movie.tmdbId, movie.posterUrl, movie.releaseDate, movie.rating, movie.overview, false)
                                            },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                        ) {
                                            Text(if (movie.isInWatchlist) "⭐ Priority" else "+ Watchlist", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(
                                            onClick = {
                                                onMarkMovieWatched(movie.title, movie.tmdbId, movie.posterUrl, movie.releaseDate, movie.rating, movie.overview)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircleOutline, contentDescription = "Mark Watched", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            onAddToPlaylist(
                                                CuratedListItem(
                                                    title = movie.title,
                                                    year = movie.year,
                                                    tmdbId = movie.tmdbId,
                                                    note = movie.overview?.take(120)
                                                )
                                            )
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to Playlist", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonFilmographyView(
    filmography: PersonFilmographyResult,
    onBack: () -> Unit,
    onAddMovieToWatchlist: (title: String, tmdbId: String?, posterUrl: String?, releaseDate: String?, rating: Double?, overview: String?, starPriority: Boolean) -> Unit,
    onMarkMovieWatched: (title: String, tmdbId: String?, posterUrl: String?, releaseDate: String?, rating: Double?, overview: String?) -> Unit,
    onAddToPlaylist: (CuratedListItem) -> Unit,
    onMovieClick: (MediaItem) -> Unit
) {
    var filterMode by remember { mutableStateOf("all") } // "all", "unwatched", "watched"
    val displayed = remember(filmography.items, filterMode) {
        when (filterMode) {
            "unwatched" -> filmography.items.filter { !it.isWatched }
            "watched" -> filmography.items.filter { it.isWatched }
            else -> filmography.items
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to search")
                        }
                        if (!filmography.profileUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = filmography.profileUrl,
                                contentDescription = filmography.name,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(filmography.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val meta = listOfNotNull(
                                filmography.department,
                                filmography.birthday?.let { "b. ${it.take(4)}" },
                                filmography.placeOfBirth
                            ).joinToString(" • ")
                            if (meta.isNotBlank()) {
                                Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { filmography.watchedCount.toFloat() / filmography.totalCount.coerceAtLeast(1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Text(
                        "Watched ${filmography.watchedCount} of ${filmography.totalCount} credits (${filmography.watchedPercent}%)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = filterMode == "all", onClick = { filterMode = "all" }, label = { Text("All (${filmography.totalCount})", fontSize = 11.sp) })
                        FilterChip(selected = filterMode == "unwatched", onClick = { filterMode = "unwatched" }, label = { Text("Unwatched (${filmography.totalCount - filmography.watchedCount})", fontSize = 11.sp) })
                        FilterChip(selected = filterMode == "watched", onClick = { filterMode = "watched" }, label = { Text("Watched (${filmography.watchedCount})", fontSize = 11.sp) })
                    }
                }
            }
        }

        items(displayed, key = { it.tmdbId + "_" + it.roleLabel }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val media = item.matchingMediaItem ?: MediaItem(
                            title = item.title,
                            tmdbId = item.tmdbId,
                            imageUrl = item.posterUrl,
                            rating = item.rating,
                            overview = item.overview,
                            releaseDate = item.releaseDate,
                            importSource = filmography.name,
                            status = MediaStatus.WATCHLIST.name
                        )
                        onMovieClick(media)
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!item.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.title,
                            modifier = Modifier
                                .size(width = 48.dp, height = 72.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(width = 48.dp, height = 72.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("🎬", fontSize = 18.sp) }
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = if (item.year != null) "${item.title} (${item.year})" else item.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            item.roleLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (item.actorAgeAtRelease != null) {
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)) {
                                    Text("🎂 Age ${item.actorAgeAtRelease}", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                }
                            }
                            if (item.isWatched) {
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF2E7D32).copy(alpha = 0.25f)) {
                                    Text("✓ Watched", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                }
                            } else if (item.isInWatchlist) {
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text(if (item.isStarred) "⭐ Priority" else "📌 Watchlist", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!item.isWatched) {
                            IconButton(
                                onClick = {
                                    onAddMovieToWatchlist(item.title, item.tmdbId, item.posterUrl, item.releaseDate, item.rating, item.overview, false)
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    if (item.isInWatchlist) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                                    contentDescription = "Watchlist",
                                    tint = if (item.isInWatchlist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    onMarkMovieWatched(item.title, item.tmdbId, item.posterUrl, item.releaseDate, item.rating, item.overview)
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Mark Watched", modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(
                            onClick = {
                                onAddToPlaylist(
                                    CuratedListItem(
                                        title = item.title,
                                        year = item.year,
                                        tmdbId = item.tmdbId,
                                        note = item.roleLabel
                                    )
                                )
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to Playlist", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistPickerDialog(
    item: CuratedListItem,
    customPlaylists: List<CuratedList>,
    onSelectPlaylist: (String) -> Unit,
    onCreateNewPlaylistWithItem: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add \"${item.title}\" to Playlist", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (customPlaylists.isEmpty()) {
                    Text(
                        "You don't have any custom playlists yet. Create one below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    customPlaylists.forEach { pl ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPlaylist(pl.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${pl.emoji} ${pl.title}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${pl.items.size} films", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("New playlist name...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { if (newName.isNotBlank()) onCreateNewPlaylistWithItem(newName.trim()) },
                        enabled = newName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
