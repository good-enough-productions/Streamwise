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
1. **Watchlist (Queue):** Your unified movie and TV queue. Features **Instant Undo** (recovers accidentally deleted movies via an animated top banner and Snackbar action), **"Free to Me"** filter to hide paywalls, **Duration Chips** (`< 90m`, `< 120m`) for quick picks, and Olivia's synthesized cultural trivia and pre-watch notes.
2. **Watched (The Vault):** Your personal viewing diary. Tap any title to log a **0.5–5.0 star rating**, mark rewatches, and attribute subscriptions. Tap **"Export to Letterboxd"** for a 1-tap CSV export in standard Letterboxd format (`Downloads/letterboxd_import.csv`).
3. **ROI Stats (Churn Optimizer & Budget):**
   - **🎯 Watchlist Match Mode:** Ranks every streaming service by how many movies on your Watchlist are currently streaming on them. Features **"Best Opportunity to Subscribe"** and **"Safe to Pause"** opportunity banners, cost-per-movie metrics, and expandable movie title preview chips.
   - **📊 Spend & Usage Mode:** Real-time monthly burn rate calculations, hours watched, cost-per-hour efficiency, and underutilized subscription cancel candidate alerts.
   - **Quick Subscriptions Ribbon & Edit Sheet:** 1-tap active/paused toggle, price presets (`$0 Free`, `$5.99`, `$7.99`, `$9.99`, `$13.99`, `$15.49`, `$19.99`, `$22.99`), auto-expiring free trial countdowns, and a `+ Add Service` modal.
4. **Agent (Chat):** Speak directly with Olivia for recommendations, trivia, or feature planning.

### 📺 Universal TV Auto-Discovery & "Watch Now" Hub
- **Always-Visible "Play on TV":** TV playback is permanently available as the primary action. If a TV is discovered, it launches immediately; if multicast is filtered by your Wi-Fi router, tapping opens an interactive **TV Connect Dialog** to scan or enter your TV's IP address (e.g. *Settings > My Fire TV > About > Network*).
- **Universal Cast & App Chooser:** Added "Cast or Open with App…" to route playback to Google Cast / Chromecast devices or installed streaming video players via Android's native system picker.
- **Watch Session Tracking:** Internal runtime timer tracks TV viewing and prompts for check-in to log your rating and compute subscription ROI.

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
