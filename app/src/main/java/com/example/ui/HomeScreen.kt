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
    var selectedTab by remember { mutableStateOf(0) } // 0: Watchlist, 1: Budget ROI, 2: My Services, 3: Settings
    var filterOnlyMyServices by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

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
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Stream Manager",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Maximize Hours. Minimize Spent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                actions = {
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
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Watchlist") },
                    icon = { Icon(Icons.Default.List, contentDescription = "Watchlist tab") },
                    modifier = Modifier.testTag("tab_watchlist")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Budget ROI") },
                    icon = { Icon(Icons.Default.Star, contentDescription = "ROI stats tab") },
                    modifier = Modifier.testTag("tab_budget")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("My Services") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "My services tab") },
                    modifier = Modifier.testTag("tab_providers")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Settings") },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Settings tab") },
                    modifier = Modifier.testTag("tab_settings")
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
                        onSyncClick = { viewModel.triggerImmediateSync() }
                    )
                    1 -> MonthlyRoiContent(
                        monthlyStats = monthlyStats,
                        allProviders = allProviders
                    )
                    2 -> ManageServicesTabContent(
                        allProviders = allProviders,
                        onProviderToggle = { id, active -> viewModel.toggleStreamingProvider(id, active) }
                    )
                    3 -> SettingsTabContent(
                        tmdbApiKey = tmdbApiKey,
                        onSaveTmdbApiKey = { viewModel.saveTmdbApiKey(it) }
                    )
                }
            }
        }

        // Add Dialog
        if (showAddDialog) {
            AddMediaDialog(
                allProviders = allProviders,
                onDismiss = { showAddDialog = false },
                onAdd = { title, selectedProviderIds ->
                    viewModel.addCustomWatchlistItem(title, selectedProviderIds)
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
    onSyncClick: () -> Unit = {}
) {
    val activeProviderIds = remember(allProviders) {
        allProviders.filter { it.isActive }.map { it.id }.toSet()
    }

    // Filter items according to state
    val filteredItems = remember(watchlistItems, filterOnlyMyServices, activeProviderIds) {
        watchlistItems.filter { item ->
            // Exclude already watched from immediate watchlist
            if (item.status == MediaStatus.WATCHED.name) return@filter false

            if (filterOnlyMyServices) {
                // Return items having at least one of their available platforms as locally active/subscribed
                val provs = item.providersList
                provs.isEmpty() || provs.any { activeProviderIds.contains(it) }
            } else {
                true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Filter Selection Pills + Sync button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !filterOnlyMyServices,
                onClick = { onFilterToggle(false) },
                label = { Text("All Entries") },
                leadingIcon = { Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("filter_all_chip")
            )

            FilterChip(
                selected = filterOnlyMyServices,
                onClick = { onFilterToggle(true) },
                label = { Text("Available on My Services") },
                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("filter_subscribed_chip")
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

        if (filteredItems.isEmpty()) {
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
                items(filteredItems, key = { it.id }) { item ->
                    MediaItemCard(
                        item = item,
                        allProviders = allProviders,
                        onWatchClick = { onWatchClick(item) },
                        onDeleteClick = { onDeleteClick(item) }
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
    onDeleteClick: () -> Unit
) {
    val activeSubscribedIds = remember(allProviders) {
        allProviders.filter { it.isActive }.map { it.id }.toSet()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
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

// ==========================================
// COMPOSABLE: Budget ROI Analyzer
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
    onAdd: (String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    val selectedProviders = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Movie or Show", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Severance") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_input_title"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
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
                onClick = { if (title.isNotBlank()) onAdd(title, selectedProviders.toList()) },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("add_dialog_confirm")
            ) {
                Text("Add to Watchlist")
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
    val activeList = remember(allProviders) { allProviders.filter { it.isActive } }
    
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
// COMPOSABLE: Settings Screen
// ==========================================
@Composable
fun SettingsTabContent(
    tmdbApiKey: String,
    onSaveTmdbApiKey: (String) -> Unit
) {
    val context = LocalContext.current
    var keyInput by remember(tmdbApiKey) { mutableStateOf(tmdbApiKey) }
    var showKey by remember { mutableStateOf(false) }
    val readmeText = remember {
        try {
            context.assets.open("README.md").bufferedReader().readText()
        } catch (e: Exception) {
            "README not found."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // API Key Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "TMDB API Key",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Required for real streaming availability data. Get your free key at themoviedb.org/settings/api",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("TMDB API Key") },
                    placeholder = { Text("Paste your key here") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = if (showKey) "Hide key" else "Show key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("settings_tmdb_api_key_input")
                )
                Button(
                    onClick = { onSaveTmdbApiKey(keyInput) },
                    modifier = Modifier.fillMaxWidth().testTag("settings_save_api_key"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save API Key")
                }
            }
        }

        // README Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "About & Setup Guide",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
                Text(
                    text = readmeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("settings_readme_text")
                )
            }
        }
    }
}
