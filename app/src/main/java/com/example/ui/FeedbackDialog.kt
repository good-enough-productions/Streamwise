package com.example.ui

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.PixelCopy
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Floating Feedback Button (FAB) + Modal Dialog.
 * Ports the autonomous feedback pipeline from Do It Now and Master Hub:
 * Captures screen screenshot, aggregates system diagnostics, and files a GitHub Issue
 * directly for Jules and Antigravity autonomous triage.
 */
@Composable
fun FloatingFeedbackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .testTag("floating_feedback_fab"),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = "Report Feedback / Issue",
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun FeedbackDialog(
    githubToken: String,
    webhookUrl: String,
    currentTabName: String,
    watchlistCount: Int,
    watchedCount: Int,
    onDismiss: () -> Unit,
    onSubmitSuccess: (String) -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var feedbackType by remember { mutableStateOf("Bug Report") } // "Bug Report", "Feature Request", "UI Tweak"
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var includeScreenshot by remember { mutableStateOf(true) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Capture screenshot on dialog open
    LaunchedEffect(Unit) {
        captureScreenBitmap(view) { bitmap ->
            capturedBitmap = bitmap
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("feedback_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
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
                            text = "Streamwise Feedback Loop",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Creates GitHub Issue · Auto-triaged by Jules",
                            style = MaterialTheme.typography.labelSmall,
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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Bug Report", "Feature Request", "UI Tweak").forEach { type ->
                            FilterChip(
                                selected = feedbackType == type,
                                onClick = { feedbackType = type },
                                label = { Text(type, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Issue Title") },
                        placeholder = { Text("Brief summary of bug or idea") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Details & Reproduction Steps") },
                        placeholder = { Text("Describe what happened or what you'd like to see...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("feedback_desc_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Screenshot Preview
                    if (capturedBitmap != null) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Attach Screenshot",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Switch(
                                    checked = includeScreenshot,
                                    onCheckedChange = { includeScreenshot = it }
                                )
                            }
                            if (includeScreenshot) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                ) {
                                    Image(
                                        bitmap = capturedBitmap!!.asImageBitmap(),
                                        contentDescription = "Screen Preview",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // Diagnostic telemetry summary
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Auto-Attached Diagnostics:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Screen: $currentTabName · Watchlist: $watchlistCount · Watched: $watchedCount · Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            isSubmitting = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = submitIssue(
                                    githubToken = githubToken,
                                    webhookUrl = webhookUrl,
                                    type = feedbackType,
                                    title = title.trim(),
                                    description = description.trim(),
                                    bitmap = if (includeScreenshot) capturedBitmap else null,
                                    tab = currentTabName,
                                    watchlistCount = watchlistCount,
                                    watchedCount = watchedCount
                                )
                                isSubmitting = false
                                if (result.isSuccess) {
                                    onSubmitSuccess(result.getOrNull() ?: "Issue created successfully!")
                                    onDismiss()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to submit issue"
                                }
                            }
                        }
                    },
                    enabled = title.isNotBlank() && !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feedback_submit_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dispatching to Jules...")
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create GitHub Issue", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun captureScreenBitmap(view: View, onCaptured: (Bitmap) -> Unit) {
    try {
        val window = (view.context as? Activity)?.window ?: return
        val bitmap = Bitmap.createBitmap(view.width.coerceAtLeast(100), view.height.coerceAtLeast(100), Bitmap.Config.ARGB_8888)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PixelCopy.request(window, bitmap, { result ->
                if (result == PixelCopy.SUCCESS) {
                    onCaptured(bitmap)
                }
            }, Handler(Looper.getMainLooper()))
        } else {
            onCaptured(bitmap)
        }
    } catch (e: Exception) {
        // Soft-fail screenshot
    }
}

private suspend fun submitIssue(
    githubToken: String,
    webhookUrl: String,
    type: String,
    title: String,
    description: String,
    bitmap: Bitmap?,
    tab: String,
    watchlistCount: Int,
    watchedCount: Int
): Result<String> = withContext(Dispatchers.IO) {
    try {
        val labelType = when (type) {
            "Bug Report" -> "bug"
            "Feature Request" -> "enhancement"
            else -> "ui"
        }

        val base64Img = bitmap?.let { bmp ->
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            "data:image/jpeg;base64," + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        }

        val contextBlock = """
            - **Type:** $type
            - **Active Screen:** $tab
            - **Watchlist Count:** $watchlistCount
            - **Watched History Count:** $watchedCount
            - **Device:** ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})
        """.trimIndent()

        // 1. Direct GitHub Issue submission if token present
        if (githubToken.isNotBlank()) {
            val body = buildString {
                appendLine(description)
                appendLine()
                appendLine("### Diagnostic Context")
                appendLine(contextBlock)
                if (base64Img != null) {
                    appendLine()
                    appendLine("*(Screenshot attached via device capture)*")
                }
                appendLine()
                appendLine("---")
                appendLine("*Auto-generated via Streamwise Do-It-Now Feedback Loop*")
            }

            val payload = JSONObject().apply {
                put("title", "[$type]: $title")
                put("body", body)
                put("labels", org.json.JSONArray(listOf("feedback", "streamwise", labelType, "jules-triage")))
            }

            val url = URL("https://api.github.com/repos/good-enough-productions/Streamwise/issues")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $githubToken")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = conn.responseCode
            if (code in 200..299) {
                return@withContext Result.success("GitHub Issue opened successfully!")
            } else {
                return@withContext Result.failure(Exception("GitHub API returned error code $code"))
            }
        }

        // 2. Apps Script Webhook fallback
        if (webhookUrl.isNotBlank()) {
            val payload = JSONObject().apply {
                put("action", "createFeedbackIssue")
                put("type", labelType)
                put("title", "[$type]: $title")
                put("description", description)
                put("labels", org.json.JSONArray(listOf("feedback", "streamwise", labelType)))
                put("screenshot", base64Img)
                put("context", JSONObject().apply {
                    put("tab", tab)
                    put("watchlistCount", watchlistCount)
                    put("watchedCount", watchedCount)
                    put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                })
            }

            val url = URL(webhookUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            if (conn.responseCode in 200..299) {
                return@withContext Result.success("Feedback submitted to Master Ledger & Jules!")
            }
        }

        Result.failure(Exception("Please configure GitHub Token or Webhook URL in Settings."))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
