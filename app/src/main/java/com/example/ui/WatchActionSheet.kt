package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
 * Replaces the silent database flag with actionable playback & logging choices:
 * 1. Play on Fire TV natively
 * 2. Watch on Phone
 * 3. Log as Watched (Quick Check-in)
 * 4. Pin as Tonight's Feature
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
    onQuickLog: (MediaItem) -> Unit,
    onPinTonight: (MediaItem) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            // Action 1: TV Launch (Highlighted if TV detected or configured)
            if (effectiveTvIp.isNotBlank()) {
                Card(
                    onClick = { onLaunchFireTv(item, primaryProvider?.id, effectiveTvIp) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
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
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Play on ${tvDisplayName ?: "Fire TV"}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Wakes TV & opens ${primaryProvider?.name ?: "app"} in 4K HDR · Starts watch session timer",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Action 2: Phone Deep-link Launch (Promoted to primary if away from TV)
            Card(
                onClick = { onLaunchPhone(item, primaryProvider?.id) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (effectiveTvIp.isBlank()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                            text = "Deep-links directly into ${primaryProvider?.name ?: "app or browser"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Action 3: Quick Log as Watched
            Card(
                onClick = { onQuickLog(item) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                            text = "Rate it, record ROI duration, and stage for Letterboxd",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Action 4: Pin as Tonight's Feature
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
}
