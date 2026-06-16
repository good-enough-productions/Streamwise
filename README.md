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
1. **Watchlist:** Your upcoming queue. Tap **Refresh (↺)** to sync latest metadata and Agent research.
2. **Watched:** Your historical library. Long-press to see Olivia's insights on why you loved it.
3. **ROI Stats:** Track how much value you're getting from Netflix, Hulu, etc., based on watch time.
4. **Agent (Chat):** Speak directly with Olivia. You can ask for recommendations or even **request new app features** (which she will submit to GitHub as real issues!).

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
