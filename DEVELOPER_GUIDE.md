# Streamwise Developer Guide

This guide details the architectural design, local build setup, and synchronization patterns for Streamwise.

## 1. Architecture Overview

- **UI Layer**: Jetpack Compose with Material 3 theming (`com.example.ui`).
- **ViewModel**: `StreamViewModel.kt` managing UI state, live reactive Flows, and direct coroutine metadata sync via `viewModelScope`.
- **Database**: Room database (`AppDatabase.kt`) caching titles (`MediaItem`), providers, and watch history offline.
- **Remote APIs**:
  - `TmdbApiService.kt`: REST endpoints for TMDB metadata and JustWatch/TMDB watch providers.
  - `AvailabilitySyncWorker.kt`: Android WorkManager worker for periodic background availability scans.
- **Environment & Secrets**: Loaded via Gradle Secrets Plugin from `.env` in `apps/streamwise/` (`TMDB_API_KEY`, `GEMINI_API_KEY`).

## 2. Local Build & Test

### Prerequisites
- JDK 17
- Android SDK (API 34)

### Build Commands
```bash
cd apps/streamwise
./gradlew assembleDebug
```

### Wireless ADB Deployment
```powershell
adb devices -l
adb -s <phone_serial> install -r -d app/build/outputs/apk/debug/app-debug.apk
adb -s <phone_serial> shell am start -n com.aistudio.streammanager.qpwoei/com.example.MainActivity
```

## 3. WorkManager vs. ViewModel Sync Rules

- **Periodic Sync**: Uses `AvailabilitySyncWorker.WORK_NAME` scheduled via `enqueueUniquePeriodicWork` every 4 hours.
- **Manual/Immediate Sync**: Uses `AvailabilitySyncWorker.ONE_TIME_WORK_NAME` with `ExistingWorkPolicy.REPLACE`. Never share the unique work name between periodic and one-time tasks.
