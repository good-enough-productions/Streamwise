<div align="center">

# 🎬 Streamwise
### Maximize watch hours. Minimize streaming costs.

A free, private, offline-first Android app for movie lovers and cinephiles.  
Track your watchlist, see where films are actually streaming, sync with Letterboxd, and discover which subscription is worth keeping this month.

[![Latest Release](https://img.shields.io/github/v/release/good-enough-productions/Streamwise?color=4CAF50&label=Latest%20Release)](https://github.com/good-enough-productions/Streamwise/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-brightgreen.svg)](https://developer.android.com)
[![Letterboxd Ready](https://img.shields.io/badge/Letterboxd-Sync%20Ready-ff8000.svg)](https://letterboxd.com)
[![Privacy First](https://img.shields.io/badge/Privacy-100%25%20Offline%20Local%20DB-9C27B0.svg)](#-100-free--privacy-first)

---

</div>

## 🍿 Why Streamwise?

How many times have you sat on your couch, scrolled through streaming menus for 45 minutes, found nothing, and realized you're paying **\$80+ every month** for five different subscriptions you barely use?

Streaming has become fragmented, expensive, and overwhelming. **Streamwise** is built to put you back in control:

1. **Centralize your queue**: Keep everything you actually want to watch in one clean, beautiful place.
2. **Eliminate subscription waste**: See exactly how many movies on your watchlist are streaming on each service.
3. **The Single-Service Rotation strategy**: Rotate services month-to-month based on your watchlist volume and pocket **\$50–\$70/month in savings**.
4. **Zero friction**: Tap a movie, hit **Watch**, and it launches straight into the provider's Android app (Netflix, Max, Prime Video, Hulu, Disney+, Tubi, Criterion, etc.).

---

## ⚡ Quick Start: Install in 30 Seconds

Streamwise is completely open-source and free. You don't need Android Studio or programming knowledge to use it!

### Option A: Direct APK Download (Easiest)
1. Grab the latest **`app-debug.apk`** from [**GitHub Releases**](https://github.com/good-enough-productions/Streamwise/releases/latest).
2. Open the downloaded file on your Android phone and tap **Install** *(if prompted, allow installing from your browser)*.
3. Launch Streamwise and start browsing!

### Option B: Auto-Updates with Obtainium (Recommended)
If you use [**Obtainium**](https://github.com/ImranR98/Obtainium) to manage open-source Android apps:
1. Open Obtainium and tap **Add App**.
2. Paste: `https://github.com/good-enough-productions/Streamwise`
3. Tap **Add**. Obtainium will install the APK and automatically notify you when new updates land!

### Option C: Built-in In-App Updates
Streamwise includes an in-app updater. Whenever you open **Settings (`⚙️`) > Updates & System**, you can check for the latest release and update directly within the app.

---

## ✨ Features You'll Love

### 🚀 1-Minute First-Launch Setup Wizard
- **Zero Confusion Onboarding**: When you open Streamwise for the first time, a friendly 3-step setup wizard introduces the app, connects your Letterboxd account, helps you grab a free TMDB API key in 1 tap, and selects your active streaming services.
- **Actionable Empty States**: No intimidating blank screens. The Watchlist and Watched tabs feature warm welcome cards with 1-tap buttons to import Letterboxd CSVs, sync RSS, add movies manually, or re-run the setup guide anytime.
- **Reorganizable Settings Hub**: Essential cinephile controls are front-and-center, with developer and advanced tools tucked into a collapsible card.

### 🔄 The Single-Service Rotation Advisor
- **See where your watchlist actually lives**: View live counts of queued movies per streaming platform (e.g., `Max: 95 Titles`, `Prime Video: 385 Titles`).
- **Cost-per-hour and ROI**: Track your monthly spend and see what you're paying per hour of entertainment.
- **Smart cancellation guidance**: Identifies the service with the deepest backlog so you can binge it for a month, cancel it, and rotate to the next one.

### 📱 Zero-Intermediary Direct Launch
- Stop typing movie titles into sluggish TV search bars.
- Tapping **Watch Now** resolves deep links directly into native Android streaming apps across 15+ services (Netflix, Max, Prime Video, Disney+, Hulu, Tubi, Apple TV+, Criterion Channel, Peacock, Paramount+, and more).

### 📜 Curated & 3rd-Party Lists Hub (Zero-Pollution Browsing)
- **Browse Canonical Cinema Without Backlog Bloat**: Explore premier curated lists—Oscar Best Picture Winners (1927–2024), AFI's 100 Greatest American Films, Sight & Sound Top 50, Criterion Channel Essentials, IMDb Top 50 Classics, and podcast essentials (*The Rewatchables*, *The Big Picture*, *Blank Check*)—without dumping hundreds of foreign movies into your personal database.
- **Triple-State Real-Time Badges**: Every single film across any 3rd-party list dynamically reflects your personal library:
  - `✓ Watched` badge + your personal Letterboxd diary star rating.
  - `📌 In Queue` badge + 1-tap ⭐ Priority star and watchlist toggle.
  - **Streaming availability** matched against your active subscriptions with a 1-tap `▶ Watch` launcher.
- **Ad-Hoc Letterboxd List Viewer**: Paste any public Letterboxd list URL or newline titles to preview the list in-memory. Cross-reference what you've seen, what's in your queue, and where to stream the rest without database clutter.

### ⚡ Watchlist Backlog Triage Mode (Tame 1,000+ Film Queues)
- **Couch-Friendly Rapid-Fire Review**: When your watchlist swells to 1k+ titles, deciding what to watch becomes paralyzing. Tap **⚡ Triage** to enter a focused one-by-one card stack.
- **4 Instant 1-Tap Actions**:
  - `⭐ Focus Queue`: Elevates the film to your starred priority queue.
  - `✓ Already Watched`: Directly records the title into your Watched Vault diary.
  - `✕ Not Interested`: Cleans the film from your backlog.
  - `⏩ Skip`: Leaves the title in backlog and moves to the next.

### 📋 Letterboxd Integration
- **Live RSS Sync**: Enter your Letterboxd username to pull in your latest diary logs and ratings automatically—no API keys required.
- **CSV & ZIP Imports**: Import your `watchlist.csv` and `watched.csv` data in seconds so you never have to start from scratch.
- **Two-Way Export**: Export your queue anytime to standard CSV or linked Markdown notes for personal vaults like Obsidian.

### 🎙️ Cinema Podcast Filters
Love hearing film analysis after you watch? Streamwise indexes movies discussed on premier cinema podcasts:
- *The Rewatchables*
- *The Big Picture*
- *Unspooled*
- *How Did This Get Made?*
- *What Went Wrong*
- *Blank Check*

Use the **Podcasts & Media Mentions** filter to instantly surface queued films that have an accompanying podcast episode waiting for you!

### 📊 Cinephile Vault & Viewing Analytics
- **Decade & Era Breakdown**: 100% of your watched diary categorized across cinema history (Pre-1970s, 70s New Hollywood, 80s, 90s, 2000s, 2010s, 2020s).
- **Auteur & Performer Tracking**: Ranked counts and ratings across your most-watched directors (Coen Brothers, Nolan, Scorsese, Fincher, Spielberg) and actors.
- **Blind Spot Finder**: Identifies eras or genres missing from your diary and highlights ready-to-stream classics from your watchlist to fill the gaps.
- **Actor Age at Release**: In the movie details sheet, see how old each actor was when the film was released (`🎂 Age 38 at release`).

### 🔒 100% Free & Privacy-First
- **No accounts or sign-ups required**.
- **No ads, banners, or tracking pixels**.
- **100% offline-first**: Your watchlist and watch history live in a local SQLite database directly on your phone.

---

## 💬 Community & Feedback: Everyone Is Welcome!

> **You do NOT need to write code to contribute to Streamwise!**

Whether you're a movie buff, a casual streamer, or a UI enthusiast, your feedback is what shapes this app. We'd love your help with:
- **Feature Ideas**: What would make your streaming life easier?
- **Streaming Service Quirks**: Did a streaming app update its deep link or change its plan pricing?
- **Podcast Suggestions**: Have a favorite movie podcast we should index next?
- **Bug Reports**: Did something look weird or break on your specific phone model?
- **UI / UX Polish**: Ideas to make buttons, cards, or browsing feel smoother.

### How to share your thoughts:
- 💡 **In the App**: Tap the **Floating Feedback Button** anywhere in the app to send your thoughts (with an optional screenshot) directly to our backlog.
- 🐛 **On GitHub**: Open a quick ticket in our [**Issues section**](https://github.com/good-enough-productions/Streamwise/issues/new/choose). We have friendly, non-technical templates ready for you!
- 📖 **Contribution Guide**: Check out our [**CONTRIBUTING.md**](./CONTRIBUTING.md) for more details.

---

<details>
<summary><b>🛠️ Developer Setup & Architecture (Click to expand)</b></summary>
<br>

### Tech Stack
- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose with Material 3 Design
- **Architecture**: MVVM + Clean Architecture Repository Pattern
- **Persistence**: Room Database (SQLite) with multi-version migration suite
- **Async & Reactive**: Kotlin Coroutines & Flow
- **Background Work**: AndroidX WorkManager (metadata & provider periodic refresh)
- **Deep Linking**: Android intent resolution across native provider packages

### Building from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/good-enough-productions/Streamwise.git
   cd Streamwise
   ```

2. **Configure API Keys (Optional)**:
   Create a `.env` file in the root directory if you want live TMDB poster artwork and availability metadata:
   ```env
   TMDB_API_KEY=your_tmdb_v3_api_key
   GEMINI_API_KEY=your_gemini_api_key
   ```
   *(Note: You can also enter API keys directly in the in-app Settings menu on your phone!)*

3. **Build the Debug APK**:
   ```powershell
   ./gradlew assembleDebug
   ```
   The compiled APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Run Unit Tests**:
   ```powershell
   ./gradlew testDebugUnitTest
   ```

5. **Deploy to Device via Wireless ADB**:
   Ensure your phone has Wireless Debugging enabled on the same Wi-Fi network, then run:
   ```powershell
   ./deploy.ps1
   ```

### Optional: Local AI Film Companion
Streamwise features an optional private AI film companion that can run against local models via [Ollama](https://ollama.com) on your home Wi-Fi:
- Supports models like `qwen2.5-coder:7b`, `gemma4:e2b`, or `llama3`.
- Configure your local workstation IP address under **Settings (`⚙️`) > Updates & System**.
- Responses cite the physical node that generated them with zero data leaving your home network.

### Mesh Node Architecture:
1. **Desktop Fry (Primary Compute Anchor):**
   - Address: `192.168.86.27:11434`
   - Model: `qwen2.5-coder:7b` (High throughput @ 48.2 tok/s)
   - Handles heavy contextual synthesis, deep-wiki research cards, and conversational analysis.
2. **Laptop Maze (Edge Field Unit):**
   - Address: `192.168.86.217:11434`
   - Model: `gemma4:e2b` / `gemma4:26b` (@ 26.5 tok/s)
   - Instant failover unit when roaming on Wi-Fi or when the desktop anchor is asleep.
3. **Dynamic Discovery & Hardware Attribution:**
   - Streamwise queries node capabilities via `/api/tags` and dynamically routes requests using the loaded model.
   - Assistant responses cite the physical machine that generated them (e.g. `Synthesized by Olivia via Desktop Fry [qwen2.5-coder:7b]`).
   - Interactive latency pinging, active model badges, and connection tests are available in Settings (Tab 3: Updates & System).

</details>

---

## 📱 Wireless Debugging & Auto-Deploy
Streamwise is built for rapid iteration. Every push to GitHub triggers an automatic install on your physical phone over your home Wi-Fi.

### Enable Wireless Debugging
1. **Developer Options:** Go to *Settings > About Phone* and tap **Build Number** 7 times.
2. **Toggle On:** Go to *Settings > System > Developer Options > Wireless Debugging* and turn it **ON**.
3. **Stay Stable:** 
   - Keep the phone **plugged into a charger** to prevent power-saving from killing the connection.
   - Keep the **Wireless Debugging screen open** while the Agent is deploying updates.

---

## 🎬 Master the Interface

### The 4 Main Tabs
1. **Watchlist:** Your upcoming queue with dynamic live title counts (`Watchlist • X Titles`, sticky summary `Showing X of Y titles`, and Bottom Nav badge counters). Features a **Couch-First 3-Column Poster Grid** (2:3 aspect ratio, gold star rating badge, orange quick-play button, and provider pill) paired with a persistent **View Mode Toggle** in the Top App Bar allowing instant switching between Poster Grid and Detailed Cards. Includes a **Watchlist Priority Star System (⭐)** allowing you to tag high-excitement priorities via 1-tap star toggling on Poster Grids, Media Cards, Spotlight Cards, and Movie Details. Includes **Letterboxd-Style Top Sub-Navigation Tabs** with dynamic count badges (*Ready on Subs*, *All Saved*, *⭐ Priorities*, *Under 100m*, *Podcast Picks*) and a `⭐ Priorities First` sort selector. Tapping the orange **Watch** button triggers **Zero-Intermediary Deep Linking (`StreamingAppLauncher`)**, resolving native Android packages, deep link URIs, and search intents across 15+ services (Netflix, Prime Video, Max, Disney+, Hulu, Tubi, Apple TV+, etc.) while seamlessly activating the background check-in watcher. Features a **Collapsible Spotlight** carousel ("Ready to Stream") with gold `PRIORITY` badges that collapses into a sleek 36dp header bar to save 1/3 of the screen, a sticky Search bar, and a **Streamlined Filter Ribbon** (`[✓ My Services]`, `[Free w/ Ads]`, `[Filters (X)]`). Deep multi-select filters are organized cleanly in the **Advanced Filters** sheet, supporting multi-genre selection, 19 streaming platforms, minimum rating thresholds (6.0+ to 8.5+), release eras, and **Cinema Podcasts & Media Mentions** (*The Rewatchables*, *The Big Picture*, *Unspooled*, *How Did This Get Made?*, *What Went Wrong*, *Blank Check*) backed by an offline catalog of 2,300+ films with $O(1)$ indexed title lookup. All additions and syncs are strictly gated against TMDB with automated non-movie scrubbing (`MediaTitleSanitizer`) and 50MB CursorWindow support on Room v10. Opening any movie's **Movie Details** sheet reveals the **Actor Age at Release** horizontal scrollbar, displaying each actor's profile photo, character name, and exact age when the movie hit theaters (`🎂 Age 38 at release`) with deceased status indicators (`🕊️`).
2. **Watched:** The **Cinephile Vault & Diary** (live count badge `Badge { 1131 }`). Features a streamlined layout dedicating over 90% of screen space to movie browsing with top sub-navigation tabs (*Watched Diary*, *Highest Rated*, *Analytics*), a slim sticky search/sort/view-mode bar, and a collapsible summary card (`🎬 1,131 Films • ~2,073h • ★ 7.1 • ~7.2/mo`) with visual decade trend capsules. Every film card features verified release years (`Title (Year)`), extracted star ratings, and watch date badges. Features a prominent 1-tap **Sync Letterboxd & Google Sheet** button alongside the full **Cinephile Viewing Analytics Hub**:
   - **Zero Unknown Cinema Eras:** 100% of all 1,131 logged films resolved into accurate decades: 2000s (34.3%), 2010s (25.9%), 1990s (18.6%), 2020s (15.0%), 1980s (4.3%), 1970s (1.3%), and Pre-1970s (0.5%) with 0% unknown.
   - **Top Directors & Auteurs:** Ranked film counts across 20+ prominent directors (Coen Brothers, Nolan, Scorsese, Fincher, Tarantino, Spielberg, etc.) with star ratings and sample title tags.
   - **Top Actors & Screen Presence:** Performer tracking across top stars (Tom Cruise, Matt Damon, Leonardo DiCaprio, Brad Pitt, Robert De Niro, etc.).
   - **Cinephile Blind Spots & Watchlist Integration:** Cross-references vault gaps (Pre-1970s, 70s New Hollywood, Westerns, Documentaries) against your 1,100+ Watchlist queue to surface queued classics ready to stream.
   - **TMDB Rating Enrichment:** 1-tap vault rating backfill to enrich unrated diary items.
   - **Letterboxd Sync & Multi-Way Tools:** Live Letterboxd RSS sync, local CSV/ZIP imports (`watchlist.csv`, `watched.csv`, `diary.csv`, `ratings.csv`, and ZIP exports), two-way CSV export, and Google Sheet sync via Apps Script ($0/mo).
3. **My Services:** Track which streaming providers you actively pay for, your monthly spend, and real-world hourly return on investment. Features the **Priority-Aware Single-Service Rotation Advisor & Expected Value Card**, using your actual watch velocity (~7.2 films/mo) to compute effective cost per film ($/film), potential monthly savings (up to ~$70/mo or ~$845/yr) from single-service rotation, and identifying the platform with the deepest **Priority** and watchlist backlog (e.g. prioritizing 5 starred titles over 30 unstarred ones). Provider cards display live **Priority Badges & Watchlist Volume** (`⭐ 2 Priority • 🎬 30 Watchlist Titles`). Tap any provider card to open the **Service Detail Bottom Sheet**: inspect the exact queued watchlist movies on that platform, filter to `⭐ Priorities Only`, toggle stars directly, edit pricing/tier, set renewal day, track subscription tenure (months, days, start date), view watch time vs ROI $/hr, and launch 1-tap live deal searches on *The Streamable Deals*, *Slickdeals*, *Doctor of Credit*, and *Google Deals*.
4. **Explore:** Modular 4-subtab cinematic culture hub designed to eliminate excessive scrolling:
   - `✨ AI & Taste`: Dynamic Cinephile Taste Matrix, personalized thematic synthesis with core themes and auteur ties, tailored picks with 1-tap `+ Watchlist`, and quick conversation prompts.
   - `💬 Olivia — AI Film Companion`: Dedicated fullscreen conversational cinema companion tailored to your personal vault history and taste matrix, featuring an ergonomic inline composer and suppressed floating feedback collisions during active chats.
   - `🎙️ Podcasts Hub`: Filterable podcast directory (*The Rewatchables*, *The Big Picture*, *Unspooled*, *How Did This Get Made?*, *What Went Wrong*, *Blank Check*) with episode counts, `▶️ Listen` and `💬 Ask Olivia`.
   - `📰 Film News Hub`: Live cinephile dispatches categorized by *Box Office*, *Festivals*, *Auteurs*, and *Streaming* with 1-tap `💬 Discuss with Olivia`.

### ⚙️ Unified 4-Tab Settings Hub
Access all configuration options from the Top App Bar gear icon:
- **Profile & Letterboxd:** User display name, Letterboxd username (`scriptedmind`), live RSS sync trigger, local CSV/ZIP file picker, export for Letterboxd, 1-tap direct web links (`watchlist/export/`, `settings/data/`, `import/`), 1-tap **Gemini Spark Podcast Recs** sync, and Google Sheet webhook.
- **Services (19):** Toggle active subscriptions and customize monthly pricing across all 19 providers.
- **Guides & Docs:** Instant offline viewers for the in-app User Guide and Changelog.
- **Updates & System:** Built-in **GitHub OTA Auto-Updater** (checking releases, downloading APKs, and launching Android Package Installer) plus API secrets management.

### 💡 Beta Feedback & Living Backlog
- Global floating feedback button captures screen screenshots and device telemetry.
- **Resilient Multi-Tier Webhook:** Feedback routes through authenticated Cloud Run and Google Apps Script endpoints with sticky error alerts and Toast confirmations.
- Feedback lands on the GitHub backlog by default; check **Assign to Jules (Autonomous AI)** in the feedback dialog for autonomous AI code fixes.

---

## 🛠 Troubleshooting

### "Sync Pending" or No Images?
- Ensure your **TMDB API Key** is set in the Settings tab.
- Tap the **Refresh (↺)** icon in the Watchlist.
- If you just cleared app data, it may take 1-2 minutes for Olivia to re-process the list.

### Agent Connection Blocked?
- We have enabled **Cleartext Traffic** for local IPs. Ensure your phone and laptop are on the **same Wi-Fi network**.
- Verify that Ollama is actually running in your laptop's system tray.
- If response times are slow, we've extended the timeout to **5 minutes** to support complex local model reasoning.

---

## 🏗 Architectural Architecture
For deep-dive documentation on the "Self-Evolution" loop, GitHub integration, and Room database schemas, see [**GEMINI.md**](./GEMINI.md).


## Recommended Enhancements (from Scraped Articles)

The following opportunities were identified during a review of scraped technical articles:

- **[Gemini task automation is slow, clunky, and super ]()** (Relevance: High)
  - *Concepts/Tools:* Model Context Protocol (MCP)
  - *Action:* Review article for best practices on this project.
- **[Tom's Guide- Google just unlocked 'Agent Mode' for]()** (Relevance: High)
  - *Concepts/Tools:* Agentic Design Patterns, Home Automation, Vibe Coding, Home Assistant, NotebookLM
  - *Action:* Review article for best practices on this project.
- **[The Verge- Why does the Googlebook exist-]()** (Relevance: High)
  - *Concepts/Tools:* Vibe Coding
  - *Action:* Review article for best practices on this project.
- **[Stop Wasting Tokens- A Smarter Alternative to JSON]()** (Relevance: Medium)
  - *Concepts/Tools:* Agentic Design Patterns, LLM Engineering, Vibe Coding, Claude Code, FastAPI, Python
  - *Action:* Review article for best practices on this project.
- **[How to Use Google Chrome’s New AI-Powered ‘Skills’ | WIRED](https://share.google/YFweRkIOJUymRBi5r)** (Relevance: Medium)
  - *Concepts/Tools:* Agentic Design Patterns, Vibe Coding
  - *Action:* Review article for best practices on this project.
- **[I Just Vibe Coded a Global Mass Surveillance Site in 2 Hours With OpenAI's Codex. It Was Terrifyingly Easy | PCMag](https://share.google/uSbjV8IzAeXDMV2vb)** (Relevance: High)
  - *Concepts/Tools:* Agentic Design Patterns, Vibe Coding, Claude Code
  - *Action:* Review article for best practices on this project.
- **[Google Brings Enterprise AI Agent Tools Under One Roof](https://share.google/NO74YHvcEZx0SozJf)** (Relevance: Medium)
  - *Concepts/Tools:* Model Context Protocol (MCP), Agentic Design Patterns, LLM Engineering
  - *Action:* Review article for best practices on this project.
- **[Self-Hosted LLMs in the Real World: Limits, Workarounds, and Hard Lessons - KDnuggets](https://share.google/1ovRMys6HeqsNJKYO)** (Relevance: High)
  - *Concepts/Tools:* Agentic Design Patterns, LLM Engineering, Local AI & Self-Hosting, Vibe Coding, Ollama, Claude Code, Python
  - *Action:* Review article for best practices on this project.
---

## 📄 License & Trademark Notice

- **Source Code**: The code in this repository is open for personal use, inspection, local building, and community contributions under the [MIT License with Trademark & Platform Distribution Reservations](LICENSE).
- **Trademark & Brand Protection**: The name **Streamwise**, logos, app iconography, and visual identity are exclusive trademarks of **Good Enough Productions**. Re-distributing, republishing, or commercializing this application on mobile app stores (including the Google Play Store) under the Streamwise name or branding is strictly prohibited without prior written authorization.
- **Official Distribution**: Official releases are published exclusively by Good Enough Productions on [GitHub](https://github.com/good-enough-productions/Streamwise/releases) and (in the future) on the Google Play Store.

