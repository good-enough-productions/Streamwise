package com.example.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun GuideDialog(onDismiss: () -> Unit, onStartTour: () -> Unit) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: User Guide, 1: Changelog

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Help & Documentation", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close Guide")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("User Guide") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Changelog") }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                val url = if (activeTab == 0) "file:///android_asset/user_guide.html" else "file:///android_asset/changelog.html"
                
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = false // safe for static docs
                            loadUrl(url)
                        }
                    },
                    update = { webView ->
                        webView.loadUrl(url)
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                onStartTour()
                onDismiss() 
            }) {
                Text("Start Interactive Tour")
            }
        }
    )
}
