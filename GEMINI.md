# Streamwise: Architectural Source of Truth

This document serves as the permanent record of the Streamwise system architecture, AI integration workflows, and the self-evolution pipeline.

## 1. System Philosophy
Streamwise is a **Privacy-First, Local-Heavy** cinematic management system. The phone acts as a high-performance UI terminal, while high-reasoning tasks are delegated to local infrastructure (Laptop/Server).

## 2. Technical Stack
- **Mobile:** Android (Kotlin, Jetpack Compose)
- **Database:** Room (SQLite) - Currently on Schema **v6**
- **AI Brain:** Local **Ollama** instance running **Gemma 4 (`gemma4:e2b`)**
- **Connectivity:** Local Wi-Fi (REST via Retrofit + SSDP Discovery)

## 3. AI Research Pipeline (Synthesis 4.0)
The app features an autonomous agent named **Olivia**.

### How Olivia Thinks:
1. **Context Bundling:** On every chat/request, the app injects the user's latest 20 watched movies and current watchlist into the system prompt.
2. **Personal Relevance:** Gemma 4 calculates a `Match: X/10` score based on historical themes and cinematographer/talent overlaps.
3. **Local Bridge:** Requests are sent to the default laptop IP `192.168.86.217:11434`.

### Self-Evolution Workflow:
Users can suggest app improvements directly to the chatbot.
1. **Detection:** Gemma 4 recognizes app feedback and outputs a `<github_issue>` JSON block.
2. **Interception:** The Android `StreamViewModel` intercepts this block.
3. **Execution:** Using a **GitHub PAT**, the app creates a real Issue on `good-enough-productions/Streamwise`.
4. **Autonomous Coding:** GitHub-integrated agents (e.g., Sweep.dev) can be configured to read these issues and open Pull Requests.

## 4. Universal Casting Engine
- **Protocol:** SSDP (Simple Service Discovery Protocol).
- **Functionality:** Scans local network for `MediaRenderer` or `DIAL` targets (Smart TVs, Plex, Kodi).
- **Operation:** Triggers a launch intent directly to the discovered device IP.

## 5. Data Sovereignty & Legacy
- **Obsidian Sync:** The app can export the entire library as structured Markdown with YAML frontmatter.
- **Location:** Saved to phone's `Downloads/StreamwiseVault/`.
- **Format:** Compatible with Obsidian for long-term cinematic archival.

## 6. Active Roadmap & Backlog
- [x] **v6:** Personal Cinematic Vault (400+ titles)
- [x] **v7:** Agentic Chatbot UI & Markdown Export
- [x] **v8:** Self-Evolving GitHub Integration
- [ ] **TODO:** Agentic Parse (Messy Notes Extractor)
- [ ] **TODO:** Analysis Hub (Genre Heatmaps)
- [ ] **TODO:** OTA (Over-The-Air) self-update mechanism
