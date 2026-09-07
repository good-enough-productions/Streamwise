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

## 8. Live Watchlist Title Counts & Sticky Queue Clarity (v1.4.0)

- **Dynamic Count Propagation**: Top App Bar title dynamically renders current list size (`Watchlist • X Titles`, `Watched Vault • Y Movies`, `My Services • Z Active`).
- **Bottom Navigation Badging**: Employs Material 3 `BadgedBox` on navigation items reflecting real-time SQLite counts (`Badge { 33 }` on Watchlist, `Badge { 1104 }` on Watched).
- **Sticky Summary Header**: Sticky subheader displays filtered match counts (`Showing X of Y titles • Z hidden by filters`), cleanly differentiating between available content and user filter constraints.

## 9. Cinema Podcasts Integration (`PodcastEpisodeCatalog`)

- **Domain Model**: `PodcastEpisodeCatalog.kt` maps popular film podcasts (*What Went Wrong*, *The Rewatchables*, *The Big Picture*, *Blank Check*, *How Did This Get Made?*) against normalized lowercase titles and subject types (`isMainSubject: Boolean`).
- **Sub-Filter Logic**: When a podcast is selected in `AdvancedFilterBottomSheet`, users can toggle between "Main Film / Featured Topic" (strict match) and "Any Mention (Inclusive)" (broad match).
- **Extensibility**: Catalog is designed for zero-API offline querying, ready for dynamic RSS episode ingestion.

## 10. Service Detail Management & Deal Finder (`ServiceDetailBottomSheet`)

- **Provider Schema v7**: Room `MIGRATION_6_7` extends `StreamingProvider` with `subscribedSince: Long?`, `planName: String?`, `renewalDayOfMonth: Int?`, and `notes: String?`.
- **Tenure Computation**: Dynamically calculates months and days active from the stored timestamp, supporting quick-set tenure chips (*This Month*, *3 Mos*, *6 Mos*, *1 Year*, *2+ Years*).
- **Deal Finder Aggregation**: Direct intent launchers targeting verified deal communities (*The Streamable Deals*, *Slickdeals*, *Doctor of Credit*, and *Google Deals*) formatted with safe URL query encoding.

## 11. GitHub OTA Auto-Updater & Documentation Sync

- **Updater Architecture**: `GitHubUpdateManager.kt` checks GitHub Releases API via OkHttp coroutines, parses release tags (`v1.4.0`), downloads APKs to internal cache with live progress callback, and invokes the Android `PackageInstaller` via `FileProvider` (`com.aistudio.streammanager.qpwoei.fileprovider`).
- **In-App Web Asset Viewers**: `HtmlAssetViewerDialog` embeds Android `WebView` to render local assets (`user_guide.html` and `changelog.html`) offline with dark-theme CSS formatting.

## 12. Letterboxd Two-Way Synchronization Pipeline (v1.5.0)

- **LetterboxdCsvParser**: High-performance parser for standalone CSV exports (`watchlist.csv`, `watched.csv`, `diary.csv`, `ratings.csv`) and full account ZIP archives (`letterboxd-*-data.zip`). Extracts titles, release years, 5-star or 10-point ratings, and watch dates.
- **Deduplication Engine**: Compares imported titles against local Room database (`MediaDao.getAllMediaItemsList()`) using normalized title and release year matching. Skips duplicates while populating missing watch dates and ratings.
- **Two-Way Export**: Generates standard Letterboxd import CSV (`Title,Year,Rating10,WatchedDate`) for movies logged in Streamwise and shares via Android `Intent.ACTION_SEND` (`text/csv`) for 1-tap upload to `https://letterboxd.com/import/`.
- **Google Apps Script Webhook**: `scripts/webhook/StreamwiseLetterboxdSync.gs` accepts file uploads, archives timestamped backups into `Active Builds / Streamwise / Backups & Exports` on Google Drive, and syncs a structured `Streamwise — Letterboxd Sync Database` Google Sheet ($0/mo).

## 13. Modular Explore Tab Architecture (v1.5.0)

- **ExploreTabContent**: Redesigned from a monolithic vertical feed into 4 dedicated, state-preserved sub-tabs with independent scroll states:
  - SubTab 0: `✨ AI & Taste` (`Cinephile Taste Matrix`, `Gemini Pro Intelligence`, tailored picks with `+ Watchlist` action, quick conversation starters).
  - SubTab 1: `💬 Olivia AI` (`AgentChatTabContent` with fullscreen conversational interface and message badge counter).
  - SubTab 2: `🎙️ Podcasts Hub` (`PodcastsExploreView` with show selector chips and `▶️ Listen` / `💬 Ask Olivia` actions).
  - SubTab 3: `📰 Film News Hub` (`NewsExploreView` with category selector chips and `💬 Discuss with Olivia` integration).
- **Navigation Hoisting**: The active sub-tab is hoisted to `HomeScreen` as `exploreSubTab`, allowing bottom sheets and external triggers (such as `onDiscussInExplore` from `MovieDetailsBottomSheet`) to transition directly into Olivia chat.

## 14. Gemini Spark Podcast Tracker Sync & TMDB Availability Sentinel (v1.5.1)

- **Gemini Spark Podcast Tracker**: Real-time synchronization of the living Google Sheet (`Podcast Film & TV Recommendations Tracker`) with Streamwise and the Master Hub General Ledger. Fetches recommendations via Apps Script Web App endpoint (`?action=getPodcastRecs`), enriches the user's Watchlist with origin tags (`importSource: Podcast: [Podcast] - [Episode]`), updates watched titles with podcast notes, and stores in SQLite.
- **Provider Sentinel Resilience ("none")**: When TMDB returns empty US watch provider results for valid titles, the sync engine records `"none"` as a sentinel instead of leaving `providerIds` null. This differentiates unstreamable/theater-only titles from un-queried titles, terminating redundant startup sync loops while cleanly filtering `"none"` from UI provider lists.

## 15. My Services Watchlist Volume Tracking & Resilient Multi-Tier Feedback (v1.5.2)

- **Provider Watchlist Volume**: `MonthlyRoiContent` and `ServiceDetailBottomSheet` compute real-time watchlist volume counts per streaming service: `watchlistItems.count { it.providersList.any { p -> p.equals(provider.id, ignoreCase = true) } }`. Active service cards display `🎬 X Watchlist Titles`, inactive cards display `🎬 Y Queued`, and `ServiceDetailBottomSheet` renders a dedicated list of available queued movies on that platform with 1-tap navigation to movie details.
- **Multi-Tier Resilient Feedback Pipeline**: `submitIssue` implements a 3-tier cascade:
  1. Direct GitHub API via user PAT (if present in settings).
  2. Central Cloud Run proxy (`feedback-proxy-rljydlcchq-uc.a.run.app`).
  3. Google Apps Script Web App fallback with HTTP 302 redirect resolution.
- **Visible Error Banner & Toasts**: `FeedbackDialog` hoists the error card outside the scrollable column right above the submit button, coupled with native Android Toast alerts on both success and error, preventing silent failures.
