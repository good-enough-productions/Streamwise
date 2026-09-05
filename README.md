<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Streamwise: Your Agentic Streaming Companion

Streamwise is a self-evolving Android application designed to track your cinematic journey across all streaming services. It features **"Olivia,"** an advanced local research agent, and a deep ROI-tracking system for your subscriptions.

---

## 🚀 Quick Start Guide

### 1. Initial Setup (Android Studio)
1. **Open Project:** Launch Android Studio and open the `Streamwise` folder.
2. **Environment Config:** Create a `.env` file in the root directory:
   ```env
   GEMINI_API_KEY=your_gemini_key
   TMDB_API_KEY=your_tmdb_key
   ```
3. **Build & Run:** Hit the green "Play" button to install on your phone or emulator.

---

## 🤖 The "Olivia" Agent & Local AI
Olivia provides "Agentic Research Strategies" for every movie on your watchlist. She runs locally on your machine for maximum privacy.

### Setup Ollama (Local Brain)
1. **Install Ollama:** Download from [ollama.com](https://ollama.com).
2. **Download Model:** Open your terminal and run:
   ```bash
   ollama run gemma4:e2b
   ```
3. **Configure the App:**
   - Go to the **Settings** (Gear icon) in the app.
   - Enter your laptop's **Local IP Address** (e.g., `192.168.86.217`).
   - Olivia will now start researching your movies in the background!

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
1. **Watchlist (Queue):** Your unified queue across movies, TV series, and podcast recommendations. Features **Decluttered Top App Bar** with overflow menu (User Guide, Letterboxd Import/Export), **Unified Horizontal Quick Filter Ribbon** (`All`, `✓ My Services`, `Free w/ Ads`, `🎬 Movies`, `📺 TV`, `🎙️ Podcasts`, `🗓️ Radar`, `< 90m`, `< 120m`, `Surprise Me`, `Letterboxd`), **Expandable Filter Drawer** for granular provider and genre filtering via the Tune button, **First-Run Onboarding Wizard** (interactive 3-step setup), **Persistent "Free to Me" default**, **Instant Undo**, and responsive cards with expandable synopses.
2. **Watched (The Vault):** Your personal viewing diary. Tap any title to log a **0.5–5.0 star rating**, mark rewatches, and track **TV show progress with Season/Episode counters**. Full two-way compatibility with **Letterboxd Import & Export** (`Downloads/letterboxd_import.csv`).
3. **My Services (Subscriptions & Churn Optimizer):**
   - **🎯 Watchlist Match Mode:** Ranks every streaming service by how many movies on your Watchlist are currently streaming on them. Features **"Best Opportunity to Subscribe"** and **"Safe to Pause"** opportunity banners, cost-per-movie metrics, and expandable movie title preview chips.
   - **⚡ Renewal Radar & 1-Click Cancellation:** Displays proactive countdown badges (e.g. *Renews in 3 days*) before billing cycles, with single-tap direct deep-links into official cancellation management portals (Netflix, Max, Disney+, Hulu, Paramount+, Criterion, Apple TV+, Prime, Peacock).
   - **📊 Spend & Usage Mode:** Real-time monthly burn rate calculations, hours watched, cost-per-hour efficiency, and underutilized subscription cancel candidate alerts.
   - **Quick Subscriptions Ribbon & Edit Sheet:** 1-tap active/paused toggle, price presets (`$0 Free`, `$5.99`, `$7.99`, `$9.99`, `$13.99`, `$15.49`, `$19.99`, `$22.99`), auto-expiring free trial countdowns, and a `+ Add Service` modal.
4. **Olivia AI (Concierge & Chat):** Powered by **Google Gemini 2.0 Flash** with `Icons.Default.AutoAwesome` sparkle icon for sub-second cloud-native recommendations and streaming synthesis (~$0.015/user/mo unit economics), with optional toggle to private local Ollama in Settings. Features 1-tap quick action prompt chips (*"What to watch tonight?"*, *"Which subscription to cancel?"*, *"When does Severance return?"*) for instant conversational guidance. Speak directly with Olivia for recommendations, trivia, or feature planning.

### ⭐ Streamwise Pro & Commercial Tiers
- **Free Tier:** Supports up to 2 active streaming services, manual title addition, watched history logging, and basic queue filtering.
- **Streamwise Pro ($3.99/mo or $39.99/yr):** Unlimited streaming services, 1-tap Letterboxd sync, Renewal Radar cancellation shortcuts, full TV show season/episode tracking, curated film podcast recommendations, and background availability alerts.
- **New Availability Push Alerts:** Periodic background sync detects when a title on your watchlist becomes streamable on one of your active subscriptions and fires a direct tap-to-watch notification.

### 📺 Native TV Companion, Auto-Discovery & "Watch Now" Hub
- **Zero-Latency Fire TV Companion (`TvCompanionService`):** Runs an embedded HTTP receiver on port 8998 on your Fire TV or Android TV. When you tap **"Play on Fire TV"** from the phone, it wakes the TV via screen wake-lock and natively launches the installed streaming app (Netflix, Disney+, Hulu, Prime Video, Tubi, Pluto TV, YouTube) with a search query for the movie, while automatically starting your watch timer.
- **Subnet Port Sweep Auto-Discovery:** Bypasses home router multicast filters by concurrently scanning local subnet IP ports (5555, 8008, 8009, 8998) to discover *Danny's Fire TV* (192.168.86.202) and Google Cast devices in < 200ms.
- **TV Connect Dialog:** Features live spinning discovery indicators, progress feedback, and 1-tap manual IP entry.
- **Universal Cast & App Chooser:** Added "Cast or Open with App…" to route playback to Google Cast / Chromecast devices or installed streaming video players via Android's native system picker.
- **Watch Session Tracking & Check-In:** Internal timer tracks TV viewing and prompts for check-in upon returning to the app to log your rating and compute subscription ROI.

### ☁️ $0/mo Master Google Sheet Cloud Ledger
- Integrated Google Apps Script serverless webhook deployed and baked into app settings. Two-way cloud sync with Google Sheets for watchlist, history, and automated podcast recommendation ingestion (`scrape_podcast_recs.py`).

### 🐞 "Do It Now" Autonomous Feedback FAB
- Floating action button on every screen captures Compose screenshots, gathers device diagnostics, and creates GitHub issues labeled `jules-triage` for autonomous AI maintenance.


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
- **[Using Nano Banana 2 to Design an Android Phone in One Prompt - Tech Advisor](https://share.google/KG4RVn9ROut4DWW4T)** (Relevance: Medium)
  - *Concepts/Tools:* LLM Engineering
  - *Action:* Review article for best practices on this project.
