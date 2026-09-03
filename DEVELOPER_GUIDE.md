# Streamwise Developer Guide

## Architecture Overview
Streamwise is a native Android application built entirely with **Kotlin** and **Jetpack Compose**. It aims to solve the "subscription fatigue" problem by combining a Watchlist manager with a Financial ROI tracker and a forward-looking Churn Optimizer.

### Key Components

1. **Local Database (Room Schema v8)**
   - **AppDatabase.kt**: SQLite database powered by Android Room with migration path `MIGRATION_7_8`.
   - **Entities**:
     - `MediaItem`: Represents a movie or TV show. Schema v8 adds `userRating` (0.5–5.0), `isRewatch`, `letterboxdUri`, `syncedToSheet`, `runtimeMinutes`, and `releaseYear`.
     - `StreamingProvider`: Represents a platform (Netflix, Hulu, Criterion, etc.). Custom user pricing, trial durations, and active states are stored here to drive ROI math.
     - `WatchSession`: Every time an item is checked off or watched on TV, a session is logged to track hours spent on a given platform.
   - **MediaDao.kt**: Contains Room queries, monthly usage aggregations (`getMonthlyUsageStats`), and custom provider deletions.

2. **Network, TV Discovery & Cloud Sync**
   - **TMDB & Watchmode**: Resolves title metadata and streaming availability across major and FAST providers in `AvailabilitySyncWorker.kt`.
   - **SSDP / DIAL TV Discovery**: `CastingManager.kt` sweeps the local Wi-Fi subnet on startup, identifies Fire TVs / Smart TVs via XML device descriptors, and provides zero-config IP binding.
   - **Watch Hub & TV Connect**: `WatchActionSheet.kt` always renders the TV playback card. If multicast discovery is restricted, an interactive **TV Connect Dialog** provides 1-tap subnet scanning or manual IP input with instant persistence. Also includes **Universal Cast** via Android system picker.
   - **Master Google Sheet Cloud Ledger**: Serverless Google Apps Script webhook deployed at `https://script.google.com/macros/s/AKfycbwTFjzb2NgW_Py8dhNTWY1Qen9y4D93yG0NUvzhkm1jzKfCz_gE01WQryMcNThfSXEKqQ/exec` (`UserPreferencesManager.DEFAULT_GOOGLE_SHEET_WEBHOOK_URL`) providing two-way sync for watchlist, ratings, and podcast recommendation ingestion.

3. **UI (Jetpack Compose)**
   - **HomeScreen.kt**: Monolithic navigation and screen layout:
     - *Watchlist Tab (0)*: Filtered by "Free to Me" and duration chips (`< 90m`, `< 120m`). Features **Instant Undo** via an animated top banner and Snackbar action whenever a title is removed.
     - *Watched Vault Tab (1)*: Viewing diary with personal ratings, rewatch badges, and 1-tap Letterboxd CSV export.
     - *ROI Churn & Budget Tab (2)*: Dual mode view featuring **🎯 Watchlist Match** (ranking services by available watchlist titles, "Best Opportunity to Subscribe" and "Safe to Pause" banners, preview chips) and **📊 Spend & Usage** (burn rate, cost/hour, cancel candidates).
     - *Agent Chat Tab (3)*: Local AI assistant (Olivia).
   - **SubscriptionEditSheet.kt & AddServiceDialog.kt**: Modal pricing presets, trial expiration tracker, and custom provider additions.
   - **QuickLogDialog.kt**: 0.5–5.0 star selector, rewatches, and notes.
   - **FeedbackDialog.kt**: Floating FAB on every screen that captures Compose screenshots, gathers device diagnostics, and creates GitHub issues labeled `jules-triage`.
   - **GuideDialog.kt**: Renders `user_guide.html` and `changelog.html` from `app/src/main/assets` directly inside an Android WebView.

### Mandate for AI Agents & Contributors
- **Documentation Integrity**: When releasing or updating any feature, you **MUST** update:
  1. `app/src/main/assets/user_guide.html` (In-App User Guide)
  2. `app/src/main/assets/changelog.html` (In-App Changelog)
  3. `README.md` (Repository documentation)
  4. `DEVELOPER_GUIDE.md` (Architecture and component index)
  5. Session Walkthrough artifact.
