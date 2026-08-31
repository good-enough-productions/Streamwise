# Streamwise Developer Guide

## Architecture Overview
Streamwise is a native Android application built entirely with **Kotlin** and **Jetpack Compose**. It aims to solve the "subscription fatigue" problem by combining a Watchlist manager with a Financial ROI tracker.

### Key Components

1. **Local Database (Room)**
   - **AppDatabase.kt**: The SQLite database powered by Android Room.
   - **Entities**:
     - MediaItem: Represents a movie or TV show.
     - StreamingProvider: Represents a platform (Netflix, Hulu, etc.). Custom pricing and trial expiration are stored here to drive the ROI math.
     - WatchSession: Every time an item is checked off, a session is logged to track hours spent on a given platform.
   - **MediaDao.kt**: Contains raw SQL queries. Specifically, getMonthlyUsageStats joins sessions and providers to figure out the "Cost Per Hour" burn rate.

2. **Network & APIs (Retrofit)**
   - **TMDB (The Movie Database)**: Used for searching titles and fetching metadata (posters, descriptions).
   - **Watchmode**: The crux of the app. It resolves a TMDB ID into actual streaming provider availability (e.g., "The Matrix" is on "Max").
   - **Ollama AI**: Talks directly to a local, self-hosted LLM (like llama3) running on the user's home network for private chat recommendations.

3. **UI (Jetpack Compose)**
   - **HomeScreen.kt**: A monolithic Compose file that handles the primary tab routing.
     - *Watchlist Tab*: Displays saved items, filtered by My Services.
     - *Search Tab*: Connects to TMDB.
     - *ROI Tab*: The budget dashboard. Calculates Potential Savings for underutilized platforms.
     - *Chat Tab*: Connects to Ollama.
   - **GuideDialog.kt**: Renders user_guide.html and changelog.html from the ssets folder directly inside an Android WebView so users always have offline access to docs.

### Vibe Coding Notes for Agents
- Do not introduce massive external UI libraries unless necessary. Compose standard Material3 is preferred.
- All docs (user_guide.html, changelog.html, and this DEVELOPER_GUIDE.md) MUST be updated when releasing new features. See ibe-coding-docs.md in the global rule set.
