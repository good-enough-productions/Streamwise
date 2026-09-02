package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MediaItem
import com.example.data.model.StreamingProvider
import java.text.SimpleDateFormat
import java.util.*

/**
 * QuickLogDialog: Frictionless couch-side movie logger with Letterboxd & ROI parity.
 * Bypasses the watchlist and lets you log a film in seconds.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickLogDialog(
    initialMovie: MediaItem? = null,
    allProviders: List<StreamingProvider>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        year: String?,
        rating: Double?,
        isRewatch: Boolean,
        providerId: String?,
        durationMinutes: Int,
        notes: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialMovie?.title ?: "") }
    var year by remember { mutableStateOf(initialMovie?.releaseYear ?: "") }
    var userRating by remember { mutableStateOf(initialMovie?.userRating ?: 4.0) }
    var isRewatch by remember { mutableStateOf(initialMovie?.isRewatch ?: false) }
    var selectedProviderId by remember {
        mutableStateOf(initialMovie?.providersList?.firstOrNull() ?: allProviders.firstOrNull { it.isActive }?.id)
    }
    var durationMinutes by remember { mutableStateOf(initialMovie?.runtimeMinutes ?: 120) }
    var notes by remember { mutableStateOf(initialMovie?.userNotes ?: "") }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("quick_log_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialMovie != null) "Log Watched Film" else "Quick Log Watched",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Logged on $todayStr · Staged for Letterboxd",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title & Year
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Movie Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_log_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it },
                            label = { Text("Year") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = durationMinutes.toString(),
                            onValueChange = { durationMinutes = it.toIntOrNull() ?: 120 },
                            label = { Text("Runtime (mins)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Rating Selector (0.5 to 5.0 stars)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Your Rating",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${"★".repeat(userRating.toInt())}${if (userRating % 1.0 >= 0.5) "½" else ""} ($userRating / 5.0)",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFFFB800),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Star Clickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(1.0, 2.0, 3.0, 4.0, 5.0).forEach { starIndex ->
                                IconButton(
                                    onClick = { userRating = starIndex },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "$starIndex stars",
                                        tint = if (userRating >= starIndex) Color(0xFFFFB800) else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        // Half-star modifier row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf(0.5, 1.5, 2.5, 3.5, 4.5).forEach { halfRating ->
                                FilterChip(
                                    selected = userRating == halfRating,
                                    onClick = { userRating = halfRating },
                                    label = { Text("$halfRating★", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Rewatch Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRewatch = !isRewatch }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Rewatch", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Check if you have seen this film before", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = isRewatch,
                            onCheckedChange = { isRewatch = it },
                            modifier = Modifier.testTag("quick_log_rewatch_switch")
                        )
                    }

                    // Provider Selector (For ROI Tracking)
                    Column {
                        Text(
                            "Where did you watch it?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Credits duration toward this subscription's monthly ROI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allProviders.forEach { provider ->
                                FilterChip(
                                    selected = selectedProviderId == provider.id,
                                    onClick = { selectedProviderId = provider.id },
                                    label = { Text(provider.name) },
                                    leadingIcon = if (selectedProviderId == provider.id) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                            FilterChip(
                                selected = selectedProviderId == "theater",
                                onClick = { selectedProviderId = "theater" },
                                label = { Text("Movie Theater") }
                            )
                            FilterChip(
                                selected = selectedProviderId == "physical",
                                onClick = { selectedProviderId = "physical" },
                                label = { Text("Blu-ray / Disc") }
                            )
                        }
                    }

                    // Notes / Review
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Review / Personal Notes") },
                        placeholder = { Text("What made this stick with you?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    title.trim(),
                                    year.ifBlank { null },
                                    userRating,
                                    isRewatch,
                                    selectedProviderId,
                                    durationMinutes,
                                    notes.ifBlank { null }
                                )
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("quick_log_save_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Save & Log", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
