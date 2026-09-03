package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
    val googleSheetWebhookUrl by viewModel.googleSheetWebhookUrl.collectAsState()
    val fireTvIp by viewModel.fireTvIp.collectAsState()
    val githubToken by viewModel.githubToken.collectAsState()
    val recentlyDeletedItem by viewModel.recentlyDeletedItem.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) } // 0: Watchlist, 1: Watched, 2: ROI Stats, 3: Agent
    var filterOnlyMyServices by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showQuickLogDialog by remember { mutableStateOf(false) }
    var quickLogInitialMovie by remember { mutableStateOf<MediaItem?>(null) }
    var watchActionItem by remember { mutableStateOf<MediaItem?>(null) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var detailMovieItem by remember { mutableStateOf<MediaItem?>(null) }
    var editingProvider by remember { mutableStateOf<StreamingProvider?>(null) }
    var showAddServiceDialog by remember { mutableStateOf(false) }

    // Clear and display Toast/Status banners beautifully with UNDO support
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            if (recentlyDeletedItem != null) {
                val result = snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoDelete()
                }
            } else {
                snackbarHostState.showSnackbar(msg)
            }
            viewModel.clearStatusMessage()
        }
    }

    val showcaseState = remember { ShowcaseState() }

    CompositionLocalProvider(LocalShowcaseState provides showcaseState) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        onClick = { viewModel.syncWithGoogleSheet() },
                        modifier = Modifier.testTag("sync_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync with Google Sheet",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.exportToLetterboxdCsv() },
                        modifier = Modifier.testTag("export_letterboxd_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export for Letterboxd (CSV)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showGuideDialog = true },
                        modifier = Modifier.testTag("guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "User Guide",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .testTag("settings_gear_button")
                            .showcaseTarget(
                                "settings_gear", 
                                "Settings & Subscriptions", 
                                "Opens the settings menu where you can manage your active streaming services, set custom trial pricing, and configure API keys.",
                                "Use this whenever you start a new free trial, cancel a service, or need to connect your local AI."
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
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
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.navigationBarsPadding()
            ) {
                // Floating Feedback Button (Do-It-Now pipeline)
                FloatingFeedbackButton(
                    onClick = { showFeedbackDialog = true }
                )

                if (selectedTab == 0) {
                    ExtendedFloatingActionButton(
                        text = { Text("Add to Queue") },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add media item") },
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .testTag("add_item_fab")
                            .showcaseTarget(
                                "fab_add", 
                                "Add Movies & Shows", 
                                "Opens a search window connected to TMDB where you can find and add any movie or TV show to your Watchlist.",
                                "Use this whenever you hear a recommendation from a friend or see a trailer for something you want to watch later."
                            ),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else if (selectedTab == 1) {
                    ExtendedFloatingActionButton(
                        text = { Text("Quick Log Film") },
                        icon = { Icon(Icons.Default.Check, contentDescription = "Log watched film") },
                        onClick = {
                            quickLogInitialMovie = null
                            showQuickLogDialog = true
                        },
                        modifier = Modifier.testTag("quick_log_fab"),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
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
                    modifier = Modifier
                        .testTag("tab_watchlist")
                        .showcaseTarget(
                            "watchlist_tab", 
                            "The Watchlist", 
                            "Your central queue. It pulls live availability data from Watchmode so you know exactly which of your services has the movie right now.",
                            "Use this as your primary dashboard to see what's ready to watch tonight."
                        )
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
                    modifier = Modifier
                        .testTag("tab_budget")
                        .showcaseTarget(
                            "roi_tab", 
                            "Budget & ROI Tracker", 
                            "Displays a dashboard of your monthly streaming burn rate. It calculates the cost per hour of each service based on your watch history.",
                            "Use this at the end of the month to identify 'Cancel Candidates' (services you aren't using enough) and save money."
                        )
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Agent", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "AI Agent tab") },
                    modifier = Modifier
                        .testTag("tab_agent")
                        .showcaseTarget(
                            "agent_tab", 
                            "Private AI Chatbot", 
                            "Connects directly to your home Ollama server (e.g. llama3) to provide completely private, localized recommendations without sending your data to the cloud.",
                            "Use this when you have no idea what to watch and want hyper-specific suggestions, like 'a 90s thriller on Hulu under 2 hours'."
                        )
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
                        onWatchClick = { watchActionItem = it },
                        onDeleteClick = { viewModel.deleteItem(it) },
                        onSyncClick = { viewModel.syncWithGoogleSheet() },
                        tmdbApiKey = tmdbApiKey,
                        onOpenSettings = { showSettingsDialog = true },
                        onMovieClick = { detailMovieItem = it },
                        recentlyDeletedItem = recentlyDeletedItem,
                        onUndoDelete = { viewModel.undoDelete() },
                        onDismissUndo = { viewModel.dismissUndo() }
                    )
                    1 -> {
                        val watchedItems by viewModel.watchedItems.collectAsState()
                        WatchedTabContent(
                            watchedItems = watchedItems,
                            allProviders = allProviders,
                            onMovieClick = { detailMovieItem = it },
                            onDeleteClick = { viewModel.deleteItem(it) },
                            onSyncClick = { viewModel.syncWithGoogleSheet() },
                            onQuickLogClick = {
                                quickLogInitialMovie = null
                                showQuickLogDialog = true
                            },
                            onExportLetterboxdClick = { viewModel.exportToLetterboxdCsv() }
                        )
                    }
                    2 -> MonthlyRoiContent(
                        monthlyStats = monthlyStats,
                        allProviders = allProviders,
                        watchlistItems = watchlistItems,
                        onToggleProvider = { id, active -> viewModel.toggleStreamingProvider(id, active) },
                        onEditProvider = { editingProvider = it },
                        onAddServiceClick = { showAddServiceDialog = true },
                        onMovieClick = { detailMovieItem = it }
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
                onProviderUpdate = { id, active, cost, start, end -> viewModel.updateStreamingProviderSettings(id, active, cost, start, end) },
                tmdbApiKey = tmdbApiKey,
                onSaveTmdbApiKey = { viewModel.saveTmdbApiKey(it) },
                watchmodeApiKey = watchmodeApiKey,
                onSaveWatchmodeApiKey = { viewModel.saveWatchmodeApiKey(it) },
                ollamaHost = ollamaHost,
                onSaveOllamaHost = { viewModel.saveOllamaHost(it) },
                githubToken = githubToken,
                onSaveGithubToken = { viewModel.saveGithubToken(it) },
                googleSheetWebhookUrl = googleSheetWebhookUrl,
                onSaveGoogleSheetWebhookUrl = { viewModel.saveGoogleSheetWebhookUrl(it) },
                fireTvIp = fireTvIp,
                onSaveFireTvIp = { viewModel.saveFireTvIp(it) },
                onSyncGoogleSheet = { viewModel.syncWithGoogleSheet() },
                onExportLetterboxd = { viewModel.exportToLetterboxdCsv() },
                onSimulateResume = simulateForegroundReturn,
                onDismiss = { showSettingsDialog = false }
            )
        }
        
        // Guide Dialog
        if (showGuideDialog) {
            GuideDialog(
                onDismiss = { showGuideDialog = false },
                onStartTour = {
                    showcaseState.enableGuideMode()
                }
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
                    val target = detailMovieItem
                    detailMovieItem = null
                    watchActionItem = target
                },
                onDeleteClick = {
                    viewModel.deleteItem(detailMovieItem!!)
                    detailMovieItem = null
                }
            )
        }

        // Watch Action Sheet (Reworked "Watch Now" action hub)
        if (watchActionItem != null) {
            val discoveredDevices by viewModel.discoveredDevices.collectAsState()
            WatchActionSheet(
                item = watchActionItem!!,
                allProviders = allProviders,
                fireTvIp = fireTvIp,
                discoveredDevices = discoveredDevices,
                onDismiss = { watchActionItem = null },
                onLaunchFireTv = { item, providerId, targetIp ->
                    viewModel.launchOnFireTv(item, providerId, targetIp)
                    watchActionItem = null
                },
                onLaunchPhone = { item, providerId ->
                    viewModel.launchOnPhone(context, item, providerId)
                    watchActionItem = null
                },
                onUniversalCast = { item, providerId ->
                    viewModel.launchUniversalCast(context, item, providerId)
                    watchActionItem = null
                },
                onQuickLog = { item ->
                    val target = item
                    watchActionItem = null
                    quickLogInitialMovie = target
                    showQuickLogDialog = true
                },
                onPinTonight = { item ->
                    viewModel.pinTonight(item)
                    watchActionItem = null
                },
                onSaveFireTvIp = { viewModel.saveFireTvIp(it) },
                onScanDevices = { viewModel.startDeviceDiscovery() }
            )
        }

        // Quick Log Dialog
        if (showQuickLogDialog) {
            QuickLogDialog(
                initialMovie = quickLogInitialMovie,
                allProviders = allProviders,
                onDismiss = {
                    showQuickLogDialog = false
                    quickLogInitialMovie = null
                },
                onSave = { title, year, rating, isRewatch, providerId, durationMinutes, notes ->
                    viewModel.logWatchedMovie(title, year, rating, isRewatch, providerId, durationMinutes, notes)
                    showQuickLogDialog = false
                    quickLogInitialMovie = null
                }
            )
        }

        // Autonomous Feedback Dialog (Do-It-Now pipeline)
        if (showFeedbackDialog) {
            val currentTabName = when (selectedTab) {
                0 -> "Watchlist"
                1 -> "Watched History"
                2 -> "ROI Stats"
                3 -> "Olivia Agent Chat"
                else -> "Main"
            }
            FeedbackDialog(
                githubToken = githubToken,
                webhookUrl = googleSheetWebhookUrl,
                currentTabName = currentTabName,
                watchlistCount = watchlistItems.count { it.status != MediaStatus.WATCHED.name },
                watchedCount = watchlistItems.count { it.status == MediaStatus.WATCHED.name },
                onDismiss = { showFeedbackDialog = false },
                onSubmitSuccess = { /* toast handled */ }
            )
        }

        // Edit Subscription / Pricing / Trial Sheet
        if (editingProvider != null) {
            SubscriptionEditSheet(
                provider = editingProvider!!,
                onDismiss = { editingProvider = null },
                onSave = { id, isActive, cost, start, trial ->
                    viewModel.updateStreamingProviderSettings(id, isActive, cost, start, trial)
                    editingProvider = null
                },
                onDelete = { id ->
                    viewModel.deleteStreamingProvider(id)
                    editingProvider = null
                }
            )
        }

        // Add Custom / Preset Service Dialog
        if (showAddServiceDialog) {
            AddServiceDialog(
                existingProviderNames = allProviders.map { it.name },
                onDismiss = { showAddServiceDialog = false },
                onAdd = { name, cost, isTrial, trialDays ->
                    viewModel.addCustomProvider(name, cost, isTrial, trialDays)
                    showAddServiceDialog = false
                }
            )
        }
    } // End Scaffold

            if (showcaseState.isGuideModeActive) {
                ShowcaseOverlay()
            }
        } // End Box
    } // End CompositionLocalProvider
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
    onMovieClick: (MediaItem) -> Unit,
    recentlyDeletedItem: MediaItem? = null,
    onUndoDelete: () -> Unit = {},
    onDismissUndo: () -> Unit = {}
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
    var runtimeFilter by remember { mutableStateOf<Int?>(null) } // null: All, 90: <90m, 120: <120m

    val allGenres = remember(watchlistItems) {
        watchlistItems.flatMap { it.genres?.split(",")?.map { g -> g.trim() } ?: emptyList() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    // Filter items according to state
    val filteredItems = remember(watchlistItems, filterOnlyMyServices, activeProviderIds, selectedPlatformId, showFreeOnly, selectedGenre, runtimeFilter) {
        watchlistItems.filter { item ->
            // Exclude already watched from immediate watchlist
            if (item.status == MediaStatus.WATCHED.name) return@filter false

            if (selectedPlatformId != null) {
                if (item.providersList.contains(selectedPlatformId) != true) return@filter false
            }

            if (selectedGenre != null) {
                if (item.genres?.contains(selectedGenre!!, ignoreCase = true) != true) return@filter false
            }

            if (runtimeFilter != null) {
                val mins = item.runtimeMinutes
                if (mins == null || mins > runtimeFilter!!) return@filter false
            }

            if (showFreeOnly) {
                val provs = item.providersList
                if (provs.none { freeProviderIds.contains(it) }) return@filter false
            }

            if (filterOnlyMyServices) {
                // Return items having at least one of their available platforms as locally active/subscribed
                val provs = item.providersList
                provs.isNotEmpty() && provs.any { activeProviderIds.contains(it) }
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
            "runtime" -> items.sortedBy { it.runtimeMinutes ?: 999 }
            else -> items.sortedByDescending { it.addedAt }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Undo Banner for recently deleted items
        AnimatedVisibility(
            visible = recentlyDeletedItem != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.inversePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Deleted \"${recentlyDeletedItem?.title}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onUndoDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary)
                        ) {
                            Text("UNDO", fontWeight = FontWeight.Black)
                        }
                        IconButton(
                            onClick = onDismissUndo,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
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
                        "runtime" -> "Duration"
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
                    DropdownMenuItem(
                        text = { Text("Shortest Duration") },
                        onClick = {
                            sortBy = "runtime"
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

            FilterChip(
                selected = runtimeFilter == 90,
                onClick = { runtimeFilter = if (runtimeFilter == 90) null else 90 },
                label = { Text("< 90m", fontSize = 11.sp, fontWeight = if (runtimeFilter == 90) FontWeight.Bold else FontWeight.Normal) },
                leadingIcon = {
                    if (runtimeFilter == 90) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                },
                modifier = Modifier.testTag("filter_runtime_90_chip")
            )

            FilterChip(
                selected = runtimeFilter == 120,
                onClick = { runtimeFilter = if (runtimeFilter == 120) null else 120 },
                label = { Text("< 120m", fontSize = 11.sp, fontWeight = if (runtimeFilter == 120) FontWeight.Bold else FontWeight.Normal) },
                leadingIcon = {
                    if (runtimeFilter == 120) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                },
                modifier = Modifier.testTag("filter_runtime_120_chip")
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    if (processedItems.isNotEmpty()) {
                        onMovieClick(processedItems.random())
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("surprise_me_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Pick random film from filter",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

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

        if (runtimeFilter != null || filterOnlyMyServices || showFreeOnly || selectedPlatformId != null || selectedGenre != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${processedItems.size} ${if (processedItems.size == 1) "movie" else "movies"} matching filters",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        runtimeFilter = null
                        showFreeOnly = false
                        selectedPlatformId = null
                        selectedGenre = null
                        onFilterToggle(false)
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text("Clear all filters", fontSize = 11.sp)
                }
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

                    // User Rating or TMDB Rating Display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        if (item.userRating != null) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Your Letterboxd Rating",
                                tint = Color(0xFFFF8C00),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "★ ${String.format("%.1f", item.userRating)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else if (item.rating != null && item.rating > 0.0) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "TMDB Rating",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", item.rating),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (item.releaseYear != null) {
                            Text(
                                text = "${item.releaseYear}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (item.isRewatch) {
                            Text(
                                text = "↻ Rewatch",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (item.runtimeMinutes != null && item.runtimeMinutes > 0) {
                            Text(
                                text = "${item.runtimeMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (!item.genres.isNullOrEmpty()) {
                            Text(
                                text = "• ${item.genres}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
    onSyncClick: () -> Unit,
    onQuickLogClick: () -> Unit,
    onExportLetterboxdClick: () -> Unit
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

            IconButton(onClick = onQuickLogClick) {
                Icon(Icons.Default.Add, contentDescription = "Quick Log Film", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }

            IconButton(onClick = onExportLetterboxdClick) {
                Icon(Icons.Default.Share, contentDescription = "Export Letterboxd CSV", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }

            IconButton(onClick = onSyncClick) {
                Icon(Icons.Default.Refresh, contentDescription = "Sync with Google Sheet", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
enum class RoiViewTab {
    WATCHLIST_OPTIMIZER,
    SPEND_ANALYTICS
}

data class ProviderWatchlistCoverage(
    val provider: StreamingProvider,
    val matchingItems: List<MediaItem>,
    val count: Int,
    val costPerTitle: Double?
)

@Composable
fun MonthlyRoiContent(
    monthlyStats: List<ProviderUsageStats>,
    allProviders: List<StreamingProvider>,
    watchlistItems: List<MediaItem>,
    onToggleProvider: (String, Boolean) -> Unit,
    onEditProvider: (StreamingProvider) -> Unit,
    onAddServiceClick: () -> Unit,
    onMovieClick: (MediaItem) -> Unit
) {
    var selectedViewTab by remember { mutableStateOf(RoiViewTab.WATCHLIST_OPTIMIZER) }
    var filterMode by remember { mutableStateOf("ALL") } // ALL, ACTIVE, INACTIVE

    val activeSubscribed = remember(allProviders) { allProviders.filter { it.isActive } }
    val inactiveProviders = remember(allProviders) { allProviders.filter { !it.isActive } }
    val totalCost = remember(activeSubscribed) { activeSubscribed.sumOf { it.userCostPerMonth ?: it.costPerMonth } }
    
    // Sort active channels by costPerHour descending (worst value!)
    val worstValueProviders = remember(monthlyStats) {
        monthlyStats.sortedByDescending { it.costPerHour }
    }
    
    val potentialSavings = remember(worstValueProviders) {
        worstValueProviders.filter { it.totalHours < 3.0 && it.isActive }.sumOf { it.effectiveCostPerMonth }
    }

    // Calculate Watchlist matches per provider for recommendation engine
    val activeWatchlist = remember(watchlistItems) {
        watchlistItems.filter { it.status == MediaStatus.WATCHLIST.name }
    }

    val providerCoverages = remember(allProviders, activeWatchlist) {
        allProviders.map { provider ->
            val matchingItems = activeWatchlist.filter { item ->
                item.providersList.contains(provider.id) ||
                item.providersList.any { it.contains(provider.id, ignoreCase = true) || provider.name.contains(it, ignoreCase = true) }
            }
            val effectiveCost = provider.userCostPerMonth ?: provider.costPerMonth
            val costPerTitle = if (matchingItems.isNotEmpty() && effectiveCost > 0) effectiveCost / matchingItems.size else if (effectiveCost == 0.0) 0.0 else null
            ProviderWatchlistCoverage(
                provider = provider,
                matchingItems = matchingItems,
                count = matchingItems.size,
                costPerTitle = costPerTitle
            )
        }.sortedWith(
            compareByDescending<ProviderWatchlistCoverage> { it.count }
                .thenBy { it.costPerTitle ?: Double.MAX_VALUE }
        )
    }

    // Identify strategic recommendations:
    val topInactiveOpportunity = remember(providerCoverages) {
        providerCoverages.firstOrNull { !it.provider.isActive && it.count > 0 }
    }

    val topSafeToPause = remember(providerCoverages) {
        providerCoverages.firstOrNull { coverage ->
            coverage.provider.isActive && 
            (coverage.provider.userCostPerMonth ?: coverage.provider.costPerMonth) > 0.0 &&
            coverage.count <= 1
        }
    }

    val filteredCoverages = remember(providerCoverages, filterMode) {
        when (filterMode) {
            "ACTIVE" -> providerCoverages.filter { it.provider.isActive }
            "INACTIVE" -> providerCoverages.filter { !it.provider.isActive }
            else -> providerCoverages
        }
    }

    var showInactiveSection by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top View Mode Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedViewTab == RoiViewTab.WATCHLIST_OPTIMIZER,
                    onClick = { selectedViewTab = RoiViewTab.WATCHLIST_OPTIMIZER },
                    label = { 
                        Text(
                            "🎯 Watchlist Match (${activeWatchlist.size})",
                            fontWeight = if (selectedViewTab == RoiViewTab.WATCHLIST_OPTIMIZER) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedViewTab == RoiViewTab.SPEND_ANALYTICS,
                    onClick = { selectedViewTab = RoiViewTab.SPEND_ANALYTICS },
                    label = { 
                        Text(
                            "📊 Spend & Usage",
                            fontWeight = if (selectedViewTab == RoiViewTab.SPEND_ANALYTICS) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ==========================================
        // TAB A: WATCHLIST COVERAGE & CHURN OPTIMIZER
        // ==========================================
        if (selectedViewTab == RoiViewTab.WATCHLIST_OPTIMIZER) {
            // Recommendation Banners
            if (topInactiveOpportunity != null && topInactiveOpportunity.count >= 2) {
                item {
                    val price = topInactiveOpportunity.provider.userCostPerMonth ?: topInactiveOpportunity.provider.costPerMonth
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("★", color = MaterialTheme.colorScheme.onTertiary, fontSize = 14.sp)
                                    }
                                }
                                Text(
                                    "Best Opportunity: ${topInactiveOpportunity.provider.name}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Text(
                                "${topInactiveOpportunity.count} movies on your watchlist are available on ${topInactiveOpportunity.provider.name} right now! Subscribe for 1 month at $${String.format("%.2f", price)} ($${String.format("%.2f", topInactiveOpportunity.costPerTitle ?: 0.0)}/movie) to binge them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { onToggleProvider(topInactiveOpportunity.provider.id, true) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Text("Activate ${topInactiveOpportunity.provider.name}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (topSafeToPause != null) {
                item {
                    val price = topSafeToPause.provider.userCostPerMonth ?: topSafeToPause.provider.costPerMonth
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("↓", color = MaterialTheme.colorScheme.onError, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    "Safe to Pause: ${topSafeToPause.provider.name}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                "Only ${topSafeToPause.count} movie on your watchlist is on ${topSafeToPause.provider.name}. Pause this subscription to save $${String.format("%.2f", price)}/mo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { onToggleProvider(topSafeToPause.provider.id, false) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Pause ${topSafeToPause.provider.name}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Filter Chips Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Providers by Watchlist Match",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onAddServiceClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Service", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = filterMode == "ALL",
                        onClick = { filterMode = "ALL" },
                        label = { Text("All (${providerCoverages.size})") }
                    )
                    FilterChip(
                        selected = filterMode == "ACTIVE",
                        onClick = { filterMode = "ACTIVE" },
                        label = { Text("Active (${providerCoverages.count { it.provider.isActive }})") }
                    )
                    FilterChip(
                        selected = filterMode == "INACTIVE",
                        onClick = { filterMode = "INACTIVE" },
                        label = { Text("Inactive (${providerCoverages.count { !it.provider.isActive }})") }
                    )
                }
            }

            // List of Providers Ranked by Watchlist Match
            if (filteredCoverages.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Text(
                            "No services match this filter.",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                items(filteredCoverages, key = { it.provider.id }) { coverage ->
                    val provider = coverage.provider
                    val effectiveCost = provider.userCostPerMonth ?: provider.costPerMonth
                    val isTrial = provider.trialEndDate != null && provider.trialEndDate > System.currentTimeMillis()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditProvider(provider) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (provider.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        border = if (provider.isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            provider.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (provider.isActive) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (provider.isActive) MaterialTheme.colorScheme.onPrimary
                                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                        ) {
                                            Text(
                                                text = if (isTrial) "TRIAL" else if (provider.isActive) "ACTIVE" else "PAUSED",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (effectiveCost == 0.0) "Free with ads"
                                               else "$${String.format("%.2f", effectiveCost)}/mo" +
                                                    (coverage.costPerTitle?.let { " · $${String.format("%.2f", it)} / movie" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = { onEditProvider(provider) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    if (provider.isActive) {
                                        Switch(
                                            checked = true,
                                            onCheckedChange = { onToggleProvider(provider.id, false) }
                                        )
                                    } else {
                                        Button(
                                            onClick = { onToggleProvider(provider.id, true) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("Activate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Watchlist Count Banner
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (coverage.count > 0) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (coverage.count > 0) "${coverage.count} Watchlist Movies Available"
                                               else "No Watchlist Movies Currently Available",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            // Horizontal preview of matching watchlist titles
                            if (coverage.matchingItems.isNotEmpty()) {
                                Text(
                                    text = "Available on your Watchlist:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(coverage.matchingItems.take(8)) { movie ->
                                        AssistChip(
                                            onClick = { onMovieClick(movie) },
                                            label = {
                                                Text(
                                                    text = movie.title,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )
                                    }
                                    if (coverage.matchingItems.size > 8) {
                                        item {
                                            AssistChip(
                                                onClick = { /* No-op */ },
                                                label = { Text("+${coverage.matchingItems.size - 8} more") }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // TAB B: SPEND & COST-PER-HOUR ANALYTICS
        // ==========================================
        if (selectedViewTab == RoiViewTab.SPEND_ANALYTICS) {
            // Summary Budget Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .showcaseTarget(
                            "roi_summary_card",
                            "Burn Rate & Potential Savings",
                            "This card aggregates the total custom pricing for all your active services. The 'Potential Savings' metric totals up any service where you've watched less than 3 hours this month.",
                            "Use this dashboard to confidently cancel services before they bill you again."
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
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
                        Text(
                            "${activeSubscribed.size} active subscriptions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        
                        if (potentialSavings > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ) {
                                Text(
                                    "Potential Savings: $${String.format("%.2f", potentialSavings)}/mo on underutilized channels",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Quick Services Ribbon & Add Service Button
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Quick Subscriptions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = onAddServiceClick,
                            modifier = Modifier.testTag("add_service_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Service", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Horizontal quick toggle chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allProviders, key = { it.id }) { provider ->
                            val effectivePrice = provider.userCostPerMonth ?: provider.costPerMonth
                            FilterChip(
                                selected = provider.isActive,
                                onClick = { onToggleProvider(provider.id, !provider.isActive) },
                                label = {
                                    Text(
                                        text = if (provider.isActive) "${provider.name} ($${String.format("%.2f", effectivePrice)})"
                                               else "+ ${provider.name}"
                                    )
                                },
                                leadingIcon = if (provider.isActive) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // Active Subscriptions Section Header
            item {
                Text(
                    "Active Subscriptions & Cost/Hour",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (activeSubscribed.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "No active subscriptions selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Tap any service chip above or click 'Add Service' to start tracking your streaming burn rate and cost per hour.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(activeSubscribed, key = { it.id }) { provider ->
                    val stats = monthlyStats.find { it.providerId == provider.id }
                    val totalHours = stats?.totalHours ?: 0.0
                    val costPerHour = stats?.costPerHour ?: (provider.userCostPerMonth ?: provider.costPerMonth)
                    val isPrimeCancelCandidate = totalHours < 3.0
                    val effectiveCost = provider.userCostPerMonth ?: provider.costPerMonth
                    val isTrial = provider.trialEndDate != null && provider.trialEndDate > System.currentTimeMillis()
                    val daysLeft = provider.trialEndDate?.let { ((it - System.currentTimeMillis()) / 86400000L).coerceAtLeast(0) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditProvider(provider) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPrimeCancelCandidate) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            }
                        ),
                        border = if (isPrimeCancelCandidate) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                        } else null,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Title row with Status & Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            provider.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isTrial) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                            ) {
                                                Text(
                                                    "TRIAL: ${daysLeft}d left",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (isPrimeCancelCandidate) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            ) {
                                                Text(
                                                    "CANCEL CANDIDATE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { onEditProvider(provider) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Edit Price/Trial",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Switch(
                                        checked = provider.isActive,
                                        onCheckedChange = { onToggleProvider(provider.id, it) },
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // Usage and Financial Metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text(
                                            "Watched",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            "${String.format("%.1f", totalHours)}h this mo",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Monthly Cost",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            "$${String.format("%.2f", effectiveCost)}/mo",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "$${String.format("%.2f", costPerHour)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (isPrimeCancelCandidate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "cost per hour",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Inactive / Paused Services Section
            if (inactiveProviders.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showInactiveSection = !showInactiveSection }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Paused / Inactive Services (${inactiveProviders.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            if (showInactiveSection) "Hide" else "Show",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (showInactiveSection) {
                    items(inactiveProviders, key = { it.id }) { provider ->
                        val effectiveCost = provider.userCostPerMonth ?: provider.costPerMonth
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditProvider(provider) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        provider.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Paused · $${String.format("%.2f", effectiveCost)}/mo when active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { onEditProvider(provider) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Edit",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Button(
                                        onClick = { onToggleProvider(provider.id, true) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Reactivate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_input_title")
                        .showcaseTarget(
                            "add_search_bar",
                            "Search TMDB",
                            "Type in any movie or show title. Streamwise will fetch its poster, description, and figure out where you can stream it using the Watchmode API.",
                            "You can paste a list of titles (one per line) to bulk-add them!"
                        ),
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
    onProviderUpdate: (String, Boolean, Double?, Long?, Long?) -> Unit,
    tmdbApiKey: String,
    onSaveTmdbApiKey: (String) -> Unit,
    watchmodeApiKey: String,
    onSaveWatchmodeApiKey: (String) -> Unit,
    ollamaHost: String,
    onSaveOllamaHost: (String) -> Unit,
    githubToken: String,
    onSaveGithubToken: (String) -> Unit,
    googleSheetWebhookUrl: String,
    onSaveGoogleSheetWebhookUrl: (String) -> Unit,
    fireTvIp: String,
    onSaveFireTvIp: (String) -> Unit,
    onSyncGoogleSheet: () -> Unit,
    onExportLetterboxd: () -> Unit,
    onSimulateResume: () -> Unit,
    onDismiss: () -> Unit
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0: Subs, 1: Cloud/Sheet, 2: Fire TV, 3: APIs, 4: AI/Local, 5: Dev

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
                    .heightIn(max = 520.dp)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = activeSubTab,
                    containerColor = Color.Transparent,
                    edgePadding = 0.dp,
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
                        text = { Text("Cloud/Sheet", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = activeSubTab == 2,
                        onClick = { activeSubTab = 2 },
                        text = { Text("Fire TV", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = activeSubTab == 3,
                        onClick = { activeSubTab = 3 },
                        text = { Text("APIs", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = activeSubTab == 4,
                        onClick = { activeSubTab = 4 },
                        text = { Text("AI/Local", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = activeSubTab == 5,
                        onClick = { activeSubTab = 5 },
                        text = { Text("Dev", fontSize = 11.sp) }
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
                                "Toggle active services and configure custom trial pricing to improve ROI calculations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            allProviders.forEach { provider ->
                                ProviderSettingsCard(
                                    provider = provider,
                                    onUpdate = { active, cost, start, end -> 
                                        onProviderUpdate(provider.id, active, cost, start, end)
                                    }
                                )
                            }
                        }
                    }
                    1 -> {
                        var sheetUrlInput by remember(googleSheetWebhookUrl) { mutableStateOf(googleSheetWebhookUrl) }
                        val scrollState = rememberScrollState()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            Text("Google Sheets & Letterboxd", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Two-way sync with your Google Sheet Master Ledger (\$0/mo). Enables Gemini scheduled tasks to ingest podcast recommendations directly into your Watchlist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            OutlinedTextField(
                                value = sheetUrlInput,
                                onValueChange = { sheetUrlInput = it },
                                label = { Text("Google Apps Script Webhook URL") },
                                placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = { onSaveGoogleSheetWebhookUrl(sheetUrlInput) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Webhook URL")
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Button(
                                onClick = onSyncGoogleSheet,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync Watchlist & Watched Now")
                            }

                            Button(
                                onClick = onExportLetterboxd,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Letterboxd CSV to Downloads")
                            }
                        }
                    }
                    2 -> {
                        var fireIpInput by remember(fireTvIp) { mutableStateOf(fireTvIp) }
                        val scrollState = rememberScrollState()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            Text("Amazon Fire TV Wi-Fi Trigger", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Configure your Fire TV IP address. When you tap 'Watch' on any movie, Streamwise wakes your Fire TV over Wi-Fi and launches the film natively inside Netflix, Prime, Hulu, or Max in 4K HDR.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            OutlinedTextField(
                                value = fireIpInput,
                                onValueChange = { fireIpInput = it },
                                label = { Text("Fire TV IP Address") },
                                placeholder = { Text("e.g. 192.168.1.150") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = { onSaveFireTvIp(fireIpInput) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Fire TV IP")
                            }
                        }
                    }
                    3 -> {
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
                    4 -> {
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
                    5 -> {
                        val scrollState = rememberScrollState()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                "Developer & Simulation",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Test lifecycle background transitions, check-in sheets, and diagnostic reporting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            OutlinedButton(
                                onClick = onSimulateResume,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simulate App Resume (Foreground Check)")
                            }
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
                    
                    val yearRuntimeText = buildString {
                        if (!item.releaseYear.isNullOrBlank()) append(item.releaseYear)
                        if (item.runtimeMinutes != null && item.runtimeMinutes > 0) {
                            if (isNotEmpty()) append(" · ")
                            append("${item.runtimeMinutes} min")
                        }
                    }
                    if (yearRuntimeText.isNotBlank()) {
                        Text(
                            text = yearRuntimeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

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
                modifier = Modifier
                    .weight(1f)
                    .showcaseTarget(
                        "agent_input",
                        "AI Assistant Prompt",
                        "Ask the AI for personalized recommendations based on your mood. It's completely private.",
                        "Use natural language like 'I want a sci-fi movie from the 80s'."
                    ),
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

@Composable
fun ProviderSettingsCard(
    provider: StreamingProvider,
    onUpdate: (Boolean, Double?, Long?, Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isTrialWarning = remember(provider.trialEndDate) {
        if (provider.trialEndDate != null) {
            val daysLeft = (provider.trialEndDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
            daysLeft in 0..3
        } else false
    }
    val isExpired = remember(provider.trialEndDate) {
        if (provider.trialEndDate != null) {
            System.currentTimeMillis() > provider.trialEndDate
        } else false
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) MaterialTheme.colorScheme.errorContainer.copy(alpha=0.2f) 
                             else if (isTrialWarning) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha=0.4f)
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val costLabel = if (provider.costPerMonth == 0.0) "Free Platform ($0.0)"
                        else if (provider.userCostPerMonth != null) "Custom: $${provider.userCostPerMonth}/mo"
                        else "$${provider.costPerMonth}/mo"
                    Text(
                        text = costLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (isExpired) {
                        Text(
                            text = "TRIAL EXPIRED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isTrialWarning) {
                        Text(
                            text = "TRIAL ENDING SOON",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Switch(
                    checked = provider.isActive,
                    onCheckedChange = { onUpdate(it, provider.userCostPerMonth, provider.subscriptionStartDate, provider.trialEndDate) },
                    modifier = Modifier.testTag("dialog_switch_${provider.id}")
                )
            }
            
            if (expanded && provider.costPerMonth > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                
                var customCostInput by remember { mutableStateOf(provider.userCostPerMonth?.toString() ?: "") }
                OutlinedTextField(
                    value = customCostInput,
                    onValueChange = { customCostInput = it },
                    label = { Text("Custom Monthly Price (e.g. 0.99 for trial)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .showcaseTarget(
                            "custom_price_input",
                            "Custom Subscription Pricing",
                            "Enter exactly what you are paying right now for this service. If you're on a promo rate, type it in here.",
                            "Use this to keep your ROI calculations exact. When the promo ends, change it back!"
                        )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // For simplicity we use a text input for days until trial ends to compute the timestamp
                var trialDaysInput by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = trialDaysInput,
                    onValueChange = { trialDaysInput = it },
                    label = { Text("Days until trial ends (leave blank if none)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .showcaseTarget(
                            "trial_days_input",
                            "Trial Expiration Tracker",
                            "Set a countdown for free trials. Streamwise will show a warning when it's almost up, and automatically disable the service in your app when it expires.",
                            "Use this whenever you start a free week or month on a service so you don't forget to cancel!"
                        )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { expanded = false }) { Text("Cancel") }
                    Button(onClick = {
                        val parsedCost = customCostInput.toDoubleOrNull()
                        val parsedDays = trialDaysInput.toLongOrNull()
                        val newEndDate = if (parsedDays != null) System.currentTimeMillis() + (parsedDays * 24 * 60 * 60 * 1000) else provider.trialEndDate
                        
                        onUpdate(provider.isActive, parsedCost, provider.subscriptionStartDate, newEndDate)
                        expanded = false
                    }) {
                        Text("Save Details")
                    }
                }
            }
        }
    }
}

