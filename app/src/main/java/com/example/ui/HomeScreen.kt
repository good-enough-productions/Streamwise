package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import java.util.Locale
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
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.webkit.WebViewClient
import com.example.data.local.ProviderUsageStats
import com.example.data.model.MediaItem
import com.example.data.model.MediaStatus
import com.example.data.model.PodcastEpisodeCatalog
import com.example.data.model.StreamingProvider
import com.example.data.remote.LetterboxdSyncResult
import com.example.data.remote.LetterboxdFileImportResult
import com.example.data.remote.UpdateStatus
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: StreamViewModel,
    modifier: Modifier = Modifier,
    simulateForegroundReturn: () -> Unit = {}
) {
    val allItems by viewModel.allMediaItems.collectAsState()
    val watchlistItems = remember(allItems) { allItems.filter { it.status != MediaStatus.WATCHED.name } }
    val watchedItems = remember(allItems) { allItems.filter { it.status == MediaStatus.WATCHED.name } }
    val allProviders by viewModel.allProviders.collectAsState()
    val monthlyStats by viewModel.monthlyROIStats.collectAsState()
    val checkInItem by viewModel.activeCheckInItem.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val tmdbApiKey by viewModel.tmdbApiKey.collectAsState()
    val letterboxdUsername by viewModel.letterboxdUsername.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val isSyncingLetterboxd by viewModel.isSyncingLetterboxd.collectAsState()
    val letterboxdSyncResult by viewModel.letterboxdSyncResult.collectAsState()
    val letterboxdFileImportResult by viewModel.letterboxdFileImportResult.collectAsState()
    val isSyncingPodcasts by viewModel.isSyncingPodcasts.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Watchlist, 1: Watched, 2: ROI Stats, 3: Explore
    var exploreSubTab by remember { mutableStateOf(0) } // 0: AI & Taste, 1: Olivia AI, 2: Podcasts, 3: Film News
    var filterOnlyMyServices by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var detailMovieItem by remember { mutableStateOf<MediaItem?>(null) }
    var showServiceDetailProvider by remember { mutableStateOf<StreamingProvider?>(null) }
    var showLetterboxdSyncDialog by remember { mutableStateOf(false) }
    var showLetterboxdImportDialog by remember { mutableStateOf(false) }
    var showUserGuideDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }

    // Android Document Picker launcher for Letterboxd CSV/ZIP files
    val letterboxdFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importLetterboxdFile(uri, context.contentResolver)
            showLetterboxdImportDialog = true
        }
    }

    // Export Watched Vault to standard Letterboxd import CSV
    val onExportForLetterboxd: () -> Unit = {
        coroutineScope.launch {
            val csvData = viewModel.exportLetterboxdCsv()
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Letterboxd Import from Streamwise")
                putExtra(Intent.EXTRA_TEXT, csvData)
            }
            val chooser = Intent.createChooser(sendIntent, "Export Watched Movies for Letterboxd")
            context.startActivity(chooser)
        }
    }

    val isDark by viewModel.isDarkMode.collectAsState()
    val enableBetaFeedback by viewModel.enableBetaFeedback.collectAsState()
    val isSpotlightCollapsed by viewModel.isSpotlightCollapsed.collectAsState()
    val isSyncingToSheet by viewModel.isSyncingToSheet.collectAsState()
    val googleSheetWebhookUrl by viewModel.googleSheetWebhookUrl.collectAsState()

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
                    Column {
                        Text(
                            "Streamwise",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            when (selectedTab) {
                                0 -> if (watchlistItems.isNotEmpty()) "Watchlist • ${watchlistItems.size} Titles" else "Watchlist"
                                1 -> if (watchedItems.isNotEmpty()) "Watched Vault • ${watchedItems.size} Movies" else "Watched History"
                                2 -> "My Services • ${allProviders.count { it.isActive }} Active"
                                3 -> "Explore & Cinema AI"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDark) "Switch to Light Theme" else "Switch to Dark Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        BadgedBox(badge = {
                            if (watchlistItems.isNotEmpty()) {
                                Badge(
                                    modifier = Modifier.offset(x = 6.dp, y = (-2).dp),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    val countText = if (watchlistItems.size > 999) "${watchlistItems.size / 1000}k+" else "${watchlistItems.size}"
                                    Text(countText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Watchlist")
                        }
                    },
                    label = { Text("Watchlist") },
                    modifier = Modifier.testTag("tab_watchlist")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(badge = {
                            if (watchedItems.isNotEmpty()) {
                                Badge(
                                    modifier = Modifier.offset(x = 8.dp, y = (-2).dp),
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ) {
                                    val countText = if (watchedItems.size >= 1000) {
                                        String.format(Locale.US, "%.1fk", watchedItems.size / 1000.0)
                                    } else {
                                        "${watchedItems.size}"
                                    }
                                    Text(countText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Watched")
                        }
                    },
                    label = { Text("Watched") },
                    modifier = Modifier.testTag("tab_watched")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Subscriptions, contentDescription = "My Services") },
                    label = { Text("My Services") },
                    modifier = Modifier.testTag("tab_services")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Explore") },
                    label = { Text("Explore") },
                    modifier = Modifier.testTag("tab_agent")
                )
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.navigationBarsPadding()
            ) {
                // Global Beta Feedback FAB
                if (enableBetaFeedback) {
                    FloatingFeedbackButton(
                        onClick = { showFeedbackDialog = true }
                    )
                }

                if (selectedTab == 0 || selectedTab == 1) {
                    ExtendedFloatingActionButton(
                        text = { Text("Add Title") },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add media item") },
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .testTag("add_item_fab"),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                        onMovieClick = { detailMovieItem = it },
                        isSpotlightCollapsed = isSpotlightCollapsed,
                        onToggleSpotlightCollapsed = { viewModel.toggleSpotlightCollapsed() }
                    )
                    1 -> {
                        val watchedItems by viewModel.watchedItems.collectAsState()
                        // Automatically sync Letterboxd when viewing the Watched Vault
                        LaunchedEffect(Unit) {
                            viewModel.syncLetterboxdLive(silent = true)
                        }
                        WatchedTabContent(
                            watchedItems = watchedItems,
                            allProviders = allProviders,
                            onMovieClick = { detailMovieItem = it },
                            onDeleteClick = { viewModel.deleteItem(it) },
                            onSyncClick = {
                                viewModel.syncLetterboxdLive(silent = false)
                                viewModel.triggerImmediateSync()
                            },
                            isSyncingToSheet = isSyncingToSheet,
                            onSyncLetterboxdToSheet = { viewModel.syncLetterboxdToGoogleSheet() },
                            onSyncLetterboxdLive = {
                                viewModel.syncLetterboxdLive(silent = false)
                                showLetterboxdSyncDialog = true
                            },
                            onRewatchIntent = { viewModel.startIntendingToWatch(it) },
                            onPickLetterboxdFile = { letterboxdFileLauncher.launch(arrayOf("*/*", "text/*", "text/csv", "application/zip")) },
                            onExportLetterboxdCsv = onExportForLetterboxd
                        )
                    }
                    2 -> MonthlyRoiContent(
                        monthlyStats = monthlyStats,
                        allProviders = allProviders,
                        watchlistItems = watchlistItems,
                        onProviderClick = { showServiceDetailProvider = it }
                    )
                    3 -> {
                        val chatMessages by viewModel.chatMessages.collectAsState()
                        val isChatLoading by viewModel.isChatLoading.collectAsState()
                        val geminiAnalysis by viewModel.geminiAnalysis.collectAsState()
                        val isGeminiAnalyzing by viewModel.isAnalyzingWithGemini.collectAsState()
                        val watchedItems by viewModel.watchedItems.collectAsState()
                        ExploreTabContent(
                            selectedSubTab = exploreSubTab,
                            onSubTabChange = { exploreSubTab = it },
                            chatMessages = chatMessages,
                            isLoading = isChatLoading,
                            onSendMessage = { viewModel.sendChatMessage(it) },
                            geminiAnalysis = geminiAnalysis,
                            isGeminiAnalyzing = isGeminiAnalyzing,
                            onRefreshGeminiAnalysis = { viewModel.runGeminiProAnalysis() },
                            podcastEpisodes = viewModel.podcastEpisodes,
                            movieNews = viewModel.movieNews,
                            watchedItems = watchedItems,
                            isSyncingPodcasts = isSyncingPodcasts,
                            onSyncPodcastRecs = { viewModel.syncPodcastRecommendations() },
                            onAddRecommendation = { title, reason -> viewModel.addRecommendationToWatchlist(title, reason) }
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
            val geminiApiKey by viewModel.geminiApiKey.collectAsState()
            SettingsDialog(
                allProviders = allProviders,
                onProviderToggle = { id, active -> viewModel.toggleStreamingProvider(id, active) },
                letterboxdUsername = letterboxdUsername,
                onSaveLetterboxdUsername = { viewModel.saveLetterboxdUsername(it) },
                userName = userName,
                onSaveUserName = { viewModel.saveUserName(it) },
                onSyncLetterboxdLive = {
                    viewModel.syncLetterboxdLive()
                    showLetterboxdSyncDialog = true
                },
                onOpenUserGuide = { showUserGuideDialog = true },
                onOpenChangelog = { showChangelogDialog = true },
                updateStatus = updateStatus,
                onCheckForUpdates = { viewModel.checkForUpdates() },
                onDownloadAndInstallUpdate = { viewModel.downloadAndInstallUpdate(it) },
                onResetUpdateStatus = { viewModel.resetUpdateStatus() },
                tmdbApiKey = tmdbApiKey,
                onSaveTmdbApiKey = { viewModel.saveTmdbApiKey(it) },
                geminiApiKey = geminiApiKey,
                onSaveGeminiApiKey = { viewModel.saveGeminiApiKey(it) },
                watchmodeApiKey = watchmodeApiKey,
                onSaveWatchmodeApiKey = { viewModel.saveWatchmodeApiKey(it) },
                ollamaHost = ollamaHost,
                onSaveOllamaHost = { viewModel.saveOllamaHost(it) },
                githubToken = githubToken,
                onSaveGithubToken = { viewModel.saveGithubToken(it) },
                googleSheetWebhookUrl = googleSheetWebhookUrl,
                onSaveGoogleSheetWebhookUrl = { viewModel.saveGoogleSheetWebhookUrl(it) },
                isSyncingToSheet = isSyncingToSheet,
                onSyncLetterboxdToSheet = { viewModel.syncLetterboxdToGoogleSheet() },
                isSyncingPodcasts = isSyncingPodcasts,
                onSyncPodcastRecs = { viewModel.syncPodcastRecommendations() },
                enableBetaFeedback = enableBetaFeedback,
                onToggleBetaFeedback = { viewModel.setEnableBetaFeedback(it) },
                onPickLetterboxdFile = { letterboxdFileLauncher.launch(arrayOf("*/*", "text/*", "text/csv", "application/zip")) },
                onExportLetterboxdCsv = onExportForLetterboxd,
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Service Detail Bottom Sheet (View/Edit service, tenure, deal finder)
        if (showServiceDetailProvider != null) {
            val providerStats = monthlyStats.find { it.providerId == showServiceDetailProvider!!.id }
            ServiceDetailBottomSheet(
                provider = showServiceDetailProvider!!,
                stats = providerStats,
                watchlistItems = watchlistItems,
                onSelectMovie = { movie ->
                    showServiceDetailProvider = null
                    detailMovieItem = movie
                },
                onUpdateProvider = { updated ->
                    viewModel.updateStreamingProvider(updated)
                    showServiceDetailProvider = null
                },
                onDismiss = { showServiceDetailProvider = null }
            )
        }

        // Live Letterboxd RSS Sync Dialog
        if (showLetterboxdSyncDialog || isSyncingLetterboxd) {
            LetterboxdSyncDialog(
                isSyncing = isSyncingLetterboxd,
                result = letterboxdSyncResult,
                onDismiss = {
                    showLetterboxdSyncDialog = false
                    viewModel.clearLetterboxdSyncResult()
                }
            )
        }

        // Letterboxd CSV/ZIP File Import Result Dialog
        if (showLetterboxdImportDialog && letterboxdFileImportResult != null) {
            val res = letterboxdFileImportResult!!
            AlertDialog(
                onDismissRequest = {
                    showLetterboxdImportDialog = false
                    viewModel.clearLetterboxdFileImportResult()
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (res.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (res.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(if (res.isSuccess) "Letterboxd Import Summary" else "Import Failed")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Source: ${res.sourceName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        if (res.isSuccess) {
                            Text("✓ Watchlist added: ${res.watchlistImported} titles", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("✓ Watched Vault added: ${res.watchedImported} titles", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("• Existing / Duplicates skipped: ${res.alreadyPresentCount}", style = MaterialTheme.typography.bodySmall)
                            if (res.sampleTitles.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Sample Titles:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                res.sampleTitles.take(5).forEach { t ->
                                    Text("• $t", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            Text(res.errorMessage ?: "Unknown error while reading Letterboxd file.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showLetterboxdImportDialog = false
                        viewModel.clearLetterboxdFileImportResult()
                    }) {
                        Text("Done")
                    }
                }
            )
        }

        // In-App User Guide Dialog
        if (showUserGuideDialog) {
            HtmlAssetViewerDialog(
                title = "Streamwise User Guide",
                assetFileName = "user_guide.html",
                onDismiss = { showUserGuideDialog = false }
            )
        }

        // In-App Changelog Dialog
        if (showChangelogDialog) {
            HtmlAssetViewerDialog(
                title = "Streamwise Changelog",
                assetFileName = "changelog.html",
                onDismiss = { showChangelogDialog = false }
            )
        }

        // Beta Feedback Dialog (Backlog-first, Jules opt-in)
        if (showFeedbackDialog) {
            val githubToken by viewModel.githubToken.collectAsState()
            val currentTabName = when (selectedTab) {
                0 -> "Watchlist"
                1 -> "Watched Vault"
                2 -> "My Services"
                3 -> "Explore & Cinema AI"
                else -> "Main"
            }
            FeedbackDialog(
                githubToken = githubToken,
                currentTabName = currentTabName,
                watchlistCount = watchlistItems.count { it.status != MediaStatus.WATCHED.name },
                watchedCount = watchlistItems.count { it.status == MediaStatus.WATCHED.name },
                onDismiss = { showFeedbackDialog = false },
                onSubmitSuccess = { msg ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
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
                    viewModel.startIntendingToWatch(detailMovieItem!!)
                    detailMovieItem = null
                },
                onDeleteClick = {
                    viewModel.deleteItem(detailMovieItem!!)
                    detailMovieItem = null
                },
                onDiscussInExplore = { prompt ->
                    detailMovieItem = null
                    exploreSubTab = 1
                    selectedTab = 3
                    viewModel.sendChatMessage(prompt)
                }
            )
        }
    }
}

// ==========================================
// COMPOSABLE: Spotlight Discovery Card
// ==========================================
@Composable
fun SpotlightCard(
    item: MediaItem,
    allProviders: List<StreamingProvider>,
    onWatchClick: () -> Unit,
    onMovieClick: () -> Unit
) {
    val activeProvider = remember(item, allProviders) {
        item.providersList.mapNotNull { pId -> allProviders.find { it.id == pId } }
            .firstOrNull { it.isActive || it.costPerMonth == 0.0 }
    }

    Card(
        modifier = Modifier
            .width(135.dp)
            .clickable { onMovieClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Inset-bordered artwork with concentric radius: 16 - 8 = 8.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            ) {
                if (!item.imageUrl.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = item.imageUrl,
                        contentDescription = "Spotlight poster",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                // Top floating rating badge with tabular numerals
                if (item.rating != null && item.rating > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", item.rating),
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Streaming provider tag
            if (activeProvider != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Text(
                        text = activeProvider.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick watch intent button with optically-centered play arrow
            Button(
                onClick = onWatchClick,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier
                            .size(14.dp)
                            .offset(x = 1.dp) // Optical centroid alignment
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Watch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// COMPOSABLE: Watchlist Screen
// ==========================================
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
    isSpotlightCollapsed: Boolean = false,
    onToggleSpotlightCollapsed: () -> Unit = {}
) {
    val activeProviderIds = remember(allProviders) {
        allProviders.filter { it.isActive }.map { it.id }.toSet()
    }
    val freeProviderIds = remember(allProviders) {
        allProviders.filter { it.costPerMonth == 0.0 }.map { it.id }.toSet()
    }

    // Multi-select filter states to widen search (OR logic)
    var selectedPlatforms by remember { mutableStateOf(emptySet<String>()) }
    var selectedGenres by remember { mutableStateOf(emptySet<String>()) }
    var selectedEras by remember { mutableStateOf(emptySet<String>()) }
    var minRating by remember { mutableStateOf(0.0) }
    var selectedPodcastId by remember { mutableStateOf<String?>(null) }
    var podcastMainSubjectOnly by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("added") } // "added", "alpha", "rating"
    var showFreeOnly by remember { mutableStateOf(false) }
    var showTopRatedOnly by remember { mutableStateOf(false) }
    var showAdvancedFiltersSheet by remember { mutableStateOf(false) }

    val allGenres = remember(watchlistItems) {
        watchlistItems.flatMap { it.genres?.split(",")?.map { g -> g.trim() } ?: emptyList() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    // Curated Spotlight items available on active or free platforms
    val spotlightItems = remember(watchlistItems, activeProviderIds, freeProviderIds) {
        watchlistItems.filter { item ->
            item.status != MediaStatus.WATCHED.name &&
            item.providersList.any { activeProviderIds.contains(it) || freeProviderIds.contains(it) }
        }.distinctBy { it.title.trim().lowercase() }.sortedByDescending { it.rating ?: 0.0 }.take(8)
    }

    val activeAdvancedCount = selectedPlatforms.size + selectedGenres.size + selectedEras.size + (if (minRating > 0.0) 1 else 0) + (if (selectedPodcastId != null) 1 else 0)

    // Filter items according to state with OR widening logic for multi-selected chips
    val filteredItems = remember(
        watchlistItems,
        filterOnlyMyServices,
        activeProviderIds,
        freeProviderIds,
        selectedPlatforms,
        showFreeOnly,
        showTopRatedOnly,
        selectedGenres,
        minRating,
        selectedEras,
        selectedPodcastId,
        podcastMainSubjectOnly
    ) {
        watchlistItems.filter { item ->
            // Exclude already watched from immediate watchlist
            if (item.status == MediaStatus.WATCHED.name) return@filter false

            // Multi-select Platforms (OR logic: widens search to any selected platform)
            if (selectedPlatforms.isNotEmpty()) {
                val hasPlatform = item.providersList.any { selectedPlatforms.contains(it) }
                if (!hasPlatform) return@filter false
            }

            // Multi-select Genres (OR logic: widens search to any selected genre)
            if (selectedGenres.isNotEmpty()) {
                val itemGenres = item.genres?.split(",")?.map { it.trim().lowercase() } ?: emptyList()
                val hasGenre = selectedGenres.any { sg -> itemGenres.contains(sg.lowercase()) }
                if (!hasGenre) return@filter false
            }

            if (showFreeOnly) {
                val provs = item.providersList
                if (provs.none { freeProviderIds.contains(it) }) return@filter false
            }

            if (showTopRatedOnly || minRating > 0.0) {
                val threshold = if (showTopRatedOnly && minRating < 7.5) 7.5 else minRating
                if ((item.rating ?: 0.0) < threshold) return@filter false
            }

            // Multi-select Release Eras (OR logic: widens search to any selected era)
            if (selectedEras.isNotEmpty()) {
                val yearMatch = Regex("""\b(19\d\d|20\d\d)\b""").find(item.overview ?: "")?.value?.toIntOrNull()
                    ?: Regex("""\b(19\d\d|20\d\d)\b""").find(item.title)?.value?.toIntOrNull()
                if (yearMatch != null) {
                    val matchesAnyEra = selectedEras.any { era ->
                        when (era) {
                            "2020s" -> yearMatch >= 2020
                            "2010s" -> yearMatch in 2010..2019
                            "2000s" -> yearMatch in 2000..2009
                            "90s" -> yearMatch in 1990..1999
                            "Classic" -> yearMatch < 1990
                            else -> true
                        }
                    }
                    if (!matchesAnyEra) return@filter false
                }
            }

            // Podcast Coverage Filter
            if (selectedPodcastId != null) {
                val matchesPodcast = com.example.data.model.PodcastEpisodeCatalog.isCoveredOnPodcast(
                    movieTitle = item.title,
                    podcastId = selectedPodcastId,
                    mainSubjectOnly = podcastMainSubjectOnly,
                    importSource = item.importSource,
                    notes = item.userNotes
                )
                if (!matchesPodcast) return@filter false
            }

            if (filterOnlyMyServices) {
                val provs = item.providersList
                if (item.tmdbId == null) {
                    true
                } else {
                    provs.any { activeProviderIds.contains(it) || freeProviderIds.contains(it) }
                }
            } else {
                true
            }
        }.distinctBy { it.title.trim().lowercase() }
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
        ) {
            // 1. Natural Scrolling Spotlight Lane (scrolls off when scrolling down, reappears when scrolling to top)
            if (searchQuery.isBlank() && spotlightItems.isNotEmpty()) {
                item(key = "spotlight_carousel_section") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Spotlight: Ready to Stream",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                        ) {
                                            Text(
                                                "${spotlightItems.size}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    spotlightItems.forEach { item ->
                                        SpotlightCard(
                                            item = item,
                                            allProviders = allProviders,
                                            onWatchClick = { onWatchClick(item) },
                                            onMovieClick = { onMovieClick(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Sticky Header: Search & Sort Bar + Filter Ribbon (Pins to top on scroll)
            stickyHeader(key = "sticky_search_filter_bar") {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 2.dp,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        // Search Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search watchlist...") },
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
                                    .height(50.dp),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Sort Selector Button
                            var sortExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1.1f)) {
                                OutlinedButton(
                                    onClick = { sortExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    val sortLabel = when (sortBy) {
                                        "alpha" -> "A-Z"
                                        "rating" -> "Rating"
                                        else -> "Recent"
                                    }
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
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

                        // Filter Ribbon with multi-selected dismiss chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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

                            // Active Multi-Select Platform Pills (1-tap dismiss)
                            selectedPlatforms.forEach { pId ->
                                val provName = allProviders.find { it.id == pId }?.name ?: pId
                                InputChip(
                                    selected = true,
                                    onClick = { selectedPlatforms = selectedPlatforms - pId },
                                    label = { Text(provName, fontSize = 11.sp) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp)) }
                                )
                            }

                            // Active Multi-Select Genre Pills
                            selectedGenres.forEach { genre ->
                                InputChip(
                                    selected = true,
                                    onClick = { selectedGenres = selectedGenres - genre },
                                    label = { Text(genre, fontSize = 11.sp) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp)) }
                                )
                            }

                            // Active Multi-Select Era Pills
                            selectedEras.forEach { era ->
                                val eraLabel = if (era == "Classic") "Pre-1990" else era
                                InputChip(
                                    selected = true,
                                    onClick = { selectedEras = selectedEras - era },
                                    label = { Text(eraLabel, fontSize = 11.sp) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp)) }
                                )
                            }

                            if (minRating > 0.0) {
                                InputChip(
                                    selected = true,
                                    onClick = { minRating = 0.0 },
                                    label = { Text("★ ${minRating}+", fontSize = 11.sp) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp)) }
                                )
                            }

                            if (selectedPodcastId != null) {
                                val pod = com.example.data.model.PodcastEpisodeCatalog.AVAILABLE_PODCASTS.find { it.id == selectedPodcastId }
                                val podLabel = "${pod?.emoji ?: "🎙️"} ${pod?.name ?: "Podcast"}${if (podcastMainSubjectOnly) " (Main)" else ""}"
                                InputChip(
                                    selected = true,
                                    onClick = { selectedPodcastId = null },
                                    label = { Text(podLabel, fontSize = 11.sp) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp)) }
                                )
                            }

                            // Advanced Filters Entry Button
                            ElevatedFilterChip(
                                selected = activeAdvancedCount > 0,
                                onClick = { showAdvancedFiltersSheet = true },
                                label = {
                                    Text(
                                        if (activeAdvancedCount > 0) "Filters ($activeAdvancedCount)" else "Filters",
                                        fontSize = 11.sp,
                                        fontWeight = if (activeAdvancedCount > 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Advanced Filters",
                                        modifier = Modifier.size(14.dp),
                                        tint = if (activeAdvancedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            )

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

                        // Watchlist title count clarity summary bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Showing ${processedItems.size} of ${watchlistItems.count { it.status != MediaStatus.WATCHED.name }} titles",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold
                            )
                            if (processedItems.size < watchlistItems.count { it.status != MediaStatus.WATCHED.name }) {
                                Text(
                                    text = "${watchlistItems.count { it.status != MediaStatus.WATCHED.name } - processedItems.size} hidden by filters",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 3. Media Items List (or Empty State)
            if (processedItems.isEmpty()) {
                item(key = "watchlist_empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "Empty list",
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No titles found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (filterOnlyMyServices) "Try switching to 'All' or widening your filters."
                                else "Use the Add button or share titles to populate your vault.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(processedItems, key = { it.id }) { item ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
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

        // Advanced Filter Sheet (Multi-Select Enabled to Widen Search)
        if (showAdvancedFiltersSheet) {
            AdvancedFilterBottomSheet(
                allProviders = allProviders,
                allGenres = allGenres,
                selectedPlatforms = selectedPlatforms,
                onTogglePlatform = { pId ->
                    selectedPlatforms = if (selectedPlatforms.contains(pId)) selectedPlatforms - pId else selectedPlatforms + pId
                },
                onClearPlatforms = { selectedPlatforms = emptySet() },
                selectedGenres = selectedGenres,
                onToggleGenre = { genre ->
                    selectedGenres = if (selectedGenres.contains(genre)) selectedGenres - genre else selectedGenres + genre
                },
                onClearGenres = { selectedGenres = emptySet() },
                minRating = minRating,
                onSelectMinRating = { minRating = it },
                selectedEras = selectedEras,
                onToggleEra = { era ->
                    selectedEras = if (selectedEras.contains(era)) selectedEras - era else selectedEras + era
                },
                onClearEras = { selectedEras = emptySet() },
                selectedPodcastId = selectedPodcastId,
                onSelectPodcast = { selectedPodcastId = it },
                podcastMainSubjectOnly = podcastMainSubjectOnly,
                onTogglePodcastMainSubjectOnly = { podcastMainSubjectOnly = it },
                sortBy = sortBy,
                onSelectSortBy = { sortBy = it },
                matchingCount = processedItems.size,
                onResetAll = {
                    selectedPlatforms = emptySet()
                    selectedGenres = emptySet()
                    selectedEras = emptySet()
                    minRating = 0.0
                    selectedPodcastId = null
                    podcastMainSubjectOnly = false
                    showFreeOnly = false
                    onFilterToggle(true)
                },
                onDismiss = { showAdvancedFiltersSheet = false }
            )
        }
    }
}

// ==========================================
// COMPOSABLE: Advanced Filter Bottom Sheet (Multi-Select Enabled)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterBottomSheet(
    allProviders: List<StreamingProvider>,
    allGenres: List<String>,
    selectedPlatforms: Set<String>,
    onTogglePlatform: (String) -> Unit,
    onClearPlatforms: () -> Unit,
    selectedGenres: Set<String>,
    onToggleGenre: (String) -> Unit,
    onClearGenres: () -> Unit,
    minRating: Double,
    onSelectMinRating: (Double) -> Unit,
    selectedEras: Set<String>,
    onToggleEra: (String) -> Unit,
    onClearEras: () -> Unit,
    selectedPodcastId: String?,
    onSelectPodcast: (String?) -> Unit,
    podcastMainSubjectOnly: Boolean,
    onTogglePodcastMainSubjectOnly: (Boolean) -> Unit,
    sortBy: String,
    onSelectSortBy: (String) -> Unit,
    matchingCount: Int,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Advanced Filters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onResetAll) {
                    Text("Reset All", color = MaterialTheme.colorScheme.error)
                }
            }

            Text(
                "Multi-select chips in any category to widen your search",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Section 1: Streaming Platforms (Multi-Select)
            Text(
                "Streaming Platform",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedPlatforms.isEmpty(),
                    onClick = onClearPlatforms,
                    label = { Text("Any Platform", fontSize = 11.sp) },
                    leadingIcon = { if (selectedPlatforms.isEmpty()) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                )
                allProviders.filter { it.isActive || it.costPerMonth == 0.0 }.forEach { provider ->
                    val isSelected = selectedPlatforms.contains(provider.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTogglePlatform(provider.id) },
                        label = { Text(provider.name, fontSize = 11.sp) },
                        leadingIcon = { if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Minimum Rating
            Text(
                "Minimum TMDB Rating",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                listOf(0.0 to "Any", 6.0 to "★ 6.0+", 7.0 to "★ 7.0+", 7.5 to "★ 7.5+", 8.0 to "★ 8.0+", 8.5 to "★ 8.5+").forEach { (rating, label) ->
                    val isSelected = minRating == rating
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectMinRating(if (minRating == rating) 0.0 else rating) },
                        label = { Text(label, fontSize = 11.sp) },
                        leadingIcon = { if (isSelected && rating > 0.0) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Release Era / Decade (Multi-Select)
            Text(
                "Release Era",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = selectedEras.isEmpty(),
                    onClick = onClearEras,
                    label = { Text("All Eras", fontSize = 11.sp) },
                    leadingIcon = { if (selectedEras.isEmpty()) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                )
                listOf("2020s" to "2020s", "2010s" to "2010s", "2000s" to "2000s", "90s" to "90s", "Classic" to "Pre-1990").forEach { (eraKey, eraLabel) ->
                    val isSelected = selectedEras.contains(eraKey)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleEra(eraKey) },
                        label = { Text(eraLabel, fontSize = 11.sp) },
                        leadingIcon = { if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 4: Genres (Multi-Select)
            if (allGenres.isNotEmpty()) {
                Text(
                    "Genre",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedGenres.isEmpty(),
                        onClick = onClearGenres,
                        label = { Text("All Genres", fontSize = 11.sp) },
                        leadingIcon = { if (selectedGenres.isEmpty()) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                    )
                    allGenres.forEach { genre ->
                        val isSelected = selectedGenres.contains(genre)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleGenre(genre) },
                            label = { Text(genre, fontSize = 11.sp) },
                            leadingIcon = { if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 5: Podcasts & Media Mentions
            Text(
                "Podcasts & Media Mentions",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Filter movies discussed on popular cinema podcasts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedPodcastId == null,
                    onClick = { onSelectPodcast(null) },
                    label = { Text("Any / All", fontSize = 11.sp) },
                    leadingIcon = { if (selectedPodcastId == null) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                )
                com.example.data.model.PodcastEpisodeCatalog.AVAILABLE_PODCASTS.forEach { pod ->
                    val isSelected = selectedPodcastId == pod.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectPodcast(if (isSelected) null else pod.id) },
                        label = { Text("${pod.emoji} ${pod.name}", fontSize = 11.sp) },
                        leadingIcon = { if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                    )
                }
            }

            if (selectedPodcastId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = podcastMainSubjectOnly,
                        onClick = { onTogglePodcastMainSubjectOnly(true) },
                        label = { Text("Main Film / Featured Topic", fontSize = 10.sp) },
                        leadingIcon = { if (podcastMainSubjectOnly) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                    )
                    FilterChip(
                        selected = !podcastMainSubjectOnly,
                        onClick = { onTogglePodcastMainSubjectOnly(false) },
                        label = { Text("Any Mention (Inclusive)", fontSize = 10.sp) },
                        leadingIcon = { if (!podcastMainSubjectOnly) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 6: Sort Order
            Text(
                "Sort Order",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("added" to "Recently Added", "rating" to "Highest Rated", "alpha" to "A-Z").forEach { (sortKey, sortLabel) ->
                    FilterChip(
                        selected = sortBy == sortKey,
                        onClick = { onSelectSortBy(sortKey) },
                        label = { Text(sortLabel, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Show $matchingCount Titles", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // TMDB Poster artwork with inset border to prevent perimeter bleeding
                if (!item.imageUrl.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = item.imageUrl,
                        contentDescription = "Poster artwork",
                        modifier = Modifier
                            .size(width = 68.dp, height = 98.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
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
                            color = MaterialTheme.colorScheme.onSurface,
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

                    // TMDB Rating Display with Tabular Figures
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
                                text = String.format(Locale.US, "%.1f", item.rating),
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
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
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Favorite, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Match: $relevanceScore/10",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        if (!item.importSource.isNullOrEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
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
                        // Limit display of providers to 3 to avoid overflow
                        providers.take(3).forEach { pId ->
                            val fullProvider = allProviders.find { it.id == pId }
                            val isSubscribed = activeSubscribedIds.contains(pId)

                            Surface(
                                shape = RoundedCornerShape(6.dp),
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
                                        modifier = Modifier
                                            .size(10.dp)
                                            .then(if (!isSubscribed) Modifier.offset(x = 1.dp) else Modifier),
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
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                // INTENT TRIGGER: Watch now button with concentric radius (20 - 12 = 8dp) and optical centroid alignment
                Button(
                    onClick = onWatchClick,
                    shape = RoundedCornerShape(8.dp),
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
                            modifier = Modifier
                                .size(16.dp)
                                .offset(x = 1.dp) // Optical centroid alignment
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Watch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPOSABLE: Watched History & Cinephile Vault (Issue #9 & #10)
// ==========================================
@Composable
fun WatchedTabContent(
    watchedItems: List<MediaItem>,
    allProviders: List<StreamingProvider>,
    onMovieClick: (MediaItem) -> Unit,
    onDeleteClick: (MediaItem) -> Unit,
    onSyncClick: () -> Unit,
    isSyncingToSheet: Boolean = false,
    onSyncLetterboxdToSheet: () -> Unit = {},
    onSyncLetterboxdLive: () -> Unit = {},
    onRewatchIntent: (MediaItem) -> Unit = {},
    onPickLetterboxdFile: () -> Unit = {},
    onExportLetterboxdCsv: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("timeline") } // "timeline", "alpha", "rating"
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var viewMode by remember { mutableStateOf("timeline") } // "timeline" or "grid"

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
        }.distinctBy { it.title.trim().lowercase() }
    }

    val processedItems = remember(filteredItems, sortBy) {
        when (sortBy) {
            "alpha" -> filteredItems.sortedBy { it.title.lowercase() }
            "rating" -> filteredItems.sortedByDescending { it.rating ?: 0.0 }
            else -> filteredItems.sortedWith(
                compareByDescending<MediaItem> { it.watchedAt ?: it.addedAt }
                    .thenByDescending { it.id }
            )
        }
    }

    // Group items by Year-Month for timeline diary
    val groupedItems = remember(processedItems) {
        val sdf = java.text.SimpleDateFormat("MMMM yyyy", Locale.US)
        processedItems.groupBy { item ->
            val ts = item.watchedAt ?: item.addedAt
            if (ts > 0) sdf.format(java.util.Date(ts)) else "Older Logs"
        }
    }

    // Cinephile Vault Stats
    val totalFilms = watchedItems.size
    val estHours = (totalFilms * 110) / 60
    val ratedFilms = remember(watchedItems) { watchedItems.filter { (it.rating ?: 0.0) > 0.0 } }
    val avgRating = remember(ratedFilms) {
        if (ratedFilms.isNotEmpty()) ratedFilms.map { it.rating!! }.average() else 0.0
    }
    val topGenres = remember(watchedItems) {
        watchedItems.flatMap { it.genres?.split(",")?.map { g -> g.trim() } ?: emptyList() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(4)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cinephile Vault & Letterboxd Overview Card (Issue #9 & #10)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "CINEMATIC VAULT & DIARY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Letterboxd Sync & Chronological Log",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // View Mode Switcher: Timeline vs Grid
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(2.dp)
                    ) {
                        IconButton(
                            onClick = { viewMode = "timeline" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Timeline View",
                                tint = if (viewMode == "timeline") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewMode = "grid" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = "Poster Wall Grid",
                                tint = if (viewMode == "grid") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3-KPI Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "$totalFilms",
                            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("Films Watched", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "~${estHours}h",
                            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("Screen Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "★ ${String.format(Locale.US, "%.1f", avgRating)}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD700)
                        )
                        Text("Avg Rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // Top Genres Breakdown Chips
                if (topGenres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        topGenres.forEach { (genre, count) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "$genre ($count)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Letterboxd Multi-Way Sync Ribbon
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onPickLetterboxdFile,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Import CSV or ZIP",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import CSV/ZIP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = onExportLetterboxdCsv,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export for Letterboxd",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export for LB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSyncLetterboxdLive,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Sync Diary from Letterboxd",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync RSS Diary", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onSyncLetterboxdToSheet,
                            enabled = !isSyncingToSheet,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                        ) {
                            if (isSyncingToSheet) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Syncing...", fontSize = 11.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Sync to Sheet",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync to Sheet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Search & Sorting controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search history...") },
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
                    Text(sortLabel, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    .padding(bottom = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${processedItems.size} Titles in History",
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
                Text("No logged titles found.", color = MaterialTheme.colorScheme.outline)
            }
        } else if (viewMode == "grid") {
            // Poster Wall Grid View
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(processedItems, key = { it.id }) { item ->
                    WatchedGridPosterCard(
                        item = item,
                        onMovieClick = { onMovieClick(item) }
                    )
                }
            }
        } else {
            // Timeline Chronological Diary View (Grouped by Month & Year)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedItems.forEach { (monthYear, itemsInMonth) ->
                    item(key = "header_$monthYear") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = monthYear,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${itemsInMonth.size} ${if (itemsInMonth.size == 1) "film" else "films"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    items(itemsInMonth, key = { it.id }) { item ->
                        WatchedMediaCard(
                            item = item,
                            allProviders = allProviders,
                            onMovieClick = { onMovieClick(item) },
                            onDeleteClick = { onDeleteClick(item) },
                            onRewatchClick = { onRewatchIntent(item) }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPOSABLE: Watched Media Item Card (Issue #9)
// ==========================================
@Composable
fun WatchedMediaCard(
    item: MediaItem,
    allProviders: List<StreamingProvider>,
    onMovieClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRewatchClick: () -> Unit
) {
    val watchDateStr = remember(item.watchedAt, item.addedAt) {
        val ts = item.watchedAt ?: item.addedAt
        if (ts > 0) {
            val sdf = java.text.SimpleDateFormat("MMM d, yyyy", Locale.US)
            sdf.format(java.util.Date(ts))
        } else "Logged"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMovieClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Movie Poster artwork
            if (!item.imageUrl.isNullOrEmpty()) {
                coil.compose.AsyncImage(
                    model = item.imageUrl,
                    contentDescription = "Poster",
                    modifier = Modifier
                        .size(width = 62.dp, height = 90.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Rating & Genres
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    if (item.rating != null && item.rating > 0.0) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", item.rating),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!item.genres.isNullOrEmpty()) {
                            Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    if (!item.genres.isNullOrEmpty()) {
                        Text(
                            text = item.genres,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Formatted Watch Date Badge (Issue #9)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Watched $watchDateStr",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // User Notes / Letterboxd excerpt if available
                if (!item.userNotes.isNullOrBlank()) {
                    Text(
                        text = "\"${item.userNotes}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                // Quick Action Bar: Re-watch intent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onRewatchClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Re-watch",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-watch", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPOSABLE: Poster Wall Grid Card
// ==========================================
@Composable
fun WatchedGridPosterCard(
    item: MediaItem,
    onMovieClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clickable { onMovieClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!item.imageUrl.isNullOrEmpty()) {
                coil.compose.AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            // Top rating badge
            if (item.rating != null && item.rating > 0.0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", item.rating),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Bottom title overlay gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
// ==========================================
@Composable
fun MonthlyRoiContent(
    monthlyStats: List<ProviderUsageStats>,
    allProviders: List<StreamingProvider>,
    watchlistItems: List<MediaItem> = emptyList(),
    onProviderClick: (StreamingProvider) -> Unit = {}
) {
    val activeSubscribed = remember(allProviders) { allProviders.filter { it.isActive } }
    val inactiveProviders = remember(allProviders) { allProviders.filter { !it.isActive } }
    val totalCost = remember(activeSubscribed) { activeSubscribed.sumOf { it.costPerMonth } }
    
    // Sort active channels by costPerHour descending (worst value!) to bubble up prime pausing candidates.
    val activeWithStats = remember(activeSubscribed, monthlyStats) {
        activeSubscribed.map { provider ->
            val st = monthlyStats.find { it.providerId == provider.id }
            val totalHrs = st?.totalHours ?: 0.0
            val usage = st ?: ProviderUsageStats(
                providerId = provider.id,
                providerName = provider.name,
                costPerMonth = provider.costPerMonth,
                isActive = provider.isActive,
                totalMinutes = 0L
            )
            provider to usage
        }.sortedByDescending { it.second.costPerHour }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Summary Budget card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "MY SERVICES & MONTHLY SPEND",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "$${String.format(Locale.US, "%.2f", totalCost)}",
                        style = MaterialTheme.typography.headlineLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val totalActiveQueued = remember(activeSubscribed, watchlistItems) {
                        watchlistItems.count { item ->
                            item.providersList.any { p -> activeSubscribed.any { it.id.equals(p, ignoreCase = true) } }
                        }
                    }
                    Text(
                        "Tracked across ${activeSubscribed.size} active services ($totalActiveQueued watchlist titles ready to stream). Threshold: ≥3h/mo per subscription for positive value.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "💡 Tap any service to edit price/plan, view tenure, or search live deals",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            Text(
                "Active Subscriptions & Usage (This Month)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (activeWithStats.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Text(
                        "No active services yet. Tap any service below or configure in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(activeWithStats, key = { it.first.id }) { (provider, stats) ->
                val isPrimeCancelCandidate = stats.totalHours < 3.0 && provider.costPerMonth > 0.0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProviderClick(provider) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPrimeCancelCandidate) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isPrimeCancelCandidate) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        else Color.White.copy(alpha = 0.08f)
                    ),
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
                                    provider.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                provider.planName?.let { pName ->
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            pName,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                if (isPrimeCancelCandidate) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text("LOW VALUE", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "Watched: ${String.format(Locale.US, "%.1f", stats.totalHours)}h",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    "Cost: $${String.format(Locale.US, "%.2f", provider.costPerMonth)}/mo",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val queueCount = remember(watchlistItems, provider.id) {
                                watchlistItems.count { it.providersList.any { p -> p.equals(provider.id, ignoreCase = true) } }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (queueCount > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = if (queueCount > 0) "🎬 $queueCount Watchlist Titles" else "0 in Watchlist",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (queueCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "$${String.format(Locale.US, "%.2f", stats.costPerHour)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Black,
                                    color = if (isPrimeCancelCandidate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "per hour",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Edit Service & Find Deals",
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Inactive / Other Available Services Section
        if (inactiveProviders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Other Streaming Services (Tap to Activate or View Deals)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(inactiveProviders, key = { it.id }) { provider ->
                val queueCount = remember(watchlistItems, provider.id) {
                    watchlistItems.count { it.providersList.any { p -> p.equals(provider.id, ignoreCase = true) } }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProviderClick(provider) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(provider.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                if (queueCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            "🎬 $queueCount Queued",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                if (provider.costPerMonth > 0) "$${provider.costPerMonth}/mo • Inactive" else "Free Platform",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        OutlinedButton(
                            onClick = { onProviderClick(provider) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Details / Deals ↗", fontSize = 10.sp)
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
// COMPOSABLE: Service Detail & Deal Finder BottomSheet
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailBottomSheet(
    provider: StreamingProvider,
    stats: ProviderUsageStats?,
    watchlistItems: List<MediaItem> = emptyList(),
    onSelectMovie: (MediaItem) -> Unit = {},
    onUpdateProvider: (StreamingProvider) -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var costInput by remember(provider) { mutableStateOf(provider.costPerMonth.toString()) }
    var planNameInput by remember(provider) { mutableStateOf(provider.planName ?: "") }
    var renewalDayInput by remember(provider) { mutableStateOf(provider.renewalDayOfMonth?.toString() ?: "") }
    var isActive by remember(provider) { mutableStateOf(provider.isActive) }
    var subscribedSince by remember(provider) { mutableStateOf(provider.subscribedSince) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = provider.name.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column {
                        Text(
                            text = provider.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isActive) "Active Subscription" else "Inactive / Not Subscribed",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Plan & Cost Section
            Text(
                text = "Plan & Billing Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = planNameInput,
                    onValueChange = { planNameInput = it },
                    label = { Text("Plan Tier") },
                    placeholder = { Text("Standard, Ad-Free...") },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = costInput,
                    onValueChange = { costInput = it },
                    label = { Text("Cost ($/mo)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(0.8f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = renewalDayInput,
                onValueChange = { renewalDayInput = it },
                label = { Text("Billing Renewal Day (1 - 31)") },
                placeholder = { Text("e.g. 15") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Tenure & ROI Calculation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Subscription Tenure & Value Metrics",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    val now = System.currentTimeMillis()
                    val tenureDays = if (subscribedSince != null && subscribedSince!! > 0) {
                        ((now - subscribedSince!!) / (1000L * 60 * 60 * 24)).coerceAtLeast(0)
                    } else 0L
                    val tenureMonths = tenureDays / 30

                    val tenureString = if (subscribedSince != null && subscribedSince!! > 0) {
                        val dateFormatted = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(subscribedSince!!))
                        if (tenureMonths > 0) "$tenureMonths months ($tenureDays days) • Since $dateFormatted"
                        else "$tenureDays days • Since $dateFormatted"
                    } else "Not specified"

                    Text(
                        text = "Tenure: $tenureString",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quick set tenure chips
                    Text(
                        text = "Quick-set subscription start:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            "This Month" to 15L,
                            "3 Mos" to 90L,
                            "6 Mos" to 180L,
                            "1 Year" to 365L,
                            "2+ Years" to 730L
                        )
                        presets.forEach { (label, daysAgo) ->
                            AssistChip(
                                onClick = {
                                    subscribedSince = now - (daysAgo * 24 * 60 * 60 * 1000L)
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (stats != null) {
                        val cost = costInput.toDoubleOrNull() ?: provider.costPerMonth
                        val hours = stats.totalHours.toDouble()
                        val roiPerHr = if (hours > 0.0) cost / hours else cost

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Hours Watched", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(String.format(Locale.US, "%.1f hrs", hours), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Minutes Streamed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("${stats.totalMinutes} min", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Cost / Hour", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = String.format(Locale.US, "$%.2f/hr", roiPerHr),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (roiPerHr <= 2.50) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Watchlist Queue on this Service
            val availableWatchlist = remember(watchlistItems, provider.id) {
                watchlistItems.filter { it.providersList.any { p -> p.equals(provider.id, ignoreCase = true) } }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "🎬",
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Watchlist on ${provider.name}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${availableWatchlist.size} Titles",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (availableWatchlist.isEmpty()) {
                        Text(
                            text = "No movies currently on your watchlist are available to stream on ${provider.name}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        availableWatchlist.take(10).forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectMovie(item) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (item.rating != null && item.rating > 0.0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "★ ${String.format(Locale.US, "%.1f", item.rating)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                        if (availableWatchlist.size > 10) {
                            Text(
                                text = "+ ${availableWatchlist.size - 10} more titles on this service",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Live Web Deals & Discount Search (The Streamable, Slickdeals, Doctor of Credit)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Live Deal Finder & Aggregators",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Check independent deal trackers and promo communities for current discounts, Black Friday offers, annual plan savings, and student deals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val cleanSearchQuery = provider.name.replace("+", " Plus")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val url = "https://thestreamable.com/?s=" + java.net.URLEncoder.encode("$cleanSearchQuery deal", "UTF-8")
                                uriHandler.openUri(url)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("The Streamable Deals ↗", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val url = "https://slickdeals.net/newsearch.php?q=" + java.net.URLEncoder.encode("$cleanSearchQuery deal", "UTF-8")
                                uriHandler.openUri(url)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Slickdeals ↗", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val url = "https://www.doctorofcredit.com/?s=" + java.net.URLEncoder.encode("$cleanSearchQuery deal", "UTF-8")
                                uriHandler.openUri(url)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Doctor of Credit ↗", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val url = "https://www.google.com/search?q=" + java.net.URLEncoder.encode("$cleanSearchQuery streaming promo discounts deals", "UTF-8")
                                uriHandler.openUri(url)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Google Deals ↗", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Save & Close Button
            Button(
                onClick = {
                    val parsedCost = costInput.toDoubleOrNull() ?: provider.costPerMonth
                    val parsedRenewalDay = renewalDayInput.toIntOrNull()?.coerceIn(1, 31)
                    val updated = provider.copy(
                        costPerMonth = parsedCost,
                        isActive = isActive,
                        planName = planNameInput.trim().ifEmpty { null },
                        renewalDayOfMonth = parsedRenewalDay,
                        subscribedSince = subscribedSince
                    )
                    onUpdateProvider(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Service Changes")
            }
        }
    }
}

// ==========================================
// COMPOSABLE: Letterboxd Live RSS Sync Dialog
// ==========================================
@Composable
fun LetterboxdSyncDialog(
    isSyncing: Boolean,
    result: LetterboxdSyncResult?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.RssFeed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Letterboxd Live RSS Sync", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isSyncing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Text(
                            "Pulling live RSS feed from letterboxd.com...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Parsing diary entries, ratings, and watch dates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (result != null) {
                    if (result.errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sync Error: ${result.errorMessage}",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "Sync Complete!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Found ${result.totalFetched} items in RSS feed. Added ${result.newlyImportedCount} new movies to your Watched Vault.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (result.newlyImportedItems.isNotEmpty()) {
                            Text(
                                "Newly Imported Diary Entries:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            ) {
                                LazyColumn(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(result.newlyImportedItems) { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (item.rating != null) {
                                                Text(
                                                    text = "★ ${String.format(Locale.US, "%.1f", item.rating / 2.0)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                "Your Watched Vault is already completely up to date with your Letterboxd diary!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                enabled = !isSyncing
            ) {
                Text("Done")
            }
        }
    )
}

// ==========================================
// COMPOSABLE: HTML Asset Viewer Dialog (User Guide & Changelog)
// ==========================================
@Composable
fun HtmlAssetViewerDialog(
    title: String,
    assetFileName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            webViewClient = WebViewClient()
                            loadUrl("file:///android_asset/$assetFileName")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ==========================================
// COMPOSABLE: Settings Dialog (4-Tab Unified Hub)
// ==========================================
@Composable
fun SettingsDialog(
    allProviders: List<StreamingProvider>,
    onProviderToggle: (String, Boolean) -> Unit,
    letterboxdUsername: String,
    onSaveLetterboxdUsername: (String) -> Unit,
    userName: String,
    onSaveUserName: (String) -> Unit,
    onSyncLetterboxdLive: () -> Unit,
    onOpenUserGuide: () -> Unit,
    onOpenChangelog: () -> Unit,
    updateStatus: UpdateStatus,
    onCheckForUpdates: () -> Unit,
    onDownloadAndInstallUpdate: (String) -> Unit,
    onResetUpdateStatus: () -> Unit,
    tmdbApiKey: String,
    onSaveTmdbApiKey: (String) -> Unit,
    geminiApiKey: String,
    onSaveGeminiApiKey: (String) -> Unit,
    watchmodeApiKey: String,
    onSaveWatchmodeApiKey: (String) -> Unit,
    ollamaHost: String,
    onSaveOllamaHost: (String) -> Unit,
    githubToken: String,
    onSaveGithubToken: (String) -> Unit,
    googleSheetWebhookUrl: String = "",
    onSaveGoogleSheetWebhookUrl: (String) -> Unit = {},
    isSyncingToSheet: Boolean = false,
    onSyncLetterboxdToSheet: () -> Unit = {},
    isSyncingPodcasts: Boolean = false,
    onSyncPodcastRecs: () -> Unit = {},
    enableBetaFeedback: Boolean = true,
    onToggleBetaFeedback: (Boolean) -> Unit = {},
    onPickLetterboxdFile: () -> Unit = {},
    onExportLetterboxdCsv: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0: Profile, 1: Services, 2: Guides & Docs, 3: Updates & System
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings & Hub", fontWeight = FontWeight.Bold)
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
                TabRow(
                    selectedTabIndex = activeSubTab,
                    containerColor = Color.Transparent,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Tab(
                        selected = activeSubTab == 0,
                        onClick = { activeSubTab = 0 },
                        text = { Text("Profile", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = activeSubTab == 1,
                        onClick = { activeSubTab = 1 },
                        text = { Text("Services", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = activeSubTab == 2,
                        onClick = { activeSubTab = 2 },
                        text = { Text("Guides", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = activeSubTab == 3,
                        onClick = { activeSubTab = 3 },
                        text = { Text("System", fontSize = 11.sp) }
                    )
                }

                when (activeSubTab) {
                    // TAB 0: PROFILE & LETTERBOXD
                    0 -> {
                        var nameInput by remember(userName) { mutableStateOf(userName) }
                        var lbInput by remember(letterboxdUsername) { mutableStateOf(letterboxdUsername) }
                        var sheetWebhookInput by remember(googleSheetWebhookUrl) { mutableStateOf(googleSheetWebhookUrl) }
                        val scrollState = rememberScrollState()

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            Text("User Profile & Accounts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Display Name", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = nameInput,
                                        onValueChange = { nameInput = it },
                                        label = { Text("Your Name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = { onSaveUserName(nameInput) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Save Name", fontSize = 12.sp)
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Letterboxd Account & Sync", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "@${lbInput.ifBlank { "scriptedmind" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text("Bidirectional syncing: Import watchlist/watched CSV/ZIP, sync live RSS, and export to Letterboxd import format.", style = MaterialTheme.typography.bodySmall)

                                    OutlinedTextField(
                                        value = lbInput,
                                        onValueChange = { lbInput = it },
                                        label = { Text("Letterboxd Username") },
                                        placeholder = { Text("e.g. scriptedmind") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onSaveLetterboxdUsername(lbInput) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Save Username", fontSize = 12.sp)
                                        }

                                        FilledTonalButton(
                                            onClick = onSyncLetterboxdLive,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.RssFeed, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sync RSS", fontSize = 12.sp)
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    Text("LOCAL FILE IMPORT / EXPORT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = onPickLetterboxdFile,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Import CSV/ZIP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        FilledTonalButton(
                                            onClick = onExportLetterboxdCsv,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Export for LB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    Text("ONE-TAP LETTERBOXD WEB ACTIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)

                                    val user = lbInput.ifBlank { "scriptedmind" }
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                uriHandler.openUri("https://letterboxd.com/$user/watchlist/export/")
                                            } catch (e: Exception) {}
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("📥 Download Watchlist CSV (letterboxd.com)", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                uriHandler.openUri("https://letterboxd.com/settings/data/")
                                            } catch (e: Exception) {}
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("📦 Export All Account Data ZIP (Settings)", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                uriHandler.openUri("https://letterboxd.com/import/")
                                            } catch (e: Exception) {}
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🌐 Open Letterboxd Importer (/import/)", fontSize = 11.sp)
                                    }
                                }
                            }

                            // Google Sheet Webhook Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Google Sheet Ledger Webhook", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Text("Apps Script Webhook to sync watched and watchlist items to your Google Drive sheet ($0/mo).", style = MaterialTheme.typography.bodySmall)

                                    OutlinedTextField(
                                        value = sheetWebhookInput,
                                        onValueChange = { sheetWebhookInput = it },
                                        label = { Text("Apps Script Webhook URL") },
                                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onSaveGoogleSheetWebhookUrl(sheetWebhookInput) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Save URL", fontSize = 12.sp)
                                        }

                                        FilledTonalButton(
                                            onClick = onSyncLetterboxdToSheet,
                                            enabled = !isSyncingToSheet && sheetWebhookInput.isNotBlank(),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            if (isSyncingToSheet) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sync Sheet", fontSize = 12.sp)
                                        }
                                    }

                                    FilledTonalButton(
                                        onClick = onSyncPodcastRecs,
                                        enabled = !isSyncingPodcasts && sheetWebhookInput.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        if (isSyncingPodcasts) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🎙️ Sync Gemini Spark Podcast Recs", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // TAB 1: STREAMING SERVICES (19 SERVICES)
                    1 -> {
                        val scrollState = rememberScrollState()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                "Streaming Services (${allProviders.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Toggle services you pay for or access. Filter ribbon and ROI stats adapt dynamically.",
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
                                                text = if (provider.costPerMonth > 0) "${provider.costPerMonth}/mo" + (provider.planName?.let { " • $it" } ?: "") else "Free Platform ($0.0)",
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

                    // TAB 2: GUIDES & DOCS
                    2 -> {
                        val scrollState = rememberScrollState()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            Text("In-App Guides & Documentation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text("Streamwise User Guide", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        "Learn all features: Watchlist management, podcast filters, ROI tracking, live deal searches, and Letterboxd RSS diary sync.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Button(
                                        onClick = onOpenUserGuide,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Open User Guide", fontSize = 12.sp)
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                        Text("Streamwise Changelog", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        "Review the latest updates across v1.4.0, v1.3.1, and earlier releases with detailed feature breakdowns.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Button(
                                        onClick = onOpenChangelog,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Text("Open Changelog", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // TAB 3: SYSTEM & UPDATES
                    3 -> {
                        var tmdbInput by remember(tmdbApiKey) { mutableStateOf(tmdbApiKey) }
                        var geminiInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
                        var wmInput by remember(watchmodeApiKey) { mutableStateOf(watchmodeApiKey) }
                        var hostInput by remember(ollamaHost) { mutableStateOf(ollamaHost) }
                        var tokenInput by remember(githubToken) { mutableStateOf(githubToken) }
                        var showTmdb by remember { mutableStateOf(false) }
                        var showGemini by remember { mutableStateOf(false) }
                        var showWm by remember { mutableStateOf(false) }
                        var showToken by remember { mutableStateOf(false) }
                        val scrollState = rememberScrollState()

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            Text("OTA Updates & System Config", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                            // GitHub OTA Auto-Updater Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("App Version", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Text("v1.4.0 (Build 140)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text("v1.4.0", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }

                                    when (updateStatus) {
                                        is UpdateStatus.Checking -> {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Text("Checking GitHub Releases...", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        is UpdateStatus.UpdateAvailable -> {
                                            Text("Update Found: ${updateStatus.tagName} (${updateStatus.releaseName})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Button(
                                                onClick = { onDownloadAndInstallUpdate(updateStatus.apkDownloadUrl) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Download & Install Update", fontSize = 12.sp)
                                            }
                                        }
                                        is UpdateStatus.Downloading -> {
                                            val progress = updateStatus.progressPercent
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("Downloading APK: $progress%", style = MaterialTheme.typography.labelSmall)
                                                LinearProgressIndicator(
                                                    progress = { progress / 100f },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                        is UpdateStatus.ReadyToInstall -> {
                                            Text("Download complete! Launching Android Package Installer...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        is UpdateStatus.UpToDate -> {
                                            Text("✓ Streamwise is up to date with the latest GitHub release.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        is UpdateStatus.Error -> {
                                            Text("Update check failed: ${updateStatus.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        }
                                        UpdateStatus.Idle -> {
                                            Text("Check for newer versions published on GitHub Releases.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }

                                    Button(
                                        onClick = onCheckForUpdates,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Check for Updates", fontSize = 12.sp)
                                    }
                                }
                            }

                            // TMDB API Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("TMDB API Key", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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

                            // Gemini API Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Gemini API (AI Explore)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = geminiInput,
                                        onValueChange = { geminiInput = it },
                                        label = { Text("Gemini API Key") },
                                        singleLine = true,
                                        visualTransformation = if (showGemini) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showGemini = !showGemini }) {
                                                Icon(imageVector = if (showGemini) Icons.Default.Clear else Icons.Default.Search, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = { onSaveGeminiApiKey(geminiInput) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Save Gemini Key", fontSize = 12.sp)
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
                                    Text("Watchmode API (Deep Links)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                                }
                            }

                            // Local AI (Ollama Host) Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Local AI Synthesis (Ollama)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Save Ollama Host", fontSize = 12.sp)
                                    }
                                }
                            }

                            // GitHub PAT Token Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("GitHub Token (PAT)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = { onSaveGithubToken(tokenInput) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary)
                                    ) {
                                        Text("Save GitHub Token", fontSize = 12.sp)
                                    }
                                }
                            }

                            // Beta Feedback FAB Toggle Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            "Enable Beta Feedback FAB",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Display floating button on all screens to capture screen diagnostics and file issues to the project backlog.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Switch(
                                        checked = enableBetaFeedback,
                                        onCheckedChange = onToggleBetaFeedback,
                                        modifier = Modifier.testTag("enable_feedback_switch")
                                    )
                                }
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
// COMPOSABLE: Synthesized Movie Insights
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SynthesizedMovieInsights(
    trivia: String,
    movieTitle: String,
    onDiscussInExplore: ((String) -> Unit)? = null
) {
    val lines = remember(trivia) { trivia.lines() }
    
    val focusTopics = remember(trivia) { mutableListOf<String>() }
    val featuredCast = remember(trivia) { mutableListOf<String>() }
    var synthesisDate: String? by remember(trivia) { mutableStateOf(null) }
    val insightCards = remember(trivia) { mutableListOf<Pair<String, String>>() }

    LaunchedEffect(trivia) {
        var inYaml = false
        var afterYaml = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == "---") {
                if (!inYaml && !afterYaml) {
                    inYaml = true
                } else if (inYaml) {
                    inYaml = false
                    afterYaml = true
                }
                continue
            }

            if (inYaml) {
                if (trimmed.startsWith("focus_topics:")) {
                    val value = trimmed.removePrefix("focus_topics:").trim().trim('"', '\'')
                    focusTopics.addAll(value.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                } else if (trimmed.startsWith("featured_cast:")) {
                    val value = trimmed.removePrefix("featured_cast:").trim().trim('"', '\'')
                    featuredCast.addAll(value.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                } else if (trimmed.startsWith("agent_synthesis_date:")) {
                    synthesisDate = trimmed.removePrefix("agent_synthesis_date:").trim().trim('"', '\'')
                }
            } else {
                if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                    val bullet = trimmed.substring(2).trim()
                    if (bullet.contains(":")) {
                        val split = bullet.split(":", limit = 2)
                        insightCards.add(split[0].trim() to split[1].trim())
                    } else {
                        insightCards.add("Key Insight" to bullet)
                    }
                } else if (!trimmed.startsWith("#") && trimmed.isNotBlank() && !trimmed.startsWith("---")) {
                    if (insightCards.isEmpty() && focusTopics.isEmpty()) {
                        insightCards.add("Cinephile Overview" to trimmed)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "AGENTIC RESEARCH STRATEGY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (!synthesisDate.isNullOrBlank()) "Curated by Olivia AI • $synthesisDate" else "Synthesized by Olivia AI Concierge",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Focus Topics & Mood
        if (focusTopics.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "THEMES & CINEMATIC MOOD",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    focusTopics.forEach { topic ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = topic,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Featured Cast & Talent
        if (featuredCast.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "FEATURED CAST & TALENT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    featuredCast.forEach { castMember ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = castMember,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sectioned Insight Cards
        if (insightCards.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                insightCards.forEach { (heading, body) ->
                    val icon = when {
                        heading.contains("Cultural", ignoreCase = true) || heading.contains("Impact", ignoreCase = true) -> Icons.Default.Public
                        heading.contains("Talent", ignoreCase = true) || heading.contains("Cast", ignoreCase = true) || heading.contains("Profile", ignoreCase = true) -> Icons.Default.People
                        heading.contains("Smart", ignoreCase = true) || heading.contains("Sourcing", ignoreCase = true) || heading.contains("Vault", ignoreCase = true) -> Icons.Default.AutoAwesome
                        else -> Icons.Default.Star
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = heading,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Graceful fallback for plain text
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = trivia,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Bridge to Explore Tab
        if (onDiscussInExplore != null) {
            OutlinedButton(
                onClick = {
                    onDiscussInExplore("Analyze the cinematic themes and director style of '$movieTitle', and recommend 3 similar movies streaming on my active services.")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Discuss with Olivia & Gemini in Explore",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
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
    onDeleteClick: () -> Unit,
    onDiscussInExplore: ((String) -> Unit)? = null
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
                SynthesizedMovieInsights(
                    trivia = item.trivia,
                    movieTitle = item.title,
                    onDiscussInExplore = onDiscussInExplore
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Action Items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Letterboxd / Web Source link (Issue #10)
                val letterboxdUrl = if (!item.sharedUrl.isNullOrEmpty()) {
                    item.sharedUrl
                } else {
                    "https://letterboxd.com/search/${android.net.Uri.encode(item.title)}/"
                }
                OutlinedButton(
                    onClick = { uriHandler.openUri(letterboxdUrl) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Letterboxd", fontSize = 12.sp)
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
// COMPOSABLE: Explore Tab (AI & Cinematic Culture)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreTabContent(
    selectedSubTab: Int = 0,
    onSubTabChange: (Int) -> Unit = {},
    chatMessages: List<com.example.data.remote.OllamaChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    geminiAnalysis: com.example.data.model.GeminiAnalysisResult?,
    isGeminiAnalyzing: Boolean,
    onRefreshGeminiAnalysis: () -> Unit,
    podcastEpisodes: List<com.example.data.model.PodcastEpisode>,
    movieNews: List<com.example.data.model.MovieNewsItem>,
    watchedItems: List<MediaItem> = emptyList(),
    isSyncingPodcasts: Boolean = false,
    onSyncPodcastRecs: () -> Unit = {},
    onAddRecommendation: ((String, String) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("explore_tab_content")
    ) {
        // Sub-Navigation Selector: 4 Modular Tabs
        // 0: ✨ AI & Taste, 1: 💬 Olivia AI, 2: 🎙️ Podcasts, 3: 📰 Film News
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: AI & Taste
                FilterChip(
                    selected = selectedSubTab == 0,
                    onClick = { onSubTabChange(0) },
                    label = { Text("✨ AI & Taste", fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("subtab_taste"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Tab 1: Chat with Olivia
                FilterChip(
                    selected = selectedSubTab == 1,
                    onClick = { onSubTabChange(1) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💬 Olivia AI", fontWeight = FontWeight.Bold)
                            if (chatMessages.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "${chatMessages.size}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("subtab_chat"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Tab 2: Podcasts
                FilterChip(
                    selected = selectedSubTab == 2,
                    onClick = { onSubTabChange(2) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎙️ Podcasts", fontWeight = FontWeight.Bold)
                            if (podcastEpisodes.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "${podcastEpisodes.size}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Headphones,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("subtab_podcasts"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Tab 3: Film News
                FilterChip(
                    selected = selectedSubTab == 3,
                    onClick = { onSubTabChange(3) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📰 Film News", fontWeight = FontWeight.Bold)
                            if (movieNews.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "${movieNews.size}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Article,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("subtab_news"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        when (selectedSubTab) {
            0 -> {
                // SubTab 0: ✨ AI & Taste (Compact, fast to read, zero unnecessary scrolling)
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 0. CINEPHILE TASTE MATRIX (Dynamic visuals calculated from Watched Vault)
                    if (watchedItems.isNotEmpty()) {
                        val genreCounts = remember(watchedItems) {
                            watchedItems.mapNotNull { it.genres }
                                .flatMap { it.split(",", "/").map { g -> g.trim() } }
                                .filter { it.isNotEmpty() }
                                .groupingBy { it }
                                .eachCount()
                                .entries
                                .sortedByDescending { it.value }
                                .take(4)
                        }
                        val topGenreSum = remember(genreCounts) { genreCounts.sumOf { it.value }.toFloat().coerceAtLeast(1f) }
                        val palette = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.secondary,
                            Color(0xFF00B4D8)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("taste_matrix_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BarChart,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            "CINEPHILE TASTE MATRIX",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Text(
                                        "${watchedItems.size} Titles Analyzed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (genreCounts.isNotEmpty()) {
                                    // Multi-segment taste distribution bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        genreCounts.forEachIndexed { index, entry ->
                                            val weight = (entry.value / topGenreSum).coerceAtLeast(0.05f)
                                            val color = palette[index % palette.size]
                                            Box(
                                                modifier = Modifier
                                                    .weight(weight)
                                                    .fillMaxHeight()
                                                    .background(color)
                                            )
                                        }
                                    }

                                    // Legend row
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        genreCounts.forEachIndexed { index, entry ->
                                            val color = palette[index % palette.size]
                                            val pct = ((entry.value / topGenreSum) * 100).toInt()
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                )
                                                Text(
                                                    "${entry.key} ($pct%)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 1. GEMINI PRO INTELLIGENCE CARD (Custom content driven by Gemini Pro API analysis)
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("gemini_pro_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Card Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "Gemini Pro",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            "CINEPHILE SYNTHESIS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            "Powered by Gemini 3.1 Pro",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                // Refresh Analysis Button
                                IconButton(
                                    onClick = onRefreshGeminiAnalysis,
                                    enabled = !isGeminiAnalyzing,
                                    modifier = Modifier.size(36.dp).testTag("refresh_gemini_button")
                                ) {
                                    if (isGeminiAnalyzing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh Gemini Pro Analysis",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (geminiAnalysis != null) {
                                // Headline
                                Text(
                                    text = geminiAnalysis.headline,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Narrative synthesis
                                Text(
                                    text = geminiAnalysis.narrative,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )

                                // Detected Core Themes Chips
                                if (geminiAnalysis.themes.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            "VAULT THEMES",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            geminiAnalysis.themes.forEach { theme ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                                ) {
                                                    Text(
                                                        text = theme,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Auteur & Cinematographer Ties
                                if (geminiAnalysis.auteurConnections.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            "AUTEUR & STYLISTIC LINKS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        geminiAnalysis.auteurConnections.forEach { connection ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = connection,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Curated Recommendations
                                if (geminiAnalysis.recommendations.isNotEmpty()) {
                                    var addedRecTitles by remember { mutableStateOf(setOf<String>()) }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "GEMINI PRO TAILORED PICKS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        geminiAnalysis.recommendations.forEach { (title, reason) ->
                                            val isAdded = addedRecTitles.contains(title)

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = title,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = reason,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                    if (onAddRecommendation != null) {
                                                        FilledTonalButton(
                                                            onClick = {
                                                                onAddRecommendation(title, reason)
                                                                addedRecTitles = addedRecTitles + title
                                                            },
                                                            enabled = !isAdded,
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(34.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Add,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                if (isAdded) "Added" else "+ Watchlist",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Action button: Chat about this analysis
                                OutlinedButton(
                                    onClick = {
                                        onSubTabChange(1)
                                        onSendMessage("Tell me more about the cinema analysis of my vault: ${geminiAnalysis.headline}")
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Discuss Analysis with Olivia", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (isGeminiAnalyzing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Synthesizing vault profile with Gemini Pro...", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // 2. QUICK CONVERSATION STARTERS (Jump to Chat)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "PROMPT OLIVIA DIRECTLY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 0.5.sp
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val prompts = listOf(
                                "What should I watch tonight?",
                                "Recommend a 70s neo-noir",
                                "Explain Denis Villeneuve's style",
                                "Which service should I cancel?"
                            )
                            prompts.forEach { p ->
                                SuggestionChip(
                                    onClick = {
                                        onSubTabChange(1)
                                        onSendMessage(p)
                                    },
                                    label = { Text(p, fontSize = 11.sp) },
                                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            1 -> {
                // SubTab 1: Chat with Olivia
                AgentChatTabContent(
                    chatMessages = chatMessages,
                    isLoading = isLoading,
                    onSendMessage = onSendMessage
                )
            }

            2 -> {
                // SubTab 2: 🎙️ Podcasts Hub
                PodcastsExploreView(
                    podcastEpisodes = podcastEpisodes,
                    isSyncingPodcasts = isSyncingPodcasts,
                    onSyncPodcastRecs = onSyncPodcastRecs,
                    onAskOlivia = { prompt ->
                        onSubTabChange(1)
                        onSendMessage(prompt)
                    }
                )
            }

            3 -> {
                // SubTab 3: 📰 Film News Hub
                NewsExploreView(
                    movieNews = movieNews,
                    onDiscussWithOlivia = { prompt ->
                        onSubTabChange(1)
                        onSendMessage(prompt)
                    }
                )
            }
        }
    }
}

// ==========================================
// COMPOSABLE: Podcasts Explore View (SubTab 2)
// ==========================================
@Composable
fun PodcastsExploreView(
    podcastEpisodes: List<com.example.data.model.PodcastEpisode>,
    isSyncingPodcasts: Boolean = false,
    onSyncPodcastRecs: () -> Unit = {},
    onAskOlivia: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var selectedShow by remember { mutableStateOf("All Shows") }

    val allShows = remember(podcastEpisodes) {
        listOf("All Shows") + podcastEpisodes.map { it.showTitle }.distinct()
    }
    val filteredEpisodes = remember(selectedShow, podcastEpisodes) {
        if (selectedShow == "All Shows") podcastEpisodes
        else podcastEpisodes.filter { it.showTitle.equals(selectedShow, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("podcasts_explore_view")
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Gemini Spark Podcast Recs Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        "Gemini Spark Tracker",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        "Sync new film recommendations from Google Sheet into Watchlist & Vault",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
                FilledTonalButton(
                    onClick = onSyncPodcastRecs,
                    enabled = !isSyncingPodcasts,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (isSyncingPodcasts) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Recs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Show Filter Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(allShows) { show ->
                FilterChip(
                    selected = selectedShow == show,
                    onClick = { selectedShow = show },
                    label = {
                        Text(
                            show,
                            fontSize = 12.sp,
                            fontWeight = if (selectedShow == show) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }

        // Subtitle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "CINEMA PODCASTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            Text(
                "${filteredEpisodes.size} episodes available",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Episodes LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredEpisodes, key = { it.podcastUrl + it.episodeTitle }) { pod ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = pod.showTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = pod.duration,
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Text(
                            text = pod.episodeTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = pod.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        uriHandler.openUri(pod.podcastUrl)
                                    } catch (e: Exception) {}
                                },
                                modifier = Modifier.weight(1f).height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Listen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    onAskOlivia("What do you think about the discussion in this episode: '${pod.showTitle} - ${pod.episodeTitle}'?")
                                },
                                modifier = Modifier.weight(1f).height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ask Olivia", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ==========================================
// COMPOSABLE: Film News Explore View (SubTab 3)
// ==========================================
@Composable
fun NewsExploreView(
    movieNews: List<com.example.data.model.MovieNewsItem>,
    onDiscussWithOlivia: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All News") }

    val allCategories = remember(movieNews) {
        listOf("All News") + movieNews.map { it.category }.distinct()
    }
    val filteredNews = remember(selectedCategory, movieNews) {
        if (selectedCategory == "All News") movieNews
        else movieNews.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("news_explore_view")
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Category Filter Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(allCategories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text(
                            cat,
                            fontSize = 12.sp,
                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )
            }
        }

        // Subtitle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "INDUSTRY DISPATCHES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            Text(
                "${filteredNews.size} dispatches",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // News LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredNews, key = { it.title + it.date }) { news ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = news.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "${news.source} • ${news.date}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Text(
                            text = news.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = news.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    onDiscussWithOlivia("Tell me more about this news item: '${news.title}'")
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Discuss with Olivia", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

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
                    if (inputMessage.isNotBlank()) {
                        onSendMessage(inputMessage)
                        inputMessage = ""
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
            }
        }
    }
}

