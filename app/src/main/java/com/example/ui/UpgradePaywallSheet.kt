package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradePaywallSheet(
    isCurrentPro: Boolean,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    onDowngrade: () -> Unit = {}
) {
    var isAnnualSelected by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("upgrade_paywall_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Close / Dismiss icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Pro Star Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFF8C00)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Pro Badge",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Headline
            Text(
                text = if (isCurrentPro) "You Are a Pro Subscriber" else "Unlock Streamwise Pro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Track unlimited subscriptions, automate Letterboxd sync, log TV shows, and never overpay for streaming services again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Plan Billing Selector
            if (!isCurrentPro) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Annual
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isAnnualSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { isAnnualSelected = true }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Annual",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAnnualSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = if (isAnnualSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "SAVE 17%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "$39.99/year ($3.33/mo)",
                                fontSize = 12.sp,
                                color = if (isAnnualSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Monthly
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (!isAnnualSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { isAnnualSelected = false }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Monthly",
                                fontWeight = FontWeight.Bold,
                                color = if (!isAnnualSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$3.99/month",
                                fontSize = 12.sp,
                                color = if (!isAnnualSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Feature List Matrix
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProFeatureRow("Unlimited Streaming Subscriptions", "Free tier capped at 2 active services")
                    ProFeatureRow("1-Tap Letterboxd Watchlist Live Sync", "Scrapes full account watchlist in seconds")
                    ProFeatureRow("Full TV Shows Tracking", "Seasons, episodes, and streaming availability")
                    ProFeatureRow("Film Podcast Recommendations Feed", "Curated recs from The Big Picture & Rewatchables")
                    ProFeatureRow("Renewal Radar Auto-Pause Links", "Direct deep links to cancel or pause unused apps")
                    ProFeatureRow("Olivia AI Research Agent", "Powered by cloud-native Gemini 2.0 Flash")
                    ProFeatureRow("Availability Push Alerts", "Instant alerts when watchlist titles become free to you")
                }
            }

            // CTA Button
            if (!isCurrentPro) {
                Button(
                    onClick = {
                        onUpgrade()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("upgrade_confirm_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isAnnualSelected) "Start 7-Day Free Trial ($39.99/yr)" else "Start 7-Day Free Trial ($3.99/mo)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {
                        onDowngrade()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("downgrade_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Revert to Free Tier (Demo/Testing)")
                }
            }

            // Small footer note
            Text(
                text = "Cancel anytime in Google Play Store settings. No commitment.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProFeatureRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
