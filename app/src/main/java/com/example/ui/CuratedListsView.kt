package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*

/**
 * Curated & 3rd-Party Lists Hub
 *
 * Allows users to browse canonical cinema lists (Oscars, AFI Top 100, Sight & Sound,
 * Podcasts, Criterion, IMDb) and ad-hoc / Letterboxd lists.
 *
 * Crucially, EVERY film on ANY list is cross-referenced in real-time with:
 * 1. Personal Watchlist status (with 1-tap add/remove/star)
 * 2. Personal Watched status (with star ratings from diary)
 * 3. Active Streaming Services (with provider badges and 1-tap direct app launcher)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuratedListsScreen(
    allItems: List<MediaItem>,
    allProviders: List<StreamingProvider>,
    onAddOrToggleWatchlist: (CuratedListItem, MediaItem?) -> Unit,
    onToggleStarred: (MediaItem) -> Unit,
    onMarkWatched: (CuratedListItem, MediaItem?) -> Unit,
    onWatchClick: (MediaItem) -> Unit,
    onMovieClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    initialListId: String? = null
) {
    val allLists = remember { CuratedListCatalog.getAllLists() }
    var selectedList by remember { mutableStateOf<CuratedList?>(allLists.firstOrNull { it.id == initialListId }) }
    var customAdHocList by remember { mutableStateOf<CuratedList?>(null) }
    var showAdHocDialog by remember { mutableStateOf(false) }

    // If viewing a list, render detail view; otherwise directory
    val activeList = selectedList ?: customAdHocList

    if (activeList != null) {
        CuratedListDetailView(
            curatedList = activeList,
            allItems = allItems,
            allProviders = allProviders,
            onBack = {
                selectedList = null
                customAdHocList = null
            },
            onAddOrToggleWatchlist = onAddOrToggleWatchlist,
            onToggleStarred = onToggleStarred,
            onMarkWatched = onMarkWatched,
            onWatchClick = onWatchClick,
            onMovieClick = onMovieClick,
            modifier = modifier
        )
    } else {
        CuratedListsDirectoryView(
            curatedLists = allLists,
            allItems = allItems,
            allProviders = allProviders,
            onSelectList = { selectedList = it },
            onOpenAdHocDialog = { showAdHocDialog = true },
            modifier = modifier
        )
    }

    if (showAdHocDialog) {
        AdHocListImportDialog(
            onDismiss = { showAdHocDialog = false },
            onLoadList = { parsedList ->
                customAdHocList = parsedList
                showAdHocDialog = false
            }
        )
    }
}

/**
 * Directory view displaying available curated lists with live user progress bars.
 */
@Composable
fun CuratedListsDirectoryView(
    curatedLists: List<CuratedList>,
    allItems: List<MediaItem>,
    allProviders: List<StreamingProvider>,
    onSelectList: (CuratedList) -> Unit,
    onOpenAdHocDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(CuratedListCategory.ALL) }

    val filteredLists = remember(curatedLists, selectedCategory) {
        if (selectedCategory == CuratedListCategory.ALL) curatedLists
        else curatedLists.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("curated_lists_directory"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Curated & 3rd-Party Lists",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Explore cinema canons and podcasts without bloating your personal queue. See what's already on your watchlist, watched, or streaming tonight.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CuratedListCategory.values().forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text("${cat.icon} ${cat.displayName}", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Action Card: Ad-Hoc / Custom Letterboxd List Viewer
        item {
            Card(
                onClick = onOpenAdHocDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_load_adhoc_list"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AddLink,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "View Ad-Hoc or Letterboxd List",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Paste movie titles or a Letterboxd list to cross-reference streaming & watched status instantly.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Curated Lists Cards
        items(filteredLists, key = { it.id }) { list ->
            val enriched = remember(list, allItems, allProviders) {
                CuratedListCatalog.crossReference(list, allItems, allProviders)
            }
            val stats = remember(enriched) {
                CuratedListCatalog.computeStats(enriched)
            }

            CuratedListCard(
                list = list,
                stats = stats,
                onClick = { onSelectList(list) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * An individual curated list card showing completion percentage, watchlist volume,
 * and live streaming availability.
 */
@Composable
fun CuratedListCard(
    list: CuratedList,
    stats: CuratedListStats,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("curated_list_card_${list.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Emoji, Title, Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(list.emoji, fontSize = 28.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        list.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        list.subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        "${stats.watchedPercent}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Description
            Text(
                list.description,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Progress Bar
            LinearProgressIndicator(
                progress = { stats.watchedCount.toFloat() / stats.totalCount.coerceAtLeast(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            )

            // Dynamic Triple-Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Watched Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "✓ ${stats.watchedCount}/${stats.totalCount} Watched",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Watchlist Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (stats.watchlistCount > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "📌 ${stats.watchlistCount} in Queue",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (stats.watchlistCount > 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Streaming Pill
                    if (stats.streamingNowCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                "🍿 ${stats.streamingNowCount} on Subs",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "View List",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Detailed view of an active curated list with deep cross-referencing,
 * search, filters, and 1-tap watchlist/watched/streaming actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuratedListDetailView(
    curatedList: CuratedList,
    allItems: List<MediaItem>,
    allProviders: List<StreamingProvider>,
    onBack: () -> Unit,
    onAddOrToggleWatchlist: (CuratedListItem, MediaItem?) -> Unit,
    onToggleStarred: (MediaItem) -> Unit,
    onMarkWatched: (CuratedListItem, MediaItem?) -> Unit,
    onWatchClick: (MediaItem) -> Unit,
    onMovieClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("all") } // "all", "streaming", "watchlist", "unwatched", "watched"
    var isGridView by remember { mutableStateOf(false) }

    val enrichedItems = remember(curatedList, allItems, allProviders) {
        CuratedListCatalog.crossReference(curatedList, allItems, allProviders)
    }

    val stats = remember(enrichedItems) {
        CuratedListCatalog.computeStats(enrichedItems)
    }

    val filteredItems = remember(enrichedItems, searchQuery, activeFilter) {
        enrichedItems.filter { enriched ->
            // Search Query
            val matchesSearch = searchQuery.isBlank() ||
                enriched.item.title.contains(searchQuery, ignoreCase = true) ||
                (enriched.item.director?.contains(searchQuery, ignoreCase = true) == true) ||
                (enriched.item.badge?.contains(searchQuery, ignoreCase = true) == true)

            if (!matchesSearch) return@filter false

            // Active Filter Chip
            when (activeFilter) {
                "streaming" -> enriched.isAvailableOnMyServices
                "watchlist" -> enriched.isInWatchlist
                "unwatched" -> !enriched.isWatched
                "watched" -> enriched.isWatched
                else -> true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("curated_list_detail_${curatedList.id}")
    ) {
        // Top App Bar Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(curatedList.emoji, fontSize = 22.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            curatedList.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${stats.totalCount} Films • ${stats.watchedCount} Watched (${stats.watchedPercent}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Grid / List Toggle
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Grid/List View"
                        )
                    }
                }

                // Progress Bar
                LinearProgressIndicator(
                    progress = { stats.watchedCount.toFloat() / stats.totalCount.coerceAtLeast(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter ${curatedList.title}...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Filter Ribbon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == "all",
                        onClick = { activeFilter = "all" },
                        label = { Text("All (${stats.totalCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = activeFilter == "streaming",
                        onClick = { activeFilter = "streaming" },
                        label = { Text("🍿 Ready on Subs (${stats.streamingNowCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                    FilterChip(
                        selected = activeFilter == "watchlist",
                        onClick = { activeFilter = "watchlist" },
                        label = { Text("📌 In Queue (${stats.watchlistCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                    FilterChip(
                        selected = activeFilter == "unwatched",
                        onClick = { activeFilter = "unwatched" },
                        label = { Text("❌ Unwatched (${stats.totalCount - stats.watchedCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = activeFilter == "watched",
                        onClick = { activeFilter = "watched" },
                        label = { Text("✓ Watched (${stats.watchedCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        // Empty state
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔍", fontSize = 36.sp)
                    Text("No films match this filter", fontWeight = FontWeight.Bold)
                    Text("Try switching filter chips or clearing your search query.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (isGridView) {
            // Grid View
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredItems, key = { it.item.title + (it.item.year ?: 0) }) { enriched ->
                    CuratedGridItemCard(
                        enriched = enriched,
                        onAddOrToggleWatchlist = { onAddOrToggleWatchlist(enriched.item, enriched.matchingMediaItem) },
                        onToggleStarred = { enriched.matchingMediaItem?.let { onToggleStarred(it) } },
                        onMovieClick = { enriched.matchingMediaItem?.let { onMovieClick(it) } }
                    )
                }
            }
        } else {
            // List View
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredItems, key = { it.item.title + (it.item.year ?: 0) }) { enriched ->
                    CuratedListItemRow(
                        enriched = enriched,
                        onAddOrToggleWatchlist = { onAddOrToggleWatchlist(enriched.item, enriched.matchingMediaItem) },
                        onToggleStarred = { enriched.matchingMediaItem?.let { onToggleStarred(it) } },
                        onMarkWatched = { onMarkWatched(enriched.item, enriched.matchingMediaItem) },
                        onWatchClick = { enriched.matchingMediaItem?.let { onWatchClick(it) } },
                        onMovieClick = { enriched.matchingMediaItem?.let { onMovieClick(it) } }
                    )
                }
            }
        }
    }
}

/**
 * Detailed list row with rich triple-state badges (Watchlist, Watched, Services)
 * and zero-intermediary direct streaming launch button.
 */
@Composable
fun CuratedListItemRow(
    enriched: EnrichedCuratedItem,
    onAddOrToggleWatchlist: () -> Unit,
    onToggleStarred: () -> Unit,
    onMarkWatched: () -> Unit,
    onWatchClick: () -> Unit,
    onMovieClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMovieClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                enriched.isWatched -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                enriched.isStarred -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                enriched.isInWatchlist -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (enriched.isStarred) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail or placeholder
            val imageUrl = enriched.matchingMediaItem?.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = if (imageUrl.startsWith("http")) imageUrl else "https://image.tmdb.org/t/p/w185$imageUrl",
                    contentDescription = enriched.item.title,
                    modifier = Modifier
                        .width(46.dp)
                        .height(68.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                    modifier = Modifier
                        .width(46.dp)
                        .height(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎬", fontSize = 20.sp)
                    }
                }
            }

            // Title, Metadata & Director
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                // Rank / Badge & Year
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!enriched.item.badge.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                enriched.item.badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (enriched.item.year != null) {
                        Text(
                            "${enriched.item.year}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Title
                Text(
                    enriched.item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Director / Note
                val subtitle = enriched.item.director ?: enriched.item.note ?: ""
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Dynamic Badges Row: [Watched?] • [Watchlist?] • [Services]
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. Watched Status Pill
                    if (enriched.isWatched) {
                        val ratingStr = if ((enriched.userRating ?: 0.0) > 0.0) " ★ ${enriched.userRating}" else ""
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.5f))
                        ) {
                            Text(
                                "✓ Watched$ratingStr",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // 2. In Watchlist Pill / Priority
                    if (enriched.isInWatchlist) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (enriched.isStarred) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (enriched.isStarred) Text("⭐", fontSize = 9.sp)
                                Text(
                                    if (enriched.isStarred) "Priority" else "In Queue",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (enriched.isStarred) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // 3. Streaming Providers Badges
                    if (enriched.activeProviders.isNotEmpty()) {
                        enriched.activeProviders.take(2).forEach { provider ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    provider.name,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    } else if (enriched.streamingProviders.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                        ) {
                            Text(
                                enriched.streamingProviders.first().uppercase(),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Quick Actions: Star, Watchlist Toggle, Watch Button
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Watchlist 1-Tap Toggle
                IconButton(
                    onClick = onAddOrToggleWatchlist,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (enriched.isInWatchlist) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                        contentDescription = "Toggle Watchlist",
                        tint = if (enriched.isInWatchlist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // If on active subscription, show 1-tap direct Watch button
                if (enriched.isAvailableOnMyServices && enriched.matchingMediaItem != null) {
                    FilledTonalButton(
                        onClick = onWatchClick,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Watch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (enriched.isInWatchlist && enriched.matchingMediaItem != null) {
                    // Star Priority Toggle
                    IconButton(
                        onClick = onToggleStarred,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (enriched.isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Toggle Star",
                            tint = if (enriched.isStarred) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Couch-first 3-column poster grid item for browsing curated lists visually.
 */
@Composable
fun CuratedGridItemCard(
    enriched: EnrichedCuratedItem,
    onAddOrToggleWatchlist: () -> Unit,
    onToggleStarred: () -> Unit,
    onMovieClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMovieClick),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (enriched.isStarred) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            ) {
                val imageUrl = enriched.matchingMediaItem?.imageUrl
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = if (imageUrl.startsWith("http")) imageUrl else "https://image.tmdb.org/t/p/w342$imageUrl",
                        contentDescription = enriched.item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎬", fontSize = 28.sp)
                    }
                }

                // Top badges overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (enriched.isWatched) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2E7D32)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Watched",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(2.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }

                    // 1-Tap Watchlist Pill
                    Surface(
                        shape = CircleShape,
                        color = if (enriched.isInWatchlist) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.clickable(onClick = onAddOrToggleWatchlist)
                    ) {
                        Icon(
                            if (enriched.isInWatchlist) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                            contentDescription = "Watchlist",
                            tint = Color.White,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(4.dp)
                        )
                    }
                }

                // Bottom badge: Streaming or Rank
                if (enriched.isAvailableOnMyServices) {
                    Surface(
                        shape = RoundedCornerShape(topEnd = 6.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Text(
                            "▶ Stream",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Title & Year text below poster
            Column(modifier = Modifier.padding(6.dp)) {
                Text(
                    enriched.item.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    enriched.displayYear,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Dialog enabling ad-hoc loading of any Letterboxd list or pasted titles.
 */
@Composable
fun AdHocListImportDialog(
    onDismiss: () -> Unit,
    onLoadList: (CuratedList) -> Unit
) {
    var titleInput by remember { mutableStateOf("My Ad-Hoc List") }
    var textInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("View Ad-Hoc / 3rd-Party List", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Paste movie titles (one per line) or titles with years. Streamwise will cross-reference them against your watchlist, watched diary, and active streaming subscriptions without saving them all into your personal queue.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("List Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            "Example:\nAlien (1979)\nThe Thing (1982)\nScream (1996)\nHereditary (2018)",
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        val parsed = CuratedListCatalog.parseAdHocList(textInput, titleInput.ifBlank { "Ad-Hoc Custom List" })
                        onLoadList(parsed)
                    }
                },
                enabled = textInput.isNotBlank()
            ) {
                Text("Preview List Ad-Hoc →")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
