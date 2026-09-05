# Streamwise Developer Guide

## Architecture Overview
Streamwise is a native Android application built entirely with **Kotlin** and **Jetpack Compose**. It aims to solve the "subscription fatigue" problem by combining a Watchlist manager with a Financial ROI tracker and a forward-looking Churn Optimizer.

### Key Components

1. **Local Database (Room Schema v10)**
   - **AppDatabase.kt**: SQLite database powered by Android Room with migration paths `MIGRATION_8_9` and `MIGRATION_9_10`.
   - **Entities**:
     - `MediaItem`: Represents a movie, TV series, or podcast recommendation. Schema v10 adds Release Radar columns: `nextAirDate`, `nextEpisodeTitle`, `releaseStatus`, and `digitalReleaseDate`. Schema v9 adds `mediaType` ("MOVIE", "TV", "PODCAST"), `totalSeasons`, `totalEpisodes`, `lastWatchedSeason`, and `lastWatchedEpisode`. Schema v8 columns: `userRating` (0.5–5.0), `isRewatch`, `letterboxdUri`, `syncedToSheet`, `runtimeMinutes`, and `releaseYear`.
     - `StreamingProvider`: Represents a platform (Netflix, Hulu, Criterion, etc.). Custom user pricing, trial durations, and active states are stored here to drive ROI math.
     - `WatchSession`: Every time an item is checked off or watched on TV, a session is logged to track hours spent on a given platform.
   - **MediaDao.kt**: Contains Room queries, monthly usage aggregations (`getMonthlyUsageStats`), and custom provider deletions.

2. **Network, TV Companion, Cloud AI & Ingestion**
   - **TMDB Movies & TV Endpoints**: Ingests title metadata, exact runtimes, release dates, seasons, episode counts, upcoming episode air dates (`next_episode_to_air`), in-theaters status, digital SVOD dates, and streaming availability across providers in `AvailabilitySyncWorker.kt`.
   - **Proactive Release Radar & Availability Alerts (`NotificationHelper.kt`)**: Background worker identifies returning series air dates, theatrical-to-streaming windows, and newly available titles on active subscriptions, firing tap-launchable push notifications.
   - **Letterboxd Watchlist Crawler & CSV Parser (`LetterboxdImporter.kt`)**: Scrapes public Letterboxd profile pages (`letterboxd.com/{username}/watchlist/page/{n}/`) directly with 1 tap, parsing film posters, slugs, and titles with zero API key dependencies. Also parses exported `watched.csv` and `watchlist.csv` files.
   - **Cloud-Native AI Advisor (`GeminiClient.kt`)**: Native REST HTTP client for Google's `gemini-2.0-flash` model. Operates at ~$0.015/user/month unit economics for instant conversational recommendations and pre-watch cultural synthesis, bypassing local Ollama server dependencies.
   - **Renewal Radar & 1-Click Cancellation (`SubscriptionRenewalManager.kt`)**: Maintains verified deep-link cancellation URLs for 12+ providers (Netflix, Max, Disney+, Hulu, Paramount+, Criterion, Apple, Prime, Peacock). Computes days-until-renewal, fires proactive notification alerts, and renders direct 1-tap browser intent launchers.
   - **Native TV Companion (`TvCompanionService.kt`)**: An embedded lightweight HTTP server listening on port 8998 with `BootReceiver.kt` and screen wake-lock capabilities. Receives JSON launch payloads (`/launch`) and initiates native Fire OS package activities (Netflix `com.netflix.ninja`, Disney+ `com.disney.disneyplus`, Hulu, Prime Video, Tubi, Pluto, etc.) or universal search.
   - **Deep-Link Relay (`FireTvRelay.kt`)**: Communicates with `TvCompanionService` over Wi-Fi, returning detailed confirmation messages and fallback warnings.
   - **Hybrid Subnet Sweep TV Discovery**: `CastingManager.kt` performs a concurrent TCP port sweep (5555, 8008, 8009, 8998) across the local `/24` subnet on startup, bypassing router multicast isolation to identify Fire TVs (*Danny's Fire TV*) and Google Cast devices in < 200ms with real-time UI scan progress.
   - **Watch Hub & TV Connect**: `WatchActionSheet.kt` renders TV playback card, interactive **TV Connect Dialog**, and **Universal Cast** via Android system picker.
   - **Master Google Sheet Cloud Ledger**: Serverless Google Apps Script webhook providing two-way sync for watchlist, ratings, and podcast recommendation ingestion.

3. **UI (Jetpack Compose)**
   - **OnboardingDialog.kt**: 3-step interactive first-run onboarding wizard (service picker, Letterboxd sync, ROI spend preview).
   - **UpgradePaywallSheet.kt**: Streamwise Pro paywall sheet ($3.99/mo or $39.99/yr with 7-day free trial) gating unlimited services, TV tracking, podcast recs, and background alerts.
   - **HomeScreen.kt**: Monolithic navigation and screen layout:
     - *Top App Bar*: Consolidated 3-button layout (Sync, Overflow Menu [Guide, Letterboxd Import/Export], Settings) with softWrap-protected FREE/PRO badges.
     - *Watchlist Tab (0)*: Filtered by "Free to Me" default with **Unified Horizontal Quick Filter Ribbon** (`All`, `✓ My Services`, `Free w/ Ads`, `🎬 Movies`, `📺 TV`, `🎙️ Podcasts`, `🗓️ Radar`, `< 90m`, `< 120m`, `Surprise Me`, `Letterboxd`), and **Expandable Advanced Filter Panel** (Services & Genres) via Tune toggle. Features **1-Tap Letterboxd Import**, **Instant Undo** on deletion, live match counter, guided recovery empty states ("Show All Watchlist Titles"), streamlined `MediaItemCard` with expandable synopses, and Olivia's pre-watch synthesis.
     - *Watched Vault Tab (1)*: Viewing diary with personal ratings, rewatch badges, TV episode progress, streamlined count summary, and 1-tap Letterboxd CSV export.
     - *ROI Churn & Renewal Radar Tab (2)*: Dual mode view featuring **🎯 Watchlist Match** (ranking services by available watchlist titles, "Best Opportunity to Subscribe", "Safe to Pause" banners, **Renewal Radar** countdown badges, and **1-Click Official Cancellation** links) and **📊 Spend & Usage** (burn rate, cost/hour, cancel candidates).
     - *Agent Chat Tab (3)*: Conversational AI assistant (Olivia) routed through Gemini 2.0 Flash or local Ollama, featuring 1-tap quick action prompt chips and keyboard-safe elevated FAB placement.
   - **QuickLogDialog.kt**: 0.5–5.0 star selector, TV episode check-in steppers (Season and Episode counters), rewatches, and notes.
   - **FeedbackDialog.kt**: Floating FAB with user-preference toggle in Settings > Dev tab that captures Compose screenshots, gathers device diagnostics, and creates GitHub issues labeled `jules-triage`.
   - **GuideDialog.kt**: Renders `user_guide.html` and `changelog.html` from `app/src/main/assets` directly inside an Android WebView.

### Mandate for AI Agents & Contributors
- **Documentation Integrity**: When releasing or updating any feature, you **MUST** update:
  1. `app/src/main/assets/user_guide.html` (In-App User Guide)
  2. `app/src/main/assets/changelog.html` (In-App Changelog)
  3. `README.md` (Repository documentation)
  4. `DEVELOPER_GUIDE.md` (Architecture and component index)
  5. Session Walkthrough artifact.
