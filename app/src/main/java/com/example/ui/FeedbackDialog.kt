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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Floating Feedback Button (FAB) + Modal Dialog.
 * Powers the autonomous Jules feedback loop defined in project architecture:
 * Captures screen screenshot, aggregates system diagnostics, and files a GitHub Issue
 * directly for Jules and Antigravity autonomous triage.
 */
@Composable
fun FloatingFeedbackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .testTag("floating_feedback_fab"),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Feedback,
            contentDescription = "Report Feedback / Issue to Jules",
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun FeedbackDialog(
    githubToken: String,
    currentTabName: String,
    watchlistCount: Int,
    watchedCount: Int,
    onDismiss: () -> Unit,
    onSubmitSuccess: (String) -> Unit
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    var feedbackType by remember { mutableStateOf("Bug Report") } // "Bug Report", "Feature Request", "UI / UX"
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
                        listOf("Bug Report", "Feature Request", "UI / UX").forEach { type ->
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
                        label = { Text("Details & Context") },
                        placeholder = { Text("Describe what happened or what you'd like improved...") },
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
                                    text = "Screen Capture",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (includeScreenshot) "Attached" else "Excluded",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (includeScreenshot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        checked = includeScreenshot,
                                        onCheckedChange = { includeScreenshot = it }
                                    )
                                }
                            }

                            if (includeScreenshot) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Captured Screen Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                )
                            }
                        }
                    }

                    // Diagnostic info preview
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "DIAGNOSTIC CONTEXT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Screen: $currentTabName • Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}) • Queue: $watchlistCount items • Vault: $watchedCount watched",
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
            - **Watched Vault Count:** $watchedCount
            - **Device:** ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})
        """.trimIndent()

        val fullBody = buildString {
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
            appendLine("*Auto-generated via Streamwise Jules Feedback Loop*")
        }

        // 1. Direct GitHub Issue submission if token present
        if (githubToken.isNotBlank()) {
            val payload = JSONObject().apply {
                put("title", "[$type]: $title")
                put("body", fullBody)
                put("labels", JSONArray(listOf("feedback", "streamwise", labelType, "jules", "jules-triage")))
            }

            val url = URL("https://api.github.com/repos/good-enough-productions/Streamwise/issues")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $githubToken")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
            }

            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = conn.responseCode
            if (code in 200..299) {
                return@withContext Result.success("GitHub Issue created and assigned to Jules!")
            }
        }

        // 2. Central Shared Feedback Proxy Cloud Function (No client PAT required)
        val proxyUrl = "https://us-central1-ai-assistant-438903.cloudfunctions.net/submitFeedback"
        val proxyPayload = JSONObject().apply {
            put("repo", "Streamwise")
            put("title", "[$type]: $title")
            put("body", fullBody)
            if (base64Img != null) {
                put("imageBase64", base64Img)
            }
        }

        val conn = (URL(proxyUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }

        conn.outputStream.use { it.write(proxyPayload.toString().toByteArray()) }
        val code = conn.responseCode
        if (code in 200..299) {
            return@withContext Result.success("Feedback submitted to Jules for triage!")
        } else {
            return@withContext Result.failure(Exception("Submission returned HTTP $code. Check connection or enter GitHub Token in Settings."))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
