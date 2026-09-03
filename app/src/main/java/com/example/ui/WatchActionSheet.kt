package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.model.StreamingProvider

/**
 * WatchActionSheet: Reworked "Watch Now" action hub.
 * 1. Play on Smart TV / Fire TV (always visible with auto-detect and 1-tap manual connect)
 * 2. Watch on Phone (Direct deep link)
 * 3. Cast / Open in External App (Google Cast / System Chooser)
 * 4. Log as Watched (0.5-5.0 rating, rewatch, ROI duration)
 * 5. Pin as Tonight's Feature
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchActionSheet(
    item: MediaItem,
    allProviders: List<StreamingProvider>,
    fireTvIp: String,
    discoveredDevices: List<CastDevice> = emptyList(),
    onDismiss: () -> Unit,
    onLaunchFireTv: (MediaItem, String?, String) -> Unit, // passes (item, providerId, targetIp)
    onLaunchPhone: (MediaItem, String?) -> Unit,
    onUniversalCast: (MediaItem, String?) -> Unit,
    onQuickLog: (MediaItem) -> Unit,
    onPinTonight: (MediaItem) -> Unit,
    onSaveFireTvIp: (String) -> Unit = {},
    onScanDevices: () -> Unit = {}
) {
    val activeItemProviders = allProviders.filter { item.providersList.contains(it.id) }
    val primaryProvider = activeItemProviders.firstOrNull { it.isActive } ?: activeItemProviders.firstOrNull()

    // Auto-detect target TV: explicit setting IP > discovered Fire TV > any discovered smart TV
    val autoTv = discoveredDevices.firstOrNull { it.type == "FireTV" || it.name.contains("Fire", ignoreCase = true) }
        ?: discoveredDevices.firstOrNull()
    val effectiveTvIp = fireTvIp.ifBlank { autoTv?.ip ?: "" }
    val tvDisplayName = when {
        autoTv != null -> autoTv.name
        fireTvIp.isNotBlank() -> "Fire TV ($fireTvIp)"
        else -> null
    }

    var showConnectTvDialog by remember { mutableStateOf(false) }
    var manualIpInput by remember { mutableStateOf(effectiveTvIp) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("watch_action_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Movie Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${item.releaseYear ?: ""} · Available on: ${activeItemProviders.joinToString { it.name }.ifEmpty { "Online / Cable" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Action 1: TV Launch (ALWAYS VISIBLE!)
            Card(
                onClick = { 
                    if (effectiveTvIp.isNotBlank()) {
                        onLaunchFireTv(item, primaryProvider?.id, effectiveTvIp)
                    } else {
                        onScanDevices()
                        showConnectTvDialog = true
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("watch_sheet_fire_tv_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (effectiveTvIp.isNotBlank()) "Play on ${tvDisplayName ?: "TV ($effectiveTvIp)"}"
                                       else "Play on Smart TV / Fire TV",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Text(
                            text = if (effectiveTvIp.isNotBlank()) "Wakes TV & opens ${primaryProvider?.name ?: "app"} in 4K HDR · Starts timer"
                                   else "Connect your TV over Wi-Fi to launch automatically",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (effectiveTvIp.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onScanDevices()
                                showConnectTvDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Change TV",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Action 2: Phone Deep-link Launch
            Card(
                onClick = { onLaunchPhone(item, primaryProvider?.id) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("watch_sheet_phone_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Watch on this Device",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Deep-links directly into ${primaryProvider?.name ?: "streaming app or browser"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Action 3: Universal Cast / Open with System Chooser
            Card(
                onClick = { onUniversalCast(item, primaryProvider?.id) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cast or Open with App…",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Choose installed streaming app, Google Cast, or external player",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Action 4: Quick Log as Watched
            Card(
                onClick = { onQuickLog(item) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("watch_sheet_log_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Already Watched (Log Now)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Rate it (0.5–5.0★), record ROI duration, and stage for Letterboxd",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Action 5: Pin as Tonight's Feature
            OutlinedButton(
                onClick = { onPinTonight(item) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pin as Tonight's Feature")
            }
        }
    }

    // Connect TV Dialog (Auto-discovery list + Manual IP Entry)
    if (showConnectTvDialog) {
        AlertDialog(
            onDismissRequest = { showConnectTvDialog = false },
            title = {
                Text("Connect Your TV", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Streamwise triggers movies directly on your Fire TV, Android TV, or Smart TV over your home Wi-Fi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Wi-Fi Discovery Scan Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Discovered on Wi-Fi:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = onScanDevices,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan", fontSize = 12.sp)
                        }
                    }

                    if (discoveredDevices.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            discoveredDevices.forEach { device ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            manualIpInput = device.ip
                                            onSaveFireTvIp(device.ip)
                                            showConnectTvDialog = false
                                            onLaunchFireTv(item, primaryProvider?.id, device.ip)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(device.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text(device.ip, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Text("Select & Play", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "No TVs auto-discovered yet. Tap 'Scan' or enter IP below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Manual IP Input
                    OutlinedTextField(
                        value = manualIpInput,
                        onValueChange = { manualIpInput = it },
                        label = { Text("TV IP Address") },
                        placeholder = { Text("e.g. 192.168.1.105") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "Tip: On Fire TV, check Settings > My Fire TV > About > Network to view your IP.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ip = manualIpInput.trim()
                        if (ip.isNotBlank()) {
                            onSaveFireTvIp(ip)
                            showConnectTvDialog = false
                            onLaunchFireTv(item, primaryProvider?.id, ip)
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save & Play Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectTvDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
