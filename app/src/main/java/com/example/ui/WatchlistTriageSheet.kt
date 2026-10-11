package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.data.model.StreamingProvider

/**
 * Watchlist Backlog Triage Bottom Sheet
 *
 * Specifically designed to tame huge 1,000+ movie backlogs.
 * Presents unstarred movies one-by-one with rapid 1-tap decisions:
 * - ⭐ Focus Queue (Star as High Priority)
 * - ✓ Already Watched (Log to Watched Diary)
 * - ✕ Not Interested (Remove from Watchlist)
 * - ⏩ Skip / Next (Leave in Backlog)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistTriageSheet(
    unstarredItems: List<MediaItem>,
    allProviders: List<StreamingProvider>,
    onToggleStar: (MediaItem) -> Unit,
    onMarkWatched: (MediaItem) -> Unit,
    onRemoveFromQueue: (MediaItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableStateOf(0) }
    var starredCount by remember { mutableStateOf(0) }
    var watchedCount by remember { mutableStateOf(0) }
    var removedCount by remember { mutableStateOf(0) }

    val activeProviderMap = remember(allProviders) {
        allProviders.associateBy { it.id }
    }

    val currentItem = unstarredItems.getOrNull(currentIndex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("watchlist_triage_sheet"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚡", fontSize = 20.sp)
                    Text(
                        "Backlog Triage Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        if (unstarredItems.isNotEmpty()) "${(currentIndex + 1).coerceAtMost(unstarredItems.size)} of ${unstarredItems.size}" else "0 of 0",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (currentItem != null) {
                // Movie Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Poster Artwork
                        val posterUrl = currentItem.imageUrl
                        if (!posterUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = if (posterUrl.startsWith("http")) posterUrl else "https://image.tmdb.org/t/p/w342$posterUrl",
                                contentDescription = currentItem.title,
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(135.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(135.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🎬", fontSize = 32.sp)
                                }
                            }
                        }

                        // Title & Details
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                currentItem.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Year & Rating
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (currentItem.releaseYear != null) {
                                    Text(
                                        "${currentItem.releaseYear}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if ((currentItem.rating ?: 0.0) > 0.0) {
                                    Text(
                                        "★ ${currentItem.rating}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB300)
                                    )
                                }
                            }

                            // Genres
                            if (!currentItem.genres.isNullOrBlank()) {
                                Text(
                                    currentItem.genres,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Streaming Availability
                            val providers = currentItem.providersList
                            if (providers.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    providers.take(3).forEach { pId ->
                                        val p = activeProviderMap[pId]
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (p?.isActive == true) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                p?.name ?: pId.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (p?.isActive == true) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4 Rapid Triage Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button 1: Add to Focus Queue (Star Priority)
                    Button(
                        onClick = {
                            onToggleStar(currentItem)
                            starredCount++
                            currentIndex++
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Focus Queue (⭐ Priority)", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Button 2: Mark Watched
                        OutlinedButton(
                            onClick = {
                                onMarkWatched(currentItem)
                                watchedCount++
                                currentIndex++
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Watched ✓", fontSize = 12.sp)
                        }

                        // Button 3: Remove
                        OutlinedButton(
                            onClick = {
                                onRemoveFromQueue(currentItem)
                                removedCount++
                                currentIndex++
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove", fontSize = 12.sp)
                        }
                    }

                    // Button 4: Skip / Next
                    TextButton(
                        onClick = { currentIndex++ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Keep in Backlog & Skip →", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                // Done state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🎉", fontSize = 48.sp)
                    Text("Triage Session Complete!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "You added $starredCount movies to your Focus Queue, logged $watchedCount as watched, and removed $removedCount from your backlog.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Return to Watchlist")
                    }
                }
            }
        }
    }
}
