# Changelog

All notable changes to Streamwise will be documented in this file.

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
