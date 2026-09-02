package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StreamingProvider
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bottom Sheet for seamlessly configuring subscription amounts, active state, and trial duration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionEditSheet(
    provider: StreamingProvider,
    onDismiss: () -> Unit,
    onSave: (providerId: String, isActive: Boolean, cost: Double, startDate: Long?, trialEndDate: Long?) -> Unit,
    onDelete: ((providerId: String) -> Unit)? = null
) {
    var isActive by remember(provider) { mutableStateOf(provider.isActive) }
    val initialCost = provider.userCostPerMonth ?: provider.costPerMonth
    var costText by remember(provider) { mutableStateOf(String.format(Locale.US, "%.2f", initialCost)) }
    
    var isTrial by remember(provider) { mutableStateOf(provider.trialEndDate != null && provider.trialEndDate > System.currentTimeMillis()) }
    var trialEndDate by remember(provider) { mutableStateOf(provider.trialEndDate) }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val pricePresets = listOf(0.00, 5.99, 7.99, 9.99, 13.99, 15.49, 19.99, 22.99)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("subscription_edit_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Edit monthly billing & trial details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (onDelete != null) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Service",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 1. Active Subscription Toggle
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isActive) "Currently Subscribed (Active)" else "Subscription Paused (Inactive)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (isActive) "Included in your monthly burn rate and filter chips"
                                   else "Excluded from burn rate; $0.00/mo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.testTag("sub_active_switch")
                    )
                }
            }

            // 2. Monthly Cost Input & Presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Monthly Amount Paid",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Cost per Month ($)") },
                    prefix = { Text("$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sub_cost_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick Presets
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pricePresets.take(4).forEach { preset ->
                        AssistChip(
                            onClick = { costText = String.format(Locale.US, "%.2f", preset) },
                            label = { Text(if (preset == 0.0) "Free" else "$${String.format(Locale.US, "%.2f", preset)}") }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pricePresets.drop(4).forEach { preset ->
                        AssistChip(
                            onClick = { costText = String.format(Locale.US, "%.2f", preset) },
                            label = { Text("$${String.format(Locale.US, "%.2f", preset)}") }
                        )
                    }
                }
            }

            // 3. Free Trial / Promo Configuration
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTrial) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Free Trial / Promo Period",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Auto-deactivates when trial expires so you don't get billed accidentally",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = isTrial,
                            onCheckedChange = {
                                isTrial = it
                                if (it && trialEndDate == null) {
                                    trialEndDate = System.currentTimeMillis() + (7 * 86400000L) // Default 7 days
                                } else if (!it) {
                                    trialEndDate = null
                                }
                            }
                        )
                    }

                    if (isTrial) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        val now = System.currentTimeMillis()
                        val daysRemaining = trialEndDate?.let { ((it - now) / 86400000L).coerceAtLeast(0) } ?: 0
                        val formattedDate = trialEndDate?.let { dateFormat.format(Date(it)) } ?: "Not set"

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Text(
                                text = "Trial expires on $formattedDate ($daysRemaining days remaining)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Text(
                            text = "Set Trial Length:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(7 to "7 Days", 14 to "14 Days", 30 to "30 Days", 90 to "3 Months").forEach { (days, label) ->
                                val isSelected = trialEndDate?.let {
                                    val diff = (it - now) / 86400000L
                                    diff in (days - 1)..(days + 1)
                                } ?: false

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        trialEndDate = now + (days * 86400000L)
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Action Buttons (Save / Cancel)
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
                        val parsedCost = costText.toDoubleOrNull() ?: initialCost
                        val finalTrialEnd = if (isTrial) trialEndDate else null
                        val startDate = provider.subscriptionStartDate ?: System.currentTimeMillis()
                        onSave(provider.id, isActive, parsedCost, startDate, finalTrialEnd)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sub_save_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove ${provider.name}?") },
            text = { Text("Are you sure you want to remove this service from your subscription list? Any watch history with this service will be preserved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(provider.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
