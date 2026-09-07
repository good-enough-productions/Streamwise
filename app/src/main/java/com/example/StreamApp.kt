package com.example

import android.app.Application
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesManager
import com.example.data.repository.MediaRepository
import com.example.data.worker.AvailabilitySyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit

/**
 * Dependency container for manual Dependency Injection (Constructor Injection).
 * Keeps scope clear and makes transitioning to Hilt as simple as adding standard annotations later.
 */
class AppContainer(private val context: Application, private val scope: CoroutineScope) {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(context, scope) }
    val mediaRepository: MediaRepository by lazy { MediaRepository(database.mediaDao()) }
    val userPreferences: UserPreferencesManager by lazy { UserPreferencesManager(context) }
}

class StreamApp : Application() {

    // Lifecycle scope bound to Application lifecycle for database callback and background worker setups.
    private val applicationScope = CoroutineScope(SupervisorJob())

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()

        // Configure CursorWindow size to prevent CursorWindowAllocationException with large datasets
        try {
            val field = android.database.CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
            field.isAccessible = true
            field.set(null, 50 * 1024 * 1024) // 50MB
        } catch (e: Throwable) {
            android.util.Log.w("StreamApp", "Could not set sCursorWindowSize: ${e.message}")
        }
        
        // Setup Dependency Injection Container
        container = AppContainer(this, applicationScope)

        // Queue WorkManager Periodic Streaming Provider Availability Sync
        setupPeriodicAvailabilitySync()
    }

    private fun setupPeriodicAvailabilitySync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        // Runs every 4 hours to periodically refresh providers, rate-limit optimized internally.
        val periodicSyncRequest = PeriodicWorkRequestBuilder<AvailabilitySyncWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        try {
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                AvailabilitySyncWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP, // Retains existing job to avoid resetting timer
                periodicSyncRequest
            )
        } catch (e: IllegalStateException) {
            // Catches initialization failures gracefully during local Robolectric/unit testing
            android.util.Log.w("StreamApp", "WorkManager not initialized. Skipping background task queue.")
        }
    }
}
