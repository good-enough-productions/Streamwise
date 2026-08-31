package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.composed

data class ShowcaseTargetInfo(
    val bounds: Rect,
    val title: String,
    val description: String
)

class ShowcaseState {
    val targets = mutableStateMapOf<String, ShowcaseTargetInfo>()
    var currentTargetId by mutableStateOf<String?>(null)
    
    private var sequence = listOf<String>()
    private var sequenceIndex = 0
    
    fun register(id: String, bounds: Rect, title: String, description: String) {
        targets[id] = ShowcaseTargetInfo(bounds, title, description)
    }

    fun startSequence(vararg ids: String) {
        sequence = ids.toList()
        sequenceIndex = 0
        currentTargetId = sequence.firstOrNull()
    }

    fun next() {
        sequenceIndex++
        if (sequenceIndex < sequence.size) {
            currentTargetId = sequence[sequenceIndex]
        } else {
            currentTargetId = null
        }
    }
    
    fun skip() {
        currentTargetId = null
    }
}

val LocalShowcaseState = compositionLocalOf<ShowcaseState> { error("No ShowcaseState provided") }

fun Modifier.showcaseTarget(id: String, title: String, description: String): Modifier = composed {
    val state = LocalShowcaseState.current
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInRoot()
        if (bounds.width > 0 && bounds.height > 0) {
            state.register(id, bounds, title, description)
        }
    }
}

@Composable
fun ShowcaseOverlay() {
    val state = LocalShowcaseState.current
    val currentId = state.currentTargetId ?: return
    val targetInfo = state.targets[currentId] ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen) 
            .clickable { state.next() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.7f))

            val padding = 16.dp.toPx()
            val paddedBounds = Rect(
                left = targetInfo.bounds.left - padding,
                top = targetInfo.bounds.top - padding,
                right = targetInfo.bounds.right + padding,
                bottom = targetInfo.bounds.bottom + padding
            )
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(paddedBounds.left, paddedBounds.top),
                size = androidx.compose.ui.geometry.Size(paddedBounds.width, paddedBounds.height),
                cornerRadius = CornerRadius(24.dp.toPx()),
                blendMode = BlendMode.Clear
            )
        }

        val isTopHalf = targetInfo.bounds.center.y < (LocalContext.current.resources.displayMetrics.heightPixels / 2)
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = if (isTopHalf) Alignment.BottomCenter else Alignment.TopCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(
                    top = if (!isTopHalf) (targetInfo.bounds.bottom / LocalContext.current.resources.displayMetrics.density).dp + 32.dp else 0.dp,
                    bottom = if (isTopHalf) ((LocalContext.current.resources.displayMetrics.heightPixels - targetInfo.bounds.top) / LocalContext.current.resources.displayMetrics.density).dp + 32.dp else 0.dp
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = targetInfo.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = targetInfo.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { state.skip() }) { Text("Skip Tour") }
                        Button(onClick = { state.next() }) { Text("Next") }
                    }
                }
            }
        }
    }
}
