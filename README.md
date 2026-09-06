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
1. **Watchlist:** Your upcoming queue with dynamic live title counts (`Watchlist • X Titles`, sticky summary `Showing X of Y titles`, and Bottom Nav badge counters). Features natural scrolling **Spotlight** carousel ("Ready to Stream") with a sticky Search bar, and an **Advanced Filters** modal sheet supporting multi-genre selection, 19 streaming platforms, minimum rating thresholds (6.0+ to 8.5+), release eras, and **Cinema Podcasts & Media Mentions** (*What Went Wrong*, *The Rewatchables*, *The Big Picture*, *Blank Check*, *How Did This Get Made?*) with main film vs any mention sub-filtering.
2. **Watched:** The **Cinephile Vault & Diary** (live count badge `Badge { 1104 }`). Tracks total films logged, estimated watch hours, average rating, and top genres. Displays a chronological month-by-month diary timeline with formatted watch dates (`Watched Sep 4, 2026`), user notes, and a toggleable Poster Wall Grid. Includes **Live Letterboxd RSS Sync** and **1-tap Letterboxd Google Sheet Sync** via Apps Script.
3. **My Services:** Track which streaming providers you actively pay for, your monthly spend, and real-world hourly return on investment. Tap any provider card to open the **Service Detail Bottom Sheet**: edit pricing/tier, set renewal day, track subscription tenure (months, days, start date), view watch time vs ROI $/hr, and launch 1-tap live deal searches on *The Streamable Deals*, *Slickdeals*, *Doctor of Credit*, and *Google Deals*.
4. **Explore:** Deep taste analytics featuring the Cinephile Taste Matrix, interactive synthesized film insights, and live chat with Olivia powered by local Ollama or Gemini Pro.

### ⚙️ Unified 4-Tab Settings Hub
Access all configuration options from the Top App Bar gear icon:
- **Profile & Letterboxd:** User display name, Letterboxd username, live RSS sync trigger, and Google Sheet webhook.
- **Services (19):** Toggle active subscriptions and customize monthly pricing across all 19 providers.
- **Guides & Docs:** Instant offline viewers for the in-app User Guide and Changelog.
- **Updates & System:** Built-in **GitHub OTA Auto-Updater** (checking releases, downloading APKs, and launching Android Package Installer) plus API secrets management.

### 💡 Beta Feedback & Living Backlog
- Global floating feedback button captures screen diagnostics.
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
- **[Using Nano Banana 2 to Design an Android Phone in One Prompt - Tech Advisor](https://share.google/KG4RVn9ROut4DWW4T)** (Relevance: Medium)
  - *Concepts/Tools:* LLM Engineering
  - *Action:* Review article for best practices on this project.
