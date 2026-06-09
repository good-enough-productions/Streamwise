package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProviderUsageStats
import com.example.data.model.MediaItem
import com.example.data.model.MediaStatus
import com.example.data.model.StreamingProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: StreamViewModel,
    modifier: Modifier = Modifier,
    simulateForegroundReturn: () -> Unit = {}
) {
    val watchlistItems by viewModel.allMediaItems.collectAsState()
    val allProviders by viewModel.allProviders.collectAsState()
    val monthlyStats by viewModel.monthlyROIStats.collectAsState()
    val checkInItem by viewModel.activeCheckInItem.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val tmdbApiKey by viewModel.tmdbApiKey.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(0) } // 0: Watchlist, 1: Watched, 2: ROI Stats, 3: Agent
    var filterOnlyMyServices by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var detailMovieItem by remember { mutableStateOf<MediaItem?>(null) }

    // Clear and display Toast/Status banners beautifully
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier.testTag("home_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Streamwise",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.exportToObsidian() },
                        modifier = Modifier.testTag("export_obsidian_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export to Obsidian",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_gear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = simulateForegroundReturn,
                        modifier = Modifier.testTag("simulate_foreground_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Simulate App Resume (Foreground Check)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                ExtendedFloatingActionButton(
                    text = { Text("Add Title") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add media item") },
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("add_item_fab"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main navigation tabs for modular layout
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Watchlist", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Watchlist tab") },
                    modifier = Modifier.testTag("tab_watchlist")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Watched", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Check, contentDescription = "Watched history tab") },
                    modifier = Modifier.testTag("tab_watched")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("ROI Stats", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Star, contentDescription = "ROI stats tab") },
                    modifier = Modifier.testTag("tab_budget")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Agent", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "AI Agent tab") },
                    modifier = Modifier.testTag("tab_agent")
                )
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                label = "tab_fade"
            ) { targetTab ->
                when (targetTab) {
                    0 -> WatchlistTabContent(
                        watchlistItems = watchlistItems,
                        allProviders = allProviders,
                        filterOnlyMyServices = filterOnlyMyServices,
                        onFilterToggle = { filterOnlyMyServices = it },
                        onWatchClick = { viewModel.startIntendingToWatch(it) },
                        onDeleteClick = { viewModel.deleteItem(it) },
                        onSyncClick = { viewModel.triggerImmediateSync() },
                        tmdbApiKey = tmdbApiKey,
                        onOpenSettings = { showSettingsDialog = true },
                        onMovieClick = { detailMovieItem = it }
                    )
                    1 -> {
                        val watchedItems by viewModel.watchedItems.collectAsState()
                        WatchedTabContent(
                            watchedItems = watchedItems,
                            allProviders = allProviders,
                            onMovieClick = { detailMovieItem = it },
                            onDeleteClick = { viewModel.deleteItem(it) },
                            onSyncClick = { viewModel.triggerImmediateSync() }
                        )
                    }
                    2 -> MonthlyRoiContent(
                        monthlyStats = monthlyStats,
                        allProviders = allProviders
                    )
                    3 -> {
                        val chatMessages by viewModel.chatMessages.collectAsState()
                        val isChatLoading by viewModel.isChatLoading.collectAsState()
                        AgentChatTabContent(
                            chatMessages = chatMessages,
                            isLoading = isChatLoading,
                            onSendMessage = { viewModel.sendChatMessage(it) }
                        )
                    }
                }
            }
        }

        // Add Dialog
        if (showAddDialog) {
            AddMediaDialog(
                allProviders = allProviders,
                onDismiss = { showAddDialog = false },
                onAdd = { titlesInput, selectedProviderIds, notes, source ->
                    viewModel.addCustomWatchlistItemsBulk(titlesInput, selectedProviderIds, notes, source)
                    showAddDialog = false
                }
            )
        }

        // Active check-in BottomSheet (Triggered on foreground return detect)
        if (checkInItem != null) {
            CheckInBottomSheet(
                item = checkInItem!!,
                allProviders = allProviders,
                onDismiss = { viewModel.clearIntentFlag(checkInItem!!) },
                onLoggedFinished = { duration, providerId, notes ->
                    viewModel.logSessionFinished(checkInItem!!, providerId, duration, notes)
                },
                onLoggedPartial = { duration, providerId, notes ->
                    viewModel.logSessionPartial(checkInItem!!, providerId, duration, notes)
                },
                onLoggedSomethingElse = { customTitle, duration, providerId ->
                    viewModel.watchedSomethingElse(checkInItem!!, customTitle, providerId, duration)
                },
                onLoggedNothing = {
                    viewModel.clearIntentFlag(checkInItem!!)
                }
            )
        }

        // Settings Dialog (Overlay)
        if (showSettingsDialog) {
            val ollamaHost by viewModel.ollamaHost.collectAsState()
            val githubToken by viewModel.githubToken.collectAsState()
            val watchmodeApiKey by viewModel.watchmodeApiKey.collectAsState()
            SettingsDialog(
                allProviders = allProviders,
                onProviderToggle = { id, active -> viewModel.toggleStreamingProvider(id, active) },
                tmdbApiKey = tmdbApiKey,
                onSaveTmdbApiKey = { viewModel.saveTmdbApiKey(it) },
                watchmodeApiKey = watchmodeApiKey,
                onSaveWatchmodeApiKey = { viewModel.saveWatchmodeApiKey(it) },
                ollamaHost = ollamaHost,
                onSaveOllamaHost = { viewModel.saveOllamaHost(it) },
                githubToken = githubToken,
                onSaveGithubToken = { viewModel.saveGithubToken(it) },
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Expanded Movie Details Bottom Sheet
        if (detailMovieItem != null) {
            val discoveredDevices by viewModel.discoveredDevices.collectAsState()
            MovieDetailsBottomSheet(
                item = detailMovieItem!!,
                allProviders = allProviders,
                discoveredDevices = discoveredDevices,
                onCastClick = { device, item -> viewModel.castToDevice(device, item) },
                onDismiss = { detailMovieItem = null },
                onWatchClick = {
                    viewModel.startIntendingToWatch(detailMovieItem!!)
                    detailMovieItem = null
                },
                onDeleteClick = {
                    viewModel.deleteItem(detailMovieItem!!)
                    detailMovieItem = null
                }
            )
        }
    }
}

// ==========================================
// COMPOSABLE: Watchlist Screen
// ==========================================
@Composable
fun WatchlistTabContent(
    watchlistItems: List<MediaItem>,
    allProviders: List<StreamingProvider>,
    filterOnlyMyServices: Boolean,
    onFilterToggle: (Boolean) -> Unit,
    onWatchClick: (MediaItem) -> Unit,
    onDeleteClick: (MediaItem) -> Unit,
    onSyncClick: () -> Unit = {},
    tmdbApiKey: String,
    onOpenSettings: () -> Unit,
    onMovieClick: (MediaItem) -> Unit
) {
    val activeProviderIds = remember(allProviders) {
        allProviders.filter { it.isActive }.map { it.id }.toSet()
    }
    val freeProviderIds = remember(allProviders) {
        allProviders.filter { it.costPerMonth == 0.0 }.map { it.id }.toSet()
    }

    var selectedPlatformId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("added") } // "added", "alpha", "rating"
    var showFreeOnly by remember { mutableStateOf(false) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }

    val allGenres = remember(watchlistItems) {
        watchlistItems.flatMap { it.genres?.split(",")?.map { g -> g.trim() } ?: emptyList() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    // Filter items according to state
    val filteredItems = remember(watchlistItems, filterOnlyMyServices, activeProviderIds, selectedPlatformId, showFreeOnly, selectedGenre) {
        watchlistItems.filter { item ->
            // Exclude already watched from immediate watchlist
            if (item.status == MediaStatus.WATCHED.name) return@filter false

            if (selectedPlatformId != null) {
                if (item.providersList.contains(selectedPlatformId) != true) return@filter false
            }

            if (selectedGenre != null) {
                if (item.genres?.contains(selectedGenre!!, ignoreCase = true) != true) return@filter false
            }

            if (showFreeOnly) {
                val provs = item.providersList
                if (provs.none { freeProviderIds.contains(it) }) return@filter false
            }

            if (filterOnlyMyServices) {
                // Return items having at least one of their available platforms as locally active/subscribed
                val provs = item.providersList
                provs.isEmpty() || provs.any { activeProviderIds.contains(it) }
            } else {
                true
            }
        }
    }

    val processedItems = remember(filteredItems, searchQuery, sortBy) {
        var items = filteredItems
        if (searchQuery.isNotBlank()) {
            items = items.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
        when (sortBy) {
            "alpha" -> items.sortedBy { it.title.lowercase() }
            "rating" -> items.sortedByDescending { it.rating ?: 0.0 }
            else -> items.sortedByDescending { it.addedAt }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ... (TMDB Key Warning Box)

        // Search & Sorting controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search watchlist...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp)
            )

            // Sort Selector Button
            var sortExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1.2f)) {
                OutlinedButton(
                    onClick = { sortExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    val sortLabel = when (sortBy) {
                        "alpha" -> "A-Z"
                        "rating" -> "Rating"
                        else -> "Recent"
                    }
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(sortLabel, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Recently Added") },
                        onClick = {
                            sortBy = "added"
                            sortExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Alphabetical A-Z") },
                        onClick = {
                            sortBy = "alpha"
                            sortExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Highest Rated") },
                        onClick = {
                            sortBy = "rating"
                            sortExpanded = false
                        }
                    )
                }
            }
        }

        // Hot-Platform Horizontal Ribbon Filter
        val filterProviders = remember(allProviders) { allProviders.filter { it.isActive || it.costPerMonth == 0.0 } }
        if (filterProviders.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Services:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )

                FilterChip(
                    selected = selectedPlatformId == null,
                    onClick = { selectedPlatformId = null },
                    label = { Text("All Platforms", fontSize = 11.sp) }
                )

                filterProviders.forEach { provider ->
                    FilterChip(
                        selected = selectedPlatformId == provider.id,
                        onClick = {
                            selectedPlatformId = if (selectedPlatformId == provider.id) null else provider.id
                        },
                        label = { Text(provider.name, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Genre Horizontal Ribbon Filter
        if (allGenres.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Genres:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )

                FilterChip(
                    selected = selectedGenre == null,
                    onClick = { selectedGenre = null },
                    label = { Text("All Genres", fontSize = 11.sp) }
                )

                allGenres.forEach { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = {
                            selectedGenre = if (selectedGenre == genre) null else genre
                        },
                        label = { Text(genre, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Filter Selection Pills + Sync button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !filterOnlyMyServices && !showFreeOnly,
                onClick = { 
                    onFilterToggle(false)
                    showFreeOnly = false
                },
                label = { Text("All", fontSize = 11.sp) },
                modifier = Modifier.testTag("filter_all_chip")
            )

            FilterChip(
                selected = filterOnlyMyServices,
                onClick = { 
                    onFilterToggle(!filterOnlyMyServices)
                    if (!filterOnlyMyServices) showFreeOnly = false
                },
                label = { Text("My Services", fontSize = 11.sp) },
                leadingIcon = { if (filterOnlyMyServices) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.testTag("filter_subscribed_chip")
            )

            FilterChip(
                selected = showFreeOnly,
                onClick = { 
                    showFreeOnly = !showFreeOnly
                    if (showFreeOnly) onFilterToggle(false)
                },
                label = { Text("Free w/ Ads", fontSize = 11.sp) },
                leadingIcon = { if (showFreeOnly) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.testTag("filter_free_chip")
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onSyncClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("sync_providers_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync streaming availability from TMDB",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (processedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Empty list",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No titles in this category.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (filterOnlyMyServices) "Try activating more subscriptions in 'My Services' or share links directly into Stream Manager."
                        else "Tap the FAB or share titles from utilities to start indexing of movies/shows.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(processedItems, key = { it.id }) { item ->
                    MediaItemCard(
                        item = item,
                        allProviders = allProviders,
                        onWatchClick = { onWatchClick(item) },
                        onDeleteClick = { onDeleteClick(item) },
                        onMovieClick = { onMovieClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun MediaItemCard(
    item: MediaItem,
    allProviders: List<StreamingProvider>,
    onWatchClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMovieClick: () -> Unit
) {
    val activeSubscribedIds = remember(allProviders) {
        allProviders.filter { it.isActive }.map { it.id }.toSet()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMovieClick() }
            .testTag("media_item_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // TMDB Poster image display
                if (!item.imageUrl.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = item.imageUrl,
                        contentDescription = "Poster artwork",
                        modifier = Modifier
                            .size(width = 65.dp, height = 95.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete item",
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // TMDB Rating Display if available
                    if (item.rating != null && item.rating > 0.0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFD700), // Gold
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", item.rating),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            if (!item.genres.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.genres,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else if (!item.genres.isNullOrEmpty()) {
                        Text(
                            text = item.genres,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    if (item.status == MediaStatus.PENDING_METADATA.name) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("matching", fontSize = 9.sp)
                            }
                        }
                    }

                    // Vibe Match & Source Badges
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Extract relevance score from YAML trivia if present
                        val relevanceScore = remember(item.trivia) {
                            item.trivia?.lines()
                                ?.find { it.contains("personal_relevance_score:") }
                                ?.substringAfter(":")
                                ?.trim()
                                ?.replace("\"", "")
                                ?.replace("'", "")
                                ?.replace("[", "")
                                ?.replace("]", "")
                        }

                        if (!relevanceScore.isNullOrEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Favorite, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Match: $relevanceScore/10", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }

                        if (!item.importSource.isNullOrEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.importSource,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (!item.overview.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.overview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streaming Providers availability badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val providers = item.providersList
                    if (providers.isEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                "No specified services",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        // Limit display of providers to 3 to avoid overflow/wrapping bugs
                        providers.take(3).forEach { pId ->
                            val fullProvider = allProviders.find { it.id == pId }
                            val isSubscribed = activeSubscribedIds.contains(pId)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSubscribed) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = if (isSubscribed) {
                                    null
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                },
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSubscribed) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = fullProvider?.name ?: pId.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSubscribed) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                         } else {
                                            MaterialTheme.colorScheme.outline
                                         },
                                        fontWeight = if (isSubscribed) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        if (providers.size > 3) {
                            Text(
                                text = "+${providers.size - 3} more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                // INTENT TRIGGER: Watch now button
                Button(
                    onClick = onWatchClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Watch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WatchedTabContent(
    watchedItems: List<MediaItem>,
    allProviders: List<StreamingProvider>,
    onMovieClick: (MediaItem) -> Unit,
    onDeleteClick: (MediaItem) -> Unit,
    onSyncClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("timeline") } // "timeline", "alpha", "rating"
    var selectedGenre by remember { mutableStateOf<String?>(null) }

    val allGenres = remember(watchedItems) {
        watchedItems.flatMap { it.genres?.split(",")?.map { g -> g.trim() } ?: emptyList() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    val filteredItems = remember(watchedItems, searchQuery, selectedGenre) {
        watchedItems.filter { item ->
            val matchesSearch = item.title.contains(searchQuery, ignoreCase = true)
            val matchesGenre = selectedGenre == null || item.genres?.contains(selectedGenre!!, ignoreCase = true) == true
            matchesSearch && matchesGenre
        }
    }

    val processedItems = remember(filteredItems, sortBy) {
        when (sortBy) {
            "alpha" -> filteredItems.sortedBy { it.title.lowercase() }
            "rating" -> filteredItems.sortedByDescending { it.rating ?: 0.0 }
            else -> filteredItems.sortedByDescending { it.watchedAt ?: it.updatedAt }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search & Sorting controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search history...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp)
            )

            // Sort Selector
            var sortExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1.2f)) {
                OutlinedButton(
                    onClick = { sortExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val sortLabel = when (sortBy) {
                        "alpha" -> "A-Z"
                        "rating" -> "Rating"
                        else -> "Timeline"
                    }
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(sortLabel, fontSize = 12.sp)
                }
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    DropdownMenuItem(text = { Text("Watched Timeline") }, onClick = { sortBy = "timeline"; sortExpanded = false })
                    DropdownMenuItem(text = { Text("Alphabetical A-Z") }, onClick = { sortBy = "alpha"; sortExpanded = false })
                    DropdownMenuItem(text = { Text("Highest Rated") }, onClick = { sortBy = "rating"; sortExpanded = false })
                }
            }
        }

        // Genre Horizontal Ribbon
        if (allGenres.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedGenre == null,
                    onClick = { selectedGenre = null },
                    label = { Text("All Genres", fontSize = 11.sp) }
                )
                allGenres.forEach { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = { selectedGenre = if (selectedGenre == genre) null else genre },
                        label = { Text(genre, fontSize = 11.sp) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${processedItems.size} Titles Logged",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onSyncClick) {
                Icon(Icons.Default.Refresh, contentDescription = "Enrich metadata", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }

        if (processedItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Your cinematic vault is empty.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(processedItems, key = { it.id }) { item ->
                    MediaItemCard(
                        item = item,
                        allProviders = allProviders,
                        onWatchClick = { /* No-op for watched */ },
                        onDeleteClick = { onDeleteClick(item) },
                        onMovieClick = { onMovieClick(item) }
                    )
                }
            }
        }
    }
}
// ==========================================
@Composable
fun MonthlyRoiContent(
    monthlyStats: List<ProviderUsageStats>,
    allProviders: List<StreamingProvider>
) {
    val activeSubscribed = remember(allProviders) { allProviders.filter { it.isActive } }
    val totalCost = remember(activeSubscribed) { activeSubscribed.sumOf { it.costPerMonth } }
    
    // Sort active channels by costPerHour descending (worst value!) to bubble up prime pausing candidates.
    val worstValueProviders = remember(monthlyStats) {
        monthlyStats.sortedByDescending { it.costPerHour }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Summary Budget card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Monthly Streaming Burn Rate",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$${String.format("%.2f", totalCost)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "You must watch at least 3 hours per service per month to justify these costs. Services are ranked below from least optimized to highest value.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        item {
            Text(
                "Subscription Value Analytics (This Month)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (worstValueProviders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Text(
                        "To analyze ROI, please configure your active streaming services in the 'My Services' tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(worstValueProviders, key = { it.providerId }) { stats ->
                val isPrimeCancelCandidate = stats.totalHours < 3.0
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPrimeCancelCandidate) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    ),
                    border = if (isPrimeCancelCandidate) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    } else null,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stats.providerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isPrimeCancelCandidate) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text("UNJUSTIFIED", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "Watched: ${String.format("%.1f", stats.totalHours)}h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    "Cost: $${stats.costPerMonth}/mo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "$${String.format("%.2f", stats.costPerHour)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isPrimeCancelCandidate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "per hour",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPOSABLE: Manage Services Tab
// ==========================================
@Composable
fun ManageServicesTabContent(
    allProviders: List<StreamingProvider>,
    onProviderToggle: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "My Subscriptions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Toggle services you currently pay for. This optimizes the watchlist filtering and values calculations.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(allProviders) { provider ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProviderToggle(provider.id, !provider.isActive) }
                        .testTag("provider_card_${provider.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (provider.isActive) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (provider.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (provider.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (provider.isActive) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                                contentDescription = null,
                                tint = if (provider.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = provider.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "$${provider.costPerMonth}/mo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Switch(
                            checked = provider.isActive,
                            onCheckedChange = { onProviderToggle(provider.id, it) },
                            modifier = Modifier.testTag("switch_${provider.id}")
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPOSABLE: Add Media Dialog
// ==========================================
@Composable
fun AddMediaDialog(
    allProviders: List<StreamingProvider>,
    onDismiss: () -> Unit,
    onAdd: (String, List<String>, String?, String?) -> Unit
) {
    var titlesInput by remember { mutableStateOf("") }
    var userNotes by remember { mutableStateOf("") }
    var importSource by remember { mutableStateOf("") }
    val selectedProviders = remember { mutableStateListOf<String>() }
    val parsedTitles = remember(titlesInput) {
        titlesInput
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Title(s)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = titlesInput,
                    onValueChange = { titlesInput = it },
                    label = { Text("Movie / Show Titles") },
                    placeholder = { Text("One title per line\nSeverance\nDune: Part Two\nThe Godfather") },
                    singleLine = false,
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("add_input_title"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                OutlinedTextField(
                    value = userNotes,
                    onValueChange = { userNotes = it },
                    label = { Text("Personal Notes (Optional)") },
                    placeholder = { Text("e.g. Danny recommended this") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = importSource,
                    onValueChange = { importSource = it },
                    label = { Text("Source / Origins (Optional)") },
                    placeholder = { Text("e.g. Podcast: The Big Picture") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (parsedTitles.size == 1) {
                        "1 title ready to add"
                    } else {
                        "${parsedTitles.size} titles ready to add"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Text("Available On Support Services:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    items(allProviders) { provider ->
                        val isSelected = selectedProviders.contains(provider.id)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        selectedProviders.remove(provider.id)
                                    } else {
                                        selectedProviders.add(provider.id)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked == true) {
                                            selectedProviders.add(provider.id)
                                        } else {
                                            selectedProviders.remove(provider.id)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(provider.name, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedTitles.isNotEmpty()) {
                        onAdd(titlesInput, selectedProviders.toList(), userNotes, importSource)
                    }
                },
                enabled = parsedTitles.isNotEmpty(),
                modifier = Modifier.testTag("add_dialog_confirm")
            ) {
                Text(if (parsedTitles.size > 1) "Add ${parsedTitles.size} Titles" else "Add to Watchlist")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==========================================
// COMPOSABLE: Check-In Foreground Sheet
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInBottomSheet(
    item: MediaItem,
    allProviders: List<StreamingProvider>,
    onDismiss: () -> Unit,
    onLoggedFinished: (duration: Int, providerId: String?, notes: String?) -> Unit,
    onLoggedPartial: (duration: Int, providerId: String?, notes: String?) -> Unit,
    onLoggedSomethingElse: (customTitle: String, duration: Int, providerId: String?) -> Unit,
    onLoggedNothing: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("check_in_bottom_sheet")
    ) {
        var flowStep by remember { mutableStateOf(0) } // 0: Question Menu, 1: Log Finished, 2: Log Partial, 3: Log Something Else

        // Inputs
        var durationInput by remember { mutableStateOf("120") }
        var notesInput by remember { mutableStateOf("") }
        var otherTitleInput by remember { mutableStateOf("") }
        var selectedProviderId by remember { 
            mutableStateOf(item.providersList.firstOrNull() ?: allProviders.firstOrNull { it.isActive }?.id) 
        }

        val focusManager = LocalFocusManager.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                "Foreground Check-In",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = flowStep,
                label = "check_in_step_transition"
            ) { step ->
                when (step) {
                    0 -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Did you end up watching \"${item.title}\"?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        Button(
                            onClick = { flowStep = 1 },
                            modifier = Modifier.fillMaxWidth().testTag("check_in_yes"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Yes, finished it! (Logged to History)")
                        }

                        Button(
                            onClick = { flowStep = 2 },
                            modifier = Modifier.fillMaxWidth().testTag("check_in_partial"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Watched some of it (Logged partial time)")
                        }

                        Button(
                            onClick = { flowStep = 3 },
                            modifier = Modifier.fillMaxWidth().testTag("check_in_else"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Watched something else")
                        }

                        OutlinedButton(
                            onClick = onLoggedNothing,
                            modifier = Modifier.fillMaxWidth().testTag("check_in_nothing"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Didn't watch anything")
                        }
                    }

                    1 -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Finished \"${item.title}\"",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = durationInput,
                            onValueChange = { durationInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Duration Watched (Minutes)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("input_duration")
                        )

                        Text("Which service did you watch this on?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        ProviderSelector(
                            allProviders = allProviders,
                            selectedId = selectedProviderId,
                            onSelect = { selectedProviderId = it }
                        )

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Notes (Optional)") },
                            placeholder = { Text("e.g. Loved the plot twist!") },
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { flowStep = 0 },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    val duration = durationInput.toIntOrNull() ?: 0
                                    onLoggedFinished(duration, selectedProviderId, notesInput)
                                },
                                enabled = durationInput.isNotBlank(),
                                modifier = Modifier.weight(1f).testTag("btn_save_finish")
                            ) {
                                Text("Save logs")
                            }
                        }
                    }

                    2 -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Partial watch feedback for \"${item.title}\"",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = durationInput,
                            onValueChange = { durationInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Minutes Watched") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("input_duration_partial")
                        )

                        Text("Which service did you watch this on?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        ProviderSelector(
                            allProviders = allProviders,
                            selectedId = selectedProviderId,
                            onSelect = { selectedProviderId = it }
                        )

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Session notes") },
                            placeholder = { Text("e.g. Watched first 30 mins") },
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { flowStep = 0 },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    val duration = durationInput.toIntOrNull() ?: 0
                                    onLoggedPartial(duration, selectedProviderId, notesInput)
                                },
                                enabled = durationInput.isNotBlank(),
                                modifier = Modifier.weight(1f).testTag("btn_save_partial")
                            ) {
                                Text("Log session")
                            }
                        }
                    }

                    3 -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "What did you watch instead?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = otherTitleInput,
                            onValueChange = { otherTitleInput = it },
                            label = { Text("What did you watch?") },
                            placeholder = { Text("Movie or Show Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_other_title")
                        )

                        OutlinedTextField(
                            value = durationInput,
                            onValueChange = { durationInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Duration (Minutes)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Which service did you watch this on?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        ProviderSelector(
                            allProviders = allProviders,
                            selectedId = selectedProviderId,
                            onSelect = { selectedProviderId = it }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { flowStep = 0 },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    val duration = durationInput.toIntOrNull() ?: 0
                                    onLoggedSomethingElse(otherTitleInput.trim(), duration, selectedProviderId)
                                },
                                enabled = otherTitleInput.isNotBlank() && durationInput.isNotBlank(),
                                modifier = Modifier.weight(1f).testTag("btn_save_else")
                            ) {
                                Text("Log session")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderSelector(
    allProviders: List<StreamingProvider>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    val activeList = remember(allProviders) { 
        allProviders.filter { it.isActive || it.costPerMonth == 0.0 } 
    }
    
    if (activeList.isEmpty()) {
        Text("No active platforms configured. Setting log as standard watch time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            activeList.forEach { provider ->
                val isSelected = provider.id == selectedId
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .clickable { onSelect(provider.id) }
                        .testTag("select_prov_${provider.id}")
                ) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPOSABLE: Settings Dialog
// ==========================================
@Composable
fun SettingsDialog(
    allProviders: List<StreamingProvider>,
    onProviderToggle: (String, Boolean) -> Unit,
    tmdbApiKey: String,
    onSaveTmdbApiKey: (String) -> Unit,
    watchmodeApiKey: String,
    onSaveWatchmodeApiKey: (String) -> Unit,
    ollamaHost: String,
    onSaveOllamaHost: (String) -> Unit,
    githubToken: String,
    onSaveGithubToken: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0: Subscriptions, 1: APIs (TMDB/Watchmode), 2: AI (Ollama) & About

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close Settings")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                TabRow(
                    selectedTabIndex = activeSubTab,
                    containerColor = Color.Transparent,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Tab(
                        selected = activeSubTab == 0,
                        onClick = { activeSubTab = 0 },
                        text = { Text("Subs", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = activeSubTab == 1,
                        onClick = { activeSubTab = 1 },
                        text = { Text("APIs", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = activeSubTab == 2,
                        onClick = { activeSubTab = 2 },
                        text = { Text("AI/Local", fontSize = 11.sp) }
                    )
                }

                when (activeSubTab) {
                    0 -> {
                        val scrollState = rememberScrollState()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                "Manage Subscriptions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Toggle active services you currently pay for. Free services (Tubi, Freevee, Pluto TV) are free and enabled by default.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            allProviders.forEach { provider ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = provider.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (provider.costPerMonth > 0) "$${provider.costPerMonth}/mo" else "Free Platform ($0.0)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Switch(
                                            checked = provider.isActive,
                                            onCheckedChange = { onProviderToggle(provider.id, it) },
                                            modifier = Modifier.testTag("dialog_switch_${provider.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        var tmdbInput by remember(tmdbApiKey) { mutableStateOf(tmdbApiKey) }
                        var wmInput by remember(watchmodeApiKey) { mutableStateOf(watchmodeApiKey) }
                        var showTmdb by remember { mutableStateOf(false) }
                        var showWm by remember { mutableStateOf(false) }
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        val scrollState = rememberScrollState()

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                "Provider Data Sources",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // TMDB Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("TMDB API (Primary)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Text("Provides metadata, posters, and primary streaming status.", style = MaterialTheme.typography.bodySmall)
                                    
                                    OutlinedTextField(
                                        value = tmdbInput,
                                        onValueChange = { tmdbInput = it },
                                        label = { Text("TMDB Key") },
                                        singleLine = true,
                                        visualTransformation = if (showTmdb) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showTmdb = !showTmdb }) {
                                                Icon(imageVector = if (showTmdb) Icons.Default.Clear else Icons.Default.Search, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Button(
                                        onClick = { onSaveTmdbApiKey(tmdbInput) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Save TMDB Key", fontSize = 12.sp)
                                    }
                                }
                            }

                            // Watchmode Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Watchmode API (Fallback)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Text("Deep-link availability source. Used when TMDB data is missing.", style = MaterialTheme.typography.bodySmall)
                                    
                                    OutlinedTextField(
                                        value = wmInput,
                                        onValueChange = { wmInput = it },
                                        label = { Text("Watchmode Key") },
                                        singleLine = true,
                                        visualTransformation = if (showWm) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showWm = !showWm }) {
                                                Icon(imageVector = if (showWm) Icons.Default.Clear else Icons.Default.Search, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Button(
                                        onClick = { onSaveWatchmodeApiKey(wmInput) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Text("Save Watchmode Key", fontSize = 12.sp)
                                    }
                                    
                                    TextButton(onClick = { uriHandler.openUri("https://api.watchmode.com/") }) {
                                        Text("Get Watchmode API Key", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        val context = LocalContext.current
                        var hostInput by remember(ollamaHost) { mutableStateOf(ollamaHost) }
                        var tokenInput by remember(githubToken) { mutableStateOf(githubToken) }
                        var showToken by remember { mutableStateOf(false) }
                        val scrollState = rememberScrollState()
                        
                        // Founder's Manual Loader
                        val manualHtml = remember {
                            try {
                                context.assets.open("readme.html").bufferedReader().readText()
                            } catch (e: Exception) {
                                "Manual file not found."
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                "Local AI Synthesis (Ollama)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Synthesis 2.0 uses your local Ollama instance (Gemma 4) to analyze watch history for personalized research.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = hostInput,
                                onValueChange = { hostInput = it },
                                label = { Text("Ollama Host IP") },
                                placeholder = { Text("e.g. 192.168.1.100") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = { onSaveOllamaHost(hostInput) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Ollama Host")
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Text(
                                "Synthesis 4.0: Self-Evolving App",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Connect a GitHub Personal Access Token to allow the Agent Chat to submit feature requests directly to the repository.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
                                label = { Text("GitHub Token (PAT)") },
                                placeholder = { Text("ghp_...") },
                                singleLine = true,
                                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showToken = !showToken }) {
                                        Icon(
                                            imageVector = if (showToken) Icons.Default.Clear else Icons.Default.Search,
                                            contentDescription = if (showToken) "Hide token" else "Show token"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = { onSaveGithubToken(tokenInput) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save GitHub Token")
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Text(
                                "Founder's Manual & Roadmap",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Basic HTML renderer (strips tags for standard Text view, 
                            // though full WebView would be better for complex styles)
                            val cleanText = remember(manualHtml) {
                                manualHtml.replace(Regex("<[^>]*>"), "")
                                    .replace("&nbsp;", " ")
                                    .trim()
                            }
                            
                            Text(
                                text = cleanText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}


// ==========================================
// COMPOSABLE: Movie Details Bottom Sheet
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailsBottomSheet(
    item: MediaItem,
    allProviders: List<StreamingProvider>,
    discoveredDevices: List<CastDevice>,
    onCastClick: (CastDevice, MediaItem) -> Unit,
    onDismiss: () -> Unit,
    onWatchClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        modifier = Modifier.testTag("movie_details_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Row with poster and titles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (!item.imageUrl.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = item.imageUrl,
                        contentDescription = "Movie Poster",
                        modifier = Modifier
                            .size(width = 110.dp, height = 160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    if (item.rating != null && item.rating > 0.0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${String.format("%.1f", item.rating)}/10",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val statusLabel = when (item.status) {
                        MediaStatus.PENDING_METADATA.name -> "Matching TMDB..."
                        MediaStatus.WATCHLIST.name -> "In Watchlist"
                        MediaStatus.INTENDING_TO_WATCH.name -> "Watching Now"
                        MediaStatus.WATCHED.name -> "Watched!"
                        else -> item.status
                    }
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // --- UNIVERSAL CASTING ENGINE ---
            if (discoveredDevices.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Text(
                    text = "Cast to Living Room Device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    discoveredDevices.forEach { device ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { onCastClick(device, item) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(device.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Text(device.ip, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }

            // Stream availability
            Text(
                text = "Currently Available On:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val providers = item.providersList
            if (providers.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Not streaming on any tracked subscriptions or free channels.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    providers.forEach { pId ->
                        val provider = allProviders.find { it.id == pId }
                        val isSubscribed = provider?.isActive == true

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSubscribed) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSubscribed) Icons.Default.Check else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isSubscribed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = provider?.name ?: pId.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSubscribed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Movie Plot / Overview
            Text(
                text = "Storyline / Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = item.overview ?: "No synopsis description fetched for this film.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            // User Personal Notes & Source Section
            if (!item.userNotes.isNullOrEmpty() || !item.importSource.isNullOrEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                Text(
                    text = "Personal Context & Origins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (!item.userNotes.isNullOrEmpty()) {
                    Text(
                        text = "Notes: ${item.userNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!item.importSource.isNullOrEmpty()) {
                    Text(
                        text = "Imported From: ${item.importSource}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Agent Research / Trivia Section
            if (!item.trivia.isNullOrEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Agentic Research Strategy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Research Notes",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = item.trivia,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Action Items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Letterboxd link if available
                if (!item.sharedUrl.isNullOrEmpty()) {
                    OutlinedButton(
                        onClick = { uriHandler.openUri(item.sharedUrl) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Source", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = {
                        onDismiss()
                        onWatchClick()
                    },
                    modifier = Modifier.weight(1.2f).testTag("detail_watch_now_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Watch Now", fontSize = 12.sp)
                }

                IconButton(
                    onClick = {
                        onDismiss()
                        onDeleteClick()
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete title")
                }
            }
        }
    }
}

// ==========================================
// COMPOSABLE: Agent Chat Tab
// ==========================================
@Composable
fun AgentChatTabContent(
    chatMessages: List<com.example.data.remote.OllamaChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit
) {
    var inputMessage by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Olivia (Gemma 4)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Ask me about your Vault, or ask for recommendations based on your history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Chat History
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatMessages.filter { it.role != "system" }) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = msg.content,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
            if (isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Surface(
                            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                "Thinking...",
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Input Field
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                placeholder = { Text("What should I watch tonight?") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            FloatingActionButton(
                onClick = {
                    onSendMessage(inputMessage)
                    inputMessage = ""
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send message")
            }
        }
    }
}

