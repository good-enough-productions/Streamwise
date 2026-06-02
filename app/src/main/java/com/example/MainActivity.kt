package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.HomeScreen
import com.example.ui.StreamViewModel
import com.example.ui.StreamViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    // Instantiate Viewmodel using Constructor Injection via modern factory linked to our AppContainer
    private val viewModel: StreamViewModel by viewModels {
        val container = (application as StreamApp).container
        StreamViewModelFactory(application, container.mediaRepository, container.userPreferences)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle incoming shared context (Chrome, IMDB, Letterboxd, Safari etc) on cold launch
        handleShareIntent(intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HomeScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    simulateForegroundReturn = {
                        // Expose manually for easy emulator triggering if background resume delay occurs!
                        Log.d(TAG, "Manual foreground check triggered")
                        viewModel.checkForIntendingToWatchOnResume()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Core Habit Loop Requirement 3:
        // Automatically check Room database for "INTENDING_TO_WATCH" items when the app returns to the foreground.
        Log.d(TAG, "App returned to foreground: Executing check-in watcher loop and immediate sync.")
        viewModel.checkForIntendingToWatchOnResume()
        
        // Zero-Touch Automation: Trigger immediate TMDB/Ollama sync on every resume to ensure data is fresh
        viewModel.triggerImmediateSync()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle incoming shared context when activity is already warm in the backstack
        handleShareIntent(intent)
    }

    /**
     * Parsing mechanism to handle ACTION_SEND intents from Chrome or IMDB.
     */
    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                if (!sharedText.isNullOrBlank()) {
                    Log.d(TAG, "Received shared plain text / url: $sharedText")
                    viewModel.handleAddSharedContent(sharedText)
                    
                    // Consume intent so we don't re-process on screen rotates
                    intent.removeExtra(Intent.EXTRA_TEXT)
                    intent.removeExtra(Intent.EXTRA_SUBJECT)
                    intent.action = null
                }
            }
        }
    }
}
