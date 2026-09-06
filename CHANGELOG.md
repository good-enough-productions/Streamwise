# Changelog

All notable changes to Streamwise will be documented in this file.

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
