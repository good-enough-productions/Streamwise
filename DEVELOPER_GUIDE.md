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

## 16. Cinema Podcast RSS Ingestion Pipeline & Dynamic Catalog Matching (v1.5.3)

- **Zero-Dependency Python Scraper (`scrape_feeds.py`)**: Standalone Python script using standard library modules (`urllib.request`, `xml.etree.ElementTree`, `re`, `csv`, `json`) to scrape, parse, and normalize all available episodes across 5 cinema podcast feeds:
  - *The Rewatchables* (`https://feeds.megaphone.fm/the-rewatchables`) - 479 episodes
  - *The Big Picture* (`https://feeds.megaphone.fm/the-big-picture`) - 968 episodes
  - *Unspooled* (`https://feeds.megaphone.fm/SBP3707703183`) - 468 episodes
  - *How Did This Get Made?* (`https://feeds.simplecast.com/Ao0C24M8`) - 393 episodes
  - *What Went Wrong* (`https://feeds.acast.com/public/shows/what-went-wrong1`) - 208 episodes
  Totaling **2,516 scraped episodes**.
- **Episode Title Extraction Regex Engine**: Robust multi-pattern extractor designed to handle single/curly quotes, internal apostrophes (e.g. *'She's the One'*, *'There's Something About Mary'*), host/guest credit stripping without truncating titles with the word "with" (e.g. *Interview with the Vampire*), and colon/dash episode formatting.
- **Google Sheet Population (`sync_to_sheet.py`)**: Apps Script Web App (`AKfycbzsbZfiDbXXGJunAmJX2xb9OtpnigwVl69M6qbBQ5bNBuyAdj6TtkW-LflbSxSFJJoI0w`) endpoint `appendPodcastEpisodes` populated 2,233 missing episodes into the living Google Sheet tracker with deduplication, bringing total tracker rows to 2,394.
- **Dynamic Asset & Tag-Aware Filtering**:
  - `generate_podcast_assets.py` exports `podcast_titles.json` (2,300+ unique titles mapped to podcast IDs) into Android assets.
  - `PodcastEpisodeCatalog.kt` dynamically loads `podcast_titles.json` on app startup (`PodcastEpisodeCatalog.initialize(context)`), integrates *Unspooled*, and tests title matches against `importSource`, user notes tags (`[The Rewatchables]`), the 2,300+ title catalog, and static mentions.
  - `AdvancedFilterBottomSheet.kt` correctly updates filter count indicators (e.g. *The Rewatchables* displaying 359 matching titles).

## 17. Actor Age at Release Engine (`ActorAgeCalculator.kt` - v1.5.4)

- **Domain Logic & Math**: Derived from the standalone `good-enough-productions/actor-age` repository. Calculates exact age when a movie was released using `ActorAgeCalculator.calculateAgeAtRelease(birthday, releaseDate, deathday)`.
- **Date Handling & Edge Cases**:
  - Full ISO-8601 `YYYY-MM-DD` date parsing using `java.time.LocalDate` and `java.time.Period`.
  - Leap year (`Feb 29`) boundary handling.
  - Year-only fallback (e.g. `"1994"`) estimating age via simple year subtraction.
  - Deceased state computation: verifies if the actor was alive when the movie released, formatting deceased status (`isDeceased: Boolean`) while ensuring post-mortem releases compute their final age at death.
- **TMDB API & Concurrent Lookups**:
  - `TmdbApiService.kt` extended with `@GET("person/{person_id}")` returning `TmdbPersonDetails` (`birthday`, `deathday`, `profile_path`, `place_of_birth`).
  - `StreamViewModel.loadMovieCastWithAges`: Runs parallel network requests using `async(Dispatchers.IO)` limited to the top 10 billed cast members. Results are cached in a thread-safe `ConcurrentHashMap<Int, TmdbPersonDetails>` to avoid redundant network traffic on repeated bottom sheet presentations.
- **UI Integration**: `MovieDetailsBottomSheet` displays a horizontal scrollable card row showing actor portrait, character name, age badge (e.g. `🎂 Age 38 at release`), and deceased badge (`🕊️ (Deceased)`).

## 18. Cinephile Viewing Analytics & Release Eras Engine (`WatchedAnalyticsCalculator.kt` - v1.5.4)

- **Analytics Computation**: Synthesizes the core analytical features from `good-enough-productions/movies-dataset` natively inside Android SQLite/Room memory:
  - **Decades & Eras Breakdown**: Partitions watch history across 7 cinema eras: `2020s`, `2010s`, `2000s`, `1990s`, `1980s`, `1970s`, and `Pre-1970s Classic`. Calculates absolute film counts, percentages, and total runtime per era.
  - **Genre Profiling & User Ratings**: Computes genre frequencies and averages the user's logged Letterboxd star ratings per genre to identify highest-rated categories vs most-watched categories.
  - **Streaming "Service Used"**: Tracks viewing platform tags (`serviceUsed` field in `MediaItem`), quantifying provider usage (Netflix, Max, Criterion Channel, Theatrical, etc.) and unassigned counts.
  - **Letterboxd Rating Distribution**: Partitions ratings into 5 visual histogram tiers: Masterpieces (9-10★), Great (7-8.9★), Good (5-6.9★), Mediocre (3-4.9★), and Poor (0.5-2.9★).
  - **Viewing Rhythm**: Identifies the user's all-time peak watch month (e.g. `October 2025: 42 films`) and calculates year-over-year watch volumes.
- **Interactive UI (`WatchedAnalyticsBottomSheet.kt`)**:
  - Modal bottom sheet with 4 KPI summary cards (Total Films, Screen Time in hours/days, Average Rating, Peak Era).
  - 1-tap interactive filtering: Tapping any era or service directly sets `selectedEra` or `selectedService` in `HomeScreen`, closing the sheet and filtering the Watched Vault with an active dismissible filter chip ribbon.
  - "Service Used to Watch" interactive selector added to `MovieDetailsBottomSheet` for watched titles to easily tag streaming services.

## 19. Data Integrity Hardening, Strict TMDB Gating & CursorWindow Memory Architecture (v1.5.5)

- **Root Cause Analysis (Issues #13 & #14)**:
  - Previously, `syncPodcastRecommendationsInternal` inserted unverified podcast RSS titles directly into `WATCHLIST`. 470 non-movie discussions (mailbags, drafts, auctions, hall of fame shows) polluted the user's database without TMDB IDs or poster art.
  - Furthermore, `filterOnlyMyServices` defaulted to `true`. When a user selected a podcast filter (e.g. *The Rewatchables*), movies covered by the podcast that were not on the user's active paid subscriptions were filtered out, giving the impression that the podcast filter was not working (#13).
  - In addition, concatenating multi-kilobyte episode descriptions into `userNotes` pushed the SQLite query payload beyond Android's default 2MB `CursorWindow` limit, causing `IllegalStateException: Couldn't read row 587, col 0 from CursorWindow` on cold launch.
- **Data Integrity Sanitize & Gatekeeping (`MediaTitleSanitizer.kt`)**:
  - `isNonMovieEpisode`: Comprehensive regex blacklist filtering out drafts, auctions, mailbags, Oscar predictions, festival recaps, and bare podcast show titles.
  - `cleanCandidateTitle`: Strips podcast prefixes (e.g. *"The Re-"*, *"(Re)"*), guest names, and episode markers before TMDB querying.
  - **Zero-Tolerance Gating**: During podcast syncs or imports, items that do not return a matching movie or TV show on TMDB are discarded immediately—never inserted into the Watchlist.
- **Room v9 Database Schema & CursorWindow Resilience**:
  - Upgraded Room DB from version 7 -> 8 (adding `releaseDate TEXT` column) -> 9 (automated data integrity purge and text trimming).
  - Synchronous `onOpen` SQLite cleanup:
    ```sql
    DELETE FROM media_items WHERE status = 'WATCHLIST' AND (tmdbId IS NULL OR tmdbId = '');
    UPDATE media_items SET userNotes = substr(userNotes, 1, 300) WHERE length(userNotes) > 300;
    UPDATE media_items SET overview = substr(overview, 1, 300) WHERE length(overview) > 300;
    UPDATE media_items SET trivia = NULL WHERE trivia IS NOT NULL;
    ```
  - In `StreamApp.onCreate()`, `sCursorWindowSize` is programmatically set to 50MB via reflection, permanently preventing `CursorWindowAllocationException`.
- **Podcast Filter Precedence (`HomeScreen.kt`)**:
  - Filter logic updated: when `selectedPodcastId != null` or `selectedPlatforms.isNotEmpty()`, the subscription-gating check (`filterOnlyMyServices`) is automatically bypassed so all covered films across all providers are displayed.

## 20. UI Streamlining, Cinephile Auteurs, and Subscription Expected Value (v1.5.6)

### Watched Vault Header Rework (Issue #16)
- **Problem**: The fixed header chrome in `WatchedTabContent` (3-KPI summary card, viewing analytics banner, genre tags ribbon, Letterboxd sync buttons) occupied over 55% of the vertical viewport on mobile devices, squeezing the diary to only 1-2 visible movie cards.
- **Solution**:
  - Re-architected `WatchedTabContent` to feature a slim sticky top bar (48dp height search bar, compact sort dropdown, view mode switcher, and analytics icon button) taking only ~90dp total height.
  - Encapsulated the heavy metrics and Letterboxd sync buttons into `WatchedVaultOverviewCard`.
  - Moved `WatchedVaultOverviewCard` inside the scrollable content (`LazyColumn` and `LazyVerticalGrid`), using `GridItemSpan(maxLineSpan)` in grid mode.
  - Implemented a collapsible state: defaults to a compact 1-line summary (`🎬 1,130 Films • ~2,071h • ★ 7.2 • ~7.0/mo [Insights 📊] [▾]`) that expands to full sync buttons and metrics upon tap. Scrolling down immediately moves the card off screen, dedicating 90%+ vertical space to films.

### "Ah Hah" Cinephile Analytics Engine (Issue #17)
- **Top Directors & Auteurs**:
  - Curated filmographies for 20+ prominent auteurs (Coen Brothers, Nolan, Scorsese, Fincher, Tarantino, Ridley Scott, Cameron, Spielberg, PTA, Kubrick, Linklater, Villeneuve).
  - Exact normalized title matching (`normalizeTitle`) strips punctuation, parentheses, and leading articles, preventing false substring matches (e.g. *"Us"* matching *"Inglourious Basterds"*).
  - Aggregates film counts, average ratings (e.g. `★ 8.8`), and surfaces distinct sample title chips.
- **Top Actors & Screen Presence**:
  - Tracks top Hollywood stars across the user's logged vault (Tom Cruise, Matt Damon, Leonardo DiCaprio, Brad Pitt, Robert De Niro, Christian Bale, Margot Robbie, Al Pacino, Emma Stone, Ryan Gosling).
- **Cinephile Blind Spots & Watchlist Integration**:
  - Identifies underrepresented areas in the user's vault (<3% representation across Golden Age Cinema Pre-1970s, 1970s New Hollywood, Westerns, and Documentaries).
  - Cross-references these gaps against the user's 1,100+ Watchlist queue to surface queued titles ready to stream (e.g. *The Searchers*, *High Noon*, *The Godfather*, *Free Solo*), creating an actionable bridge between past viewing and future discovery.

### Subscription Expected Value & Single-Service Rotation Advisor (Issue #15)
- **Viewing Velocity**: Analyzes the last 6 active viewing months to compute the user's real-world watch velocity (~7.0 films/month).
- **Effective Cost Per Film**: Dynamically calculates `totalMonthlySpend / monthlyVelocity` (e.g. \$64.95 / 7 = \$9.28/film).
- **Single-Service Rotation Advisor**:
  - Models the expected value of maintaining only 1 active subscription at a time.
  - Identifies the streaming service with the largest backlog in the user's Watchlist (e.g. Max with 142 queued films) and calculates how many months of continuous entertainment it provides at the user's current velocity (~20 months).
  - Calculates potential savings (saving ~$49/month or ~$588/year) by pausing idle subscriptions with zero loss of content supply.

## 21. Watched Era & Rating Extraction, Collapsible Spotlight & Clean UI Architecture (v1.5.7)

### 100% Release Year & Decade Extraction (0% Unknown Era)
- **Root Cause**: In previous versions, Letterboxd CSV imports populated `status = 'WATCHED'` and `tmdbId` but left `releaseDate` null or unparsed for bulk history, causing `WatchedAnalyticsCalculator` to classify 1,104 out of 1,130 films (97.7%) under "Unknown Era".
- **Startup Backfill Engine (`AppDatabase.kt` / `MediaRepository.kt`)**:
  - Automatically parses release years from Letterboxd URL slugs (`/film/{slug}-{year}/`), user notes, and title strings during database initialization and sync.
  - Safely backfills `releaseDate = 'YYYY-01-01'` without overwriting existing TMDB metadata.
  - Result verified on physical device: 100% of 1,130 watched films classified into exact cinema eras (2000s: 34.3%, 2010s: 25.9%, 1990s: 18.6%, 2020s: 15.0%, 1980s: 4.3%, 1970s: 1.3%, Pre-1970s: 0.5%) with **0 films (0.0%) in Unknown Era**.
- **Star Rating Extraction**:
  - Parses 10-point and 5-star ratings from import notes and CSV records into `userRating`, powering accurate rating distributions and top auteur averages.

### UI Streamlining & Screen Reclaim (Issues #7, #8, #9, #10)
- **Collapsible Spotlight (Issue #7)**:
  - Replaced fixed 220dp spotlight carousel with a collapsible header row.
  - When collapsed, reduces to a compact 36dp header bar (`✨ Spotlight: Ready to Stream (X) • Tap to view [▾]`), reclaiming over 30% of vertical screen space on mobile.
  - State persisted via `UserPreferencesManager.isSpotlightCollapsed`.
- **Streamlined Filter Header (Issue #8)**:
  - Eliminated noisy multi-select filter chip carousels from the top of the Watchlist tab.
  - Replaced with a minimal, unified action row: `[✓ My Services]`, `[Free w/ Ads]`, and `[Filters (X)]` with active count badge.
  - Multi-select platforms, genres, release decades, rating floors, and podcast filters are consolidated into `AdvancedFilterBottomSheet`.
- **Watched Vault Rework & Decade Trend Capsules (Issue #9)**:
  - Added instant visual decade trend capsules (`2000s (34%)`, `2010s (26%)`, etc.) directly on `WatchedVaultOverviewCard`.
  - Updated `WatchedMediaCard` and `WatchedGridPosterCard` to emphasize release years (`Title (Year)`), star rating badges, and watch dates.
  - Encapsulated secondary CSV import/export buttons into a collapsible "CSV File Tools" drawer to declutter the diary view.
- **1-Tap Letterboxd + Google Sheet Sync (Issue #10)**:
  - Added prominent `[☁️ Sync Letterboxd & Google Sheet]` button on `WatchedVaultOverviewCard` with indeterminate progress indicator and toast notifications.

## 22. Design System Modernization & UX Critic Audit Resolution (v1.5.8)

Comprehensive resolution of the 14 critical findings identified in the Streamwise UX/UI Audit (`UX Critic-saved.html`):

### Standardized Button & Component System (Tokens & Styling)
- **High-Contrast Input Tokens (`Color.kt`)**:
  - `DarkInputBackground` (`0x14FFFFFF` / 8% surface fill): Elevates text field contrast against pure black AMOLED backgrounds.
  - `DarkBorderOutline` (`0x40FFFFFF` / 25% white opacity): Replaced 12% opacity borders to meet WCAG AA contrast for unfocused form fields.
  - `DarkOutlineVariant` (`0x2EFFFFFF` / 18% white opacity): Replaced 10% borders for subtler card and divider outlines.
  - `TextLowEmphasis` (`0x9EFFFFFF` / 62% white opacity): Replaced 38% opacity placeholder/label text to guarantee legibility.
- **Universal Field Application**:
  - Standardized `OutlinedTextFieldDefaults.colors` across all search bars (Watchlist, Watched Vault), dialogs (`AddMediaDialog`, `EditProviderDialog`, `CheckInDialog`, `FeedbackDialog`), and Settings configuration fields.

### Agent Screen Humanization & Conversational Ergonomics
- **Technical Jargon Elimination**:
  - Replaced internal engineering references ("Gemma 4", "Gemini 3.1 Pro", "API Keys") with consumer-friendly framing: *"Olivia — AI Film Companion"*, *"Personalized Film & Taste Analysis"*, and *"Analyzing your cinematic vault..."*.
- **Floating Icon Collision Elimination**:
  - Replaced oversized 56dp floating send button with an ergonomic 52dp inline `FilledIconButton` positioned alongside the input field with automated keyboard avoidance.
  - Suppressed the global `FloatingFeedbackButton` whenever the user is actively on the Olivia chat tab (`selectedTab == 3 && exploreSubTab == 1`), eliminating touch target overlaps.

### Check-In Prompts & Decision Clarity
- **Hierarchical Action Buttons**:
  - Replaced ambiguous inline checkbox/toggle logic with explicit hierarchical buttons:
    - Primary: `[Yes, finished it →]` (`Button`)
    - Secondary: `[Watched partially →]` (`FilledTonalButton`)
    - Tertiary: `[Watched something else →]` (`OutlinedButton`)
    - Dismissal: `[Didn't watch anything]` (`TextButton`)
  - Clear multi-step wizard preserves provider attribution, duration logging, and optional personal notes.

### Subscription Rotation & Billing Transparency
- **Advisor Transparency**:
  - Added an explicit disclaimer on the Single-Service Rotation card: *"Note: Streamwise tracks subscriptions to calculate savings and optimize your viewing schedule. Actual subscription changes must be managed directly with providers."*
  - Updated provider status toggle text in `EditProviderDialog` to *"Active (Tracked in Streamwise)"* and *"Inactive / Paused (Tracked in Streamwise)"*.

### Movie Card Polish & Deduplication
- **Free Badge & Action Collision**:
  - Isolated the `FREE` indicator with a dedicated pill token (`FreeBadgeContainer` / `OnFreeBadgeContainer`) and capped provider badge rows with ellipsis truncation.
  - Restricted movie synopsis previews to 1 line, ensuring the primary "Watch" button remains visually unobstructed.
- **Catalog Deduplication**:
  - Integrated `MediaTitleSanitizer.cleanCandidateTitle(it.title).lowercase()` deduplication across spotlight and watchlist feeds.

### Responsive Breakpoints
- **Landscape Navigation**:
  - Added orientation-aware height checks to `NavigationBar`, adjusting from 72dp in portrait to a compact 56dp in landscape to preserve vertical browsing real estate.

## 23. Couch-First Poster Density, Letterboxd Sub-Navigation & Zero-Intermediary Deep Linking (v1.5.9)

Architecture updates addressing mobile visual density and couch-first discovery:

### 1. Couch-First 3-Column Poster Grid & View Mode Toggle
- **`PosterGridItem` Composable (`HomeScreen.kt`)**:
  - High-density 3-column layout utilizing 2:3 aspect ratio posters with 10dp rounded corners.
  - Overlay badges: Top-left gold star rating badge (`RatingGold` / `DarkGoldSurface`), top-right circular quick-play button (`DeepCouchOrange`), and bottom service attribution pill.
  - Beneath poster: Single-line bold title, release year, and primary genre.
- **Sticky Header Integration (`LazyColumn`)**:
  - Chunked 3-column row rendering (`processedItems.chunked(3)`) inside `LazyColumn` rather than nested grids, maintaining seamless pinning of the search and filter ribbon via `stickyHeader`.
- **Persistent View Mode State**:
  - Backed by `UserPreferencesManager.isGridView` (default `true`), exposed via `StreamViewModel.isGridView: StateFlow<Boolean>`.
  - Seamless toggle icon in `TopAppBar` (`Icons.Default.ViewAgenda` vs `Icons.Default.GridView`).

### 2. Letterboxd-Style Top Sub-Navigation Tabs
- **Watchlist Sub-Tabs**:
  - `Ready on Subs (X)`: Dynamically counts and filters titles ready to stream on active subscriptions or free ad-supported platforms.
  - `All Saved (X)`: Shows complete saved catalog across all providers.
  - `Under 100m`: Filter for short runtime cinema (<105 mins or comedy/animation/shorts).
  - `Podcast Picks (X)`: Dynamic count and filter for films covered on major cinema podcasts.
- **Watched Vault Sub-Tabs**:
  - `Watched Diary (X)`: Chronological timeline with re-watch intents.
  - `Highest Rated`: Instant sort by user and community ratings.
  - `Analytics`: One-tap navigation to the Cinephile Viewing Analytics Hub.

### 3. Zero-Intermediary Streaming App Deep Linking (`StreamingAppLauncher.kt`)
- **Direct Couch Handoff**:
  - Tapping "Watch" resolves native Android package names, custom deep link URI schemes, and media search intents across 15+ streaming services:
    - Prime Video (`com.amazon.avod.thirdpartyclient`, `https://app.primevideo.com/detail?gti=...`)
    - Netflix (`com.netflix.mediaclient`, `nflx://www.netflix.com/title/...`)
    - Max (`com.wbd.stream`, `https://play.max.com/movie/...`)
    - Disney+ (`com.disney.disneyplus`, `https://www.disneyplus.com/movies/...`)
    - Hulu (`com.hulu.plus`, `https://www.hulu.com/movie/...`)
    - Tubi (`com.tubitv`, `https://tubitv.com/movies/...`)
    - Apple TV+ (`com.apple.atve.androidtv.appletv`, `https://tv.apple.com/movie/...`)
    - Paramount+, Criterion Channel, Pluto TV, Peacock, Hoopla, Kanopy
  - Seamlessly activates `viewModel.launchAndIntendToWatch(context, item)`, updating the database status to `INTENDING_TO_WATCH` so the background check-in watcher triggers when the user returns.

### 4. High-Performance O(1) Catalog Indexing
- **Startup ANR Elimination (`PodcastEpisodeCatalog.kt`)**:
  - Pre-indexes 2,300+ dynamic titles and static mentions into `allDynamicTitlesSet` and `staticMentionTitles` hash sets.
  - Converts $O(N \times M)$ string search loops across 1,151 movies into instantaneous $O(1)$ hash set lookups (<1ms), completely eliminating main-thread freezing and Android input dispatch timeouts.


