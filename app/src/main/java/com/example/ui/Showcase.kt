package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.composed

data class ShowcaseTargetInfo(
    val bounds: Rect,
    val title: String,
    val whatItDoes: String,
    val whenToUseIt: String
)

class ShowcaseState {
    val targets = mutableStateMapOf<String, ShowcaseTargetInfo>()
    
    var isGuideModeActive by mutableStateOf(false)
    var selectedTargetId by mutableStateOf<String?>(null)
    
    fun register(id: String, bounds: Rect, title: String, whatItDoes: String, whenToUseIt: String) {
        targets[id] = ShowcaseTargetInfo(bounds, title, whatItDoes, whenToUseIt)
    }

    fun enableGuideMode() {
        isGuideModeActive = true
        selectedTargetId = null
    }

    fun disableGuideMode() {
        isGuideModeActive = false
        selectedTargetId = null
    }
}

val LocalShowcaseState = compositionLocalOf<ShowcaseState> { error("No ShowcaseState provided") }

fun Modifier.showcaseTarget(id: String, title: String, whatItDoes: String, whenToUseIt: String): Modifier = composed {
    val state = LocalShowcaseState.current
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInRoot()
        if (bounds.width > 0 && bounds.height > 0) {
            state.register(id, bounds, title, whatItDoes, whenToUseIt)
        }
    }
}

@Composable
fun ShowcaseOverlay() {
    val state = LocalShowcaseState.current
    if (!state.isGuideModeActive) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen) 
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Find if tap is within any registered target bounds
                    val hit = state.targets.entries.find { it.value.bounds.contains(offset) }
                    if (hit != null) {
                        state.selectedTargetId = hit.key
                    } else {
                        state.selectedTargetId = null
                    }
                }
            }
    ) {
        // Draw overlay with transparent holes and yellow glowing borders
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.75f))

            for (target in state.targets.values) {
                val padding = 12.dp.toPx()
                val targetRect = Rect(
                    left = target.bounds.left - padding,
                    top = target.bounds.top - padding,
                    right = target.bounds.right + padding,
                    bottom = target.bounds.bottom + padding
                )

                // Punch transparent hole
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(targetRect.left, targetRect.top),
                    size = Size(targetRect.width, targetRect.height),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
                
                // Draw highlight border
                drawRoundRect(
                    color = Color.Yellow,
                    topLeft = Offset(targetRect.left, targetRect.top),
                    size = Size(targetRect.width, targetRect.height),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Top bar to exit guide mode
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            ElevatedButton(
                onClick = { state.disableGuideMode() },
                colors = ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text("Exit Guide Mode", fontWeight = FontWeight.Bold)
            }
        }

        // Detailed Information Card Modal
        state.selectedTargetId?.let { id ->
            val info = state.targets[id] ?: return@let
            AlertDialog(
                onDismissRequest = { state.selectedTargetId = null },
                title = { Text(info.title, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("What it does", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(info.whatItDoes, style = MaterialTheme.typography.bodyMedium)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("When to use it", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(info.whenToUseIt, style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { state.selectedTargetId = null }) { Text("Got it") }
                }
            )
        }
    }
}
