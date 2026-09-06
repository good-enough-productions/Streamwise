# Changelog

All notable changes to Streamwise will be documented in this file.

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
