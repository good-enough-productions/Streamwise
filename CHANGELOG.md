# Changelog

All notable changes to Streamwise will be documented in this file.

## [1.6.1] - 2026-09-10

### Fixed
- **Awkward Text Wrapping Everywhere**:
  - Restructured the bottom action bar in `MovieDetailsBottomSheet` into a spacious 2-row layout: a prominent full-width **Watch Now** button on top, with **Watched / Watchlist**, **Letterboxd**, and **Delete** evenly spaced below.
  - Enforced `maxLines = 1`, `softWrap = false`, and `TextOverflow.Ellipsis` across all buttons, filter chips, navigation labels, and badges throughout the app so text never breaks awkwardly into single-letter vertical columns.
- **Immediate Watchlist-to-Watched Move**:
  - Fixed a race condition in `MovieDetailsBottomSheet` where `onDismiss()` was called before the mark-as-watched callback, setting the selected movie reference to `null` and preventing the move from executing.
  - Clicking "Watched" anywhere (bottom sheet, watchlist card, or removal confirmation) now immediately transitions the movie out of the Watchlist and into the Watched Vault in Room DB.

### Added
- **Interactive Mark Watched & Custom Date Dialog (`MarkWatchedDialog`)**:
  - Tapping "Watched" or "Change Date" now launches a dedicated dialog with flexible date options:
    - **No Date (Default)**: Leaves `watchedAt = null`, grouping under "Undated Logs" without artificially falling back to the import timestamp.
    - **April 2025**: 1-tap option matching the bulk of back-catalog diary history (~55% of watched titles).
    - **Today**: Instant current timestamp logging.
    - **Pick Date...**: Launches native Android `DatePickerDialog` to select any custom calendar date.
  - Optional star rating (1–5 stars) and viewing method/service selector (Netflix, Max, Theater, Blu-ray, etc.).
  - Added "Change Date" editor banner directly inside `MovieDetailsBottomSheet` for movies already in the Watched Vault.

## [1.6.0] - 2026-09-09

### Added
- **Share & Multi-Format Export Suite (Resolves Issue #18)**:
  - Top app bar Share icon now launches an interactive `ShareAndExportDialog` with 4 clear export options:
    - 📤 **Share Recommendations**: Opens the native Android share chooser with formatted titles and streaming platform availability for easy sharing via SMS, Discord, Slack, etc.
    - 📝 **Export Markdown (Obsidian)**: Generates linked Markdown notes directly into `Downloads/StreamwiseVault` for personal knowledge management and Obsidian vaults.
    - 📊 **Export Letterboxd CSV**: Generates standard diary import CSV files for seamless Letterboxd sync.
    - 📋 **Copy Picks to Clipboard**: Instantly copies a clean list of top recommendations to the system clipboard.
- **Card Removal, 1-Tap Watched & Vault Destination Controls (Resolves Issue #20)**:
  - Tapping the `X` button on any movie card now opens a `RemoveOrWatchedConfirmationDialog` clarifying the exact action:
    - For Watchlist items: choice between `Mark as Watched` (moves to Watched Vault) vs `Delete` (permanently removes item).
    - For Watched items: choice between `Move to Watchlist` (restores to active queue) vs `Delete`.
  - Added dedicated 1-tap `[✓]` Watched button on `MediaItemCard` right beside the "Watch" button.
  - Added `[✓ Mark as Watched]` / `[↶ Move to Watchlist]` actions inside `MovieDetailsBottomSheet`.
  - Upgraded `AddMediaDialog` with a destination toggle allowing users to directly catalog films straight into the **Watched Vault** (with automatic watch session logging) or the **Watchlist**.

### Fixed & Improved
- **UI Responsiveness & Sync Throttling (Resolves Issue #19)**:
  - Eliminated UI lag and recomposition stutter caused by unthrottled sync operations on app resume.
  - Implemented 30-minute persistent cooldown throttling on `triggerImmediateSync()` and Letterboxd live RSS background fetches.
  - Optimized `PosterGridItem` rendering from $O(N)$ allocations to $O(1)$ set lookups using precomputed `remember` provider sets.
  - Removed redundant network thrashes when switching to the Watched Vault tab.


## [1.5.3] - 2026-09-06

### Added
- **Cinema Podcast Catalog & RSS Scraper (`scrape_feeds.py`)**:
  - Standalone zero-dependency script parsing, normalizing, and extracting film titles across 5 premier film podcasts (2,516 total episodes):
    - *The Rewatchables* (479 episodes)
    - *The Big Picture* (968 episodes)
    - *Unspooled* (468 episodes)
    - *How Did This Get Made?* (393 episodes)
    - *What Went Wrong* (208 episodes)
  - Exports standard catalog to `podcast_catalog.csv` and `podcast_catalog.json` with air dates, clean film titles, formats, verdicts, and show context.
- **Gemini Spark Sheet Tracker Completion**:
  - Synchronized 2,233 newly discovered podcast episodes directly into the living Google Sheet (`1Eo-SVac12qRs8Z5DtYKUGqyC54xgDtm8584CFCyRr18`) with automatic deduplication.
- **Dynamic In-App Podcast Filtering**:
  - Bundled `podcast_titles.json` into assets for offline, zero-latency lookup of 2,300+ movie titles across podcasts.
  - Added *Unspooled* (`📽️`) to `AVAILABLE_PODCASTS` in `PodcastEpisodeCatalog`.
  - Upgraded `isCoveredOnPodcast` to check both dynamic asset catalogs and movie `importSource`/`userNotes` metadata tags.
  - Fixed filter modal showing `Show 0 Titles` for *The Rewatchables* (now matches 350+ queued titles).

## [1.5.2] - 2026-09-06

### Added
- **My Services Watchlist Volume & Queue Tracking (Issue #11)**:
  - Streaming service cards in the *My Services* tab now display live counts of how many titles on your active watchlist are available to stream on that provider (e.g. `🎬 10 Watchlist Titles`).
  - Inactive streaming services also display queued counts (e.g. `🎬 3 Queued`), providing instant signal on whether an inactive subscription is worth activating.
  - Overall monthly spend summary card displays total ready-to-stream watchlist titles across all currently active services.
  - In the `ServiceDetailBottomSheet`, a dedicated **Watchlist on [Service]** card lists all queued titles on that provider with 1-tap navigation to open movie details.

### Fixed & Improved
- **Multi-Tier Resilient Feedback Pipeline**:
  - Re-authenticated the Cloud Run `feedback-proxy` service (`https://feedback-proxy-rljydlcchq-uc.a.run.app`) with updated GitHub credentials, resolving HTTP 401 submission failures.
  - Added automated fallback to Google Apps Script Web App (`https://script.google.com/macros/s/AKfycbzsbZfiDbXXGJunAmJX2xb9OtpnigwVl69M6qbBQ5bNBuyAdj6TtkW-LflbSxSFJJoI0w/exec`) with HTTP 302 redirect following.
  - Feedback dialog now features a sticky error banner directly above the submit button (outside the scrollable area) with full Android Toast alerts on success and failure.

## [1.4.0] - 2026-09-06

### Added
- **Watchlist & Vault Live Title Count Clarity**:
  - Top App Bar dynamically displays live counts: `Watchlist • X Titles`, `Watched Vault • Y Movies`, `My Services • Z Active`.
  - Bottom Navigation items feature real-time badge counters (`Badge { 33 }` on Watchlist, `Badge { 1104 }` on Watched).
  - Sticky list header provides clear queue tracking: `Showing X of Y titles • Z hidden by filters`.
  - Removed redundant Top Bar refresh icon in favor of inline pull/sync and sticky bar status.
- **Cinema Podcasts & Media Mentions Filter**:
  - Dedicated "Podcasts & Media Mentions" section in `AdvancedFilterBottomSheet`.
  - Filter by premier film podcasts: *What Went Wrong*, *The Rewatchables*, *The Big Picture*, *Blank Check*, and *How Did This Get Made?*.
  - Sub-filter toggle: "Main Film / Featured Topic" vs "Any Mention (Inclusive)".
  - Backed by offline `PodcastEpisodeCatalog` matching normalized movie titles.
- **My Services Management & Live Deal Finder**:
  - Clickable provider cards in `MonthlyRoiContent` open `ServiceDetailBottomSheet`.
  - View and customize monthly subscription cost, plan tier (e.g. *Standard with Ads*), and billing renewal day of month (1-31).
  - Accurate tenure tracking: view total months and days subscribed (`Tenure: 12 months (365 days) • Since Sep 6, 2025`) with quick-set chips (*This Month*, *3 Mos*, *6 Mos*, *1 Year*, *2+ Years*).
  - Live return on investment metrics: total hours watched, minutes streamed, and effective cost-per-hour.
  - 1-tap live deal finder search queries targeting *The Streamable Deals*, *Slickdeals*, *Doctor of Credit*, and *Google Deals* to discover discounts, bundles, and student offers beyond standard provider pricing.
- **Letterboxd Live RSS Diary Sync**:
  - Direct RSS parser (`LetterboxdSyncManager.kt`) fetching `https://letterboxd.com/$username/rss/` directly without API keys.
  - Automatically scales 5-star ratings to 10-scale and parses watch dates into the offline Room database.
  - Interactive `LetterboxdSyncDialog` with live status indicator and summary results.
- **Unified 4-Tab Settings Hub**:
  - Modernized `SettingsDialog` into 4 dedicated tabs:
    1. *Profile & Accounts*: Display name, Letterboxd username, live RSS sync trigger, and Google Sheet ledger webhook URL.
    2. *Streaming Services (19)*: Toggle switches and monthly pricing for all 19 tracked providers.
    3. *Guides & Docs*: 1-tap in-app HTML asset viewers (`HtmlAssetViewerDialog`) for `user_guide.html` and `changelog.html`.
    4. *Updates & System*: GitHub OTA Auto-Updater and API key management (TMDB, Gemini Pro, Watchmode, Ollama host, GitHub token).
- **GitHub OTA Auto-Updater & Third-Party Aggregator Support**:
  - `GitHubUpdateManager.kt` checks GitHub Releases API for new releases (`v1.4.0`, etc.).
  - Shows update status, release notes, and download progress bar.
  - Uses `FileProvider` (`com.aistudio.streammanager.qpwoei.fileprovider`) and `REQUEST_INSTALL_PACKAGES` permission to hand off APKs directly to Android's `PackageInstaller`.
  - Releases are ready for public third-party sideloading tools like Obtainium.
- **Expanded 19 Streaming Providers Coverage**:
  - Full catalog and DB support across AMC+, Apple TV+, BritBox, Criterion Channel, Disney+, Fandango at Home, Freevee, Hoopla, Hulu, Kanopy, Max (HBO), MGM+, Netflix, Paramount+, Peacock, Pluto TV, Prime Video, Starz, and Tubi.
  - Seeded in `AppDatabase.kt` via `MIGRATION_6_7`.

## [1.3.1] - 2026-09-06

### Improved
- **Natural Scrolling Spotlight & Sticky Search Bar**:
  - Spotlight "Ready to Stream" now scrolls naturally with the feed instead of requiring manual collapse interaction.
  - When scrolling down, Spotlight scrolls cleanly offscreen while the Search & Filter bar sticks firmly to the top (`stickyHeader`).
  - Scrolling back to the top smoothly brings the Spotlight carousel back into view.
- **Multi-Select Filter Widening (OR Logic)**:
  - Advanced Filters modal now supports selecting multiple chips within any category (Streaming Platforms, Genres, Release Eras) to broaden/widen searches using OR logic.
  - Active filters display as individual 1-tap dismissible pills in the sticky filter ribbon.
  - Dynamic "Show N Titles" badge reflects the cumulative match count in real time.

## [1.3.0] - 2026-09-06

### Added
- **Collapsible & Non-Sticky Spotlight (Issue #7)**: Added persistent collapse/expand toggle on the "Spotlight: Ready to Stream" carousel with animated chevron, freeing up the top 1/3 of the screen for denser watchlist browsing.
- **Advanced Multi-Dimensional Filters Sheet (Issue #8)**: Replaced cramped horizontal filter chip rows with a clean top ribbon ("My Services", "Free w/ Ads", quick dismiss tags) and an expandable "Filters" modal sheet supporting streaming provider checkboxes, genre chips, minimum rating thresholds (6.0+ to 8.5+), release era filters, and sorting.
- **Reworked Watched Cinephile Vault & Diary (Issue #9)**:
  - Added Cinephile Vault statistics card displaying total logged movies, estimated watch hours, average rating, and top 4 favorite genres.
  - Grouped watch diary entries chronologically by Year-Month (`September 2026`, etc.).
  - Added dedicated `WatchedMediaCard` featuring formatted watch dates (`Watched Sep 4, 2026`), user notes/Letterboxd excerpts, and 1-tap re-watch intent triggers.
  - Added view mode toggle between chronological Diary Timeline and visual Poster Wall Grid (`WatchedGridPosterCard`).
- **1-Tap Letterboxd Google Sheet Sync (Issue #10)**:
  - Added "Sync to Google Sheet" button in Watched tab that exports watched history and watchlist items into structured JSON payloads sent directly to a Google Apps Script webhook ($0/month architecture).
  - Configurable Google Sheet Webhook URL in Settings -> APIs tab.
  - Fallback Letterboxd web search action directly inside Movie Details Bottom Sheet.
- **Global Backlog & Jules Opt-In Standard**: Feedback issues now land directly on the living GitHub backlog by default, reserving the `jules` / `jules-triage` autonomous AI labels for when the user explicitly checks "Assign to Jules (Autonomous AI)".

## [1.2.2] - 2026-09-06

### Fixed
- **Feedback Webhook 404 Resolution**: Updated the Jules feedback proxy URL in `FeedbackDialog.kt` to the live Cloud Run endpoint (`https://feedback-proxy-rljydlcchq-uc.a.run.app`), restoring $0/month tokenless feedback submissions with attached screenshots directly into GitHub Issues.
- **Duplicate Titles Elimination**:
  - Fixed concurrent database seeding between `onCreate` and `onOpen` in `AppDatabase.kt` using atomic synchronization and normalized title caching.
  - Implemented automatic database deduplication in `MediaDao.kt` (`deduplicateMediaItems`) that preserves the richest record (with TMDB poster, metadata, and provider links) and purges twin entries.
  - Added Flow-level deduplication in `MediaRepository.kt` and UI-level guardrails across Spotlight, Watchlist, and Watched Vault in `HomeScreen.kt`.

## [1.2.1] - 2026-09-06

### Restored
- **Global Beta Feedback FAB & Jules Triage Pipeline**: Restored the floating action button (`FloatingFeedbackButton`) across all screens and the in-app `FeedbackDialog` modal that was lost during the AI Studio alignment commit `6a57283`.
- **Automatic Screen Capture & Diagnostics**: Feedback dialog captures the current screen bitmap, system diagnostics (OS version, device model, watchlist & vault counts), and dispatches issues directly to GitHub labeled `jules` and `jules-triage` via the shared Cloud Function feedback proxy.
- **Settings Beta Toggle**: Added "Enable Beta Feedback FAB" toggle under Settings -> AI/Local tab backed by `UserPreferencesManager`.

## [1.2.0] - 2026-09-06

### Fixed & Modernized
- **Navigation Renamed to "My Services"**: Restored "My Services" tab naming (with `Icons.Default.Subscriptions`) and updated headers across spend cards and empty states, recovering edits from commit `710a6ed`.
- **Movie Details Markdown Transformation**: Replaced raw monospace YAML frontmatter and markdown in `MovieDetailsBottomSheet` with rich `SynthesizedMovieInsights` (theme pills, cast badges, and styled insight cards).
- **Direct Explore AI Bridge**: Added "Discuss with Olivia & Gemini in Explore" action directly inside the movie details sheet.

### Added
- **Cinephile Taste Matrix**: Multi-segment visual taste distribution bar in the Explore tab calculated dynamically from the user's watched vault.
- **Gemini API Integration**: Connected live Gemini API key with runtime preference management in Settings.
- **1-Tap Recommendation Watchlist Addition**: Added instant `+ Watchlist` buttons on all Gemini recommendations in Explore.

## [1.1.0] - 2026-09-06

### Fixed
- **Watchlist Filter Default**: Restored default selection of the "My Services" filter chip (`filterOnlyMyServices = true`) on startup.
- **TMDB Metadata & Availability Sync**: Fixed API secrets resolution, endpoints, and data parsing so movie and TV posters, release years, ratings, and streaming provider mappings populate live in SQLite and the Compose UI.
- **WorkManager Name Collision**: Separated periodic background work name from immediate one-time work name and applied `ExistingWorkPolicy.REPLACE` to ensure manual sync requests execute without dropping.

### Added
- **Direct Coroutine Sync**: Added `syncWatchlistMetadata` in `StreamViewModel` to populate pending items immediately upon app launch and UI refresh button clicks.
- **Extended Provider Mappings**: Supported Criterion Channel, Peacock, Paramount+, Mubi, Shudder, Starz, BritBox, Tubi, Freevee, and Pluto TV.
- **In-App User Guide & Changelog**: Added `user_guide.html` and `changelog.html` assets for offline documentation access.

## [1.0.0] - 2026-09-04

### Added
- Initial Material 3 UI with dark mode support.
- Room database for offline-first title and watch history storage.
- Streaming ROI analytics and subscription cost tracker.
