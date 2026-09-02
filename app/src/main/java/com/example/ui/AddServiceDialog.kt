package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Dialog for quickly adding a new streaming channel or subscription.
 */
@Composable
fun AddServiceDialog(
    existingProviderNames: List<String>,
    onDismiss: () -> Unit,
    onAdd: (name: String, cost: Double, isTrial: Boolean, trialDays: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("9.99") }
    var isTrial by remember { mutableStateOf(false) }
    var trialDays by remember { mutableStateOf(7) }

    val popularSuggestions = listOf(
        "Peacock" to 7.99,
        "Paramount+" to 7.99,
        "Criterion Channel" to 10.99,
        "MUBI" to 14.99,
        "Shudder" to 6.99,
        "Starz" to 9.99,
        "YouTube Premium" to 13.99,
        "BritBox" to 8.99,
        "F1 TV" to 9.99
    ).filter { suggestion -> existingProviderNames.none { it.equals(suggestion.first, ignoreCase = true) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Streaming Service",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Service Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name") },
                    placeholder = { Text("e.g. Criterion Channel, Peacock") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_service_name_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick Popular Suggestions
                if (popularSuggestions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Popular Suggestions:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            popularSuggestions.take(3).forEach { (suggestedName, defaultCost) ->
                                AssistChip(
                                    onClick = {
                                        name = suggestedName
                                        costText = String.format(Locale.US, "%.2f", defaultCost)
                                    },
                                    label = { Text(suggestedName) }
                                )
                            }
                        }
                        if (popularSuggestions.size > 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                popularSuggestions.drop(3).take(3).forEach { (suggestedName, defaultCost) ->
                                    AssistChip(
                                        onClick = {
                                            name = suggestedName
                                            costText = String.format(Locale.US, "%.2f", defaultCost)
                                        },
                                        label = { Text(suggestedName) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Monthly Cost Input
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Monthly Cost ($)") },
                    prefix = { Text("$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_service_cost_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Free Trial Section
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTrial) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                         else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Currently on Free Trial?",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = isTrial,
                                onCheckedChange = { isTrial = it }
                            )
                        }

                        if (isTrial) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(7 to "7d", 14 to "14d", 30 to "30d", 90 to "3mo").forEach { (days, label) ->
                                    FilterChip(
                                        selected = trialDays == days,
                                        onClick = { trialDays = days },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val cost = costText.toDoubleOrNull() ?: 0.0
                        onAdd(name.trim(), cost, isTrial, trialDays)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_service_confirm_btn")
            ) {
                Text("Add Service", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
