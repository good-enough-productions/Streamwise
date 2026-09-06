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

## 4. Explore AI & Cinephile Insights Architecture

- **SynthesizedMovieInsights**: Automatically parses YAML frontmatter (`focus_topics`, `featured_cast`, `agent_synthesis_date`) and markdown bullet points into interactive topic pills, cast badges, and styled insight cards. Includes direct handoff action to Explore chat.
- **Cinephile Taste Matrix**: Multi-segment visual bar in the Explore tab calculating exact genre distributions from the user's offline SQLite watched history.
- **Gemini Pro Recommendations**: Integrates `GeminiClient` with user-managed API keys in Settings, providing 1-tap `+ Watchlist` addition that triggers immediate TMDB metadata and availability enrichment.

## 5. Autonomous Beta Feedback Pipeline (App -> Backlog & Jules)

- **FloatingFeedbackButton**: Material 3 floating action button rendered globally across all screens when `enableBetaFeedback` is enabled in `UserPreferencesManager`.
- **FeedbackDialog**: Captures on-device screenshots via `PixelCopy` alongside device telemetry (device model, Android OS version, active tab, watchlist and vault counts).
- **Backlog First & Jules AI Opt-In**: By default, feedback generates standard GitHub Issues labeled `feedback`, `streamwise`, and `[category]` for backlog tracking. When the user explicitly checks "Assign to Jules (Autonomous AI)", the `jules` and `jules-triage` labels are attached.
- **Secure Webhook**: Submits payload to the Cloud Run proxy (`https://feedback-proxy-rljydlcchq-uc.a.run.app`), keeping GitHub PATs off client devices.

## 6. Collapsible Spotlight & Advanced Multi-Dimensional Filters (Issues #7 & #8)

- **Collapsible Spotlight**: Persistent top carousel toggleable via animated chevron, freeing up screen real estate while retaining fast access to high-priority streaming titles.
- **AdvancedFilterBottomSheet**: Modal filter panel supporting:
  - Streaming Provider grid chips.
  - Multi-genre selector pills.
  - Minimum rating threshold chips (6.0+, 7.0+, 7.5+, 8.0+, 8.5+).
  - Release era filtering (2020s, 2010s, 2000s, 90s, Classic pre-1990).
  - Dynamic sort orders (Priority, Title A-Z, Rating High-Low, Release Year).
  - Quick-clear pill and active filter badges on the home screen toolbar.

## 7. Cinephile Vault, Diary Timeline & Letterboxd Sync (Issues #9 & #10)

- **Cinephile Vault Overview**: Real-time stats card displaying total logged films, estimated screen time hours, average rating, and top 4 favorite genres.
- **Monthly Diary Timeline**: Groups watched entries chronologically by Year-Month (`September 2026`, `August 2026`), displaying formatted watch dates (`Watched Sep 4, 2026`), user notes, and 1-tap re-watch triggers.
- **Poster Wall Grid**: View mode toggle between structured timeline diary list and compact visual poster grid (`WatchedGridPosterCard`).
- **1-Tap Letterboxd Google Sheet Sync**: Dispatches watched history and watchlist titles as structured JSON to a Google Apps Script webhook, following HTTP 302 redirects to mirror data into Google Drive / Google Sheets at $0/mo.

