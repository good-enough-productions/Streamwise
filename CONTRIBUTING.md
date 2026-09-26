# Contributing to Streamwise 🎬

First off, **thank you** for checking out Streamwise!  
Streamwise is an indie, open-source project created to help movie lovers maximize their watch time, eliminate subscription waste, and take back control of their watchlists.

Whether you're an experienced Android engineer, a cinephile who watches 10 movies a week, or someone who just got frustrated trying to find where a movie is streaming—**your perspective is valuable, and we welcome your contributions with open arms!**

---

## 🌟 No Coding Required! How Non-Developers Can Help

> **You do NOT need to write a single line of code to make Streamwise significantly better.**

A great app is built on real-world usage, thoughtful feedback, and honest impressions. Here are some of the most impactful ways you can help:

### 1. Tell Us What You Wish Existed
- Have an idea that would make managing your watchlist easier?
- Wish there was a specific filter (e.g., runtime under 90 minutes, certain studios, audio languages)?
- Have an idea for how the subscription rotation advisor could be smarter?
- [**Submit a Feature Request**](https://github.com/good-enough-productions/Streamwise/issues/new?template=feature_request.md) — just describe the idea in plain English!

### 2. Report Bugs & Weird UI Quirks
- Did text look squished or cut off on your phone screen?
- Did a movie fail to match with TMDB or show the wrong release year?
- Did a streaming service fail to open when you tapped "Watch Now"?
- [**File a Bug Report**](https://github.com/good-enough-productions/Streamwise/issues/new?template=bug_report.md) — don't worry about being overly technical; just tell us what phone you're using and what happened.

### 3. Keep Streaming Providers & Pricing Accurate
- Streaming platforms change plans, price points, and deep links frequently.
- If you notice a subscription price changed or a service launched a new tier, let us know so we can update the defaults!

### 4. Suggest Cinema Podcasts to Index
- Love a film podcast that isn't in our directory yet?
- Tell us the name and RSS feed, and we'll work on indexing its recommended titles so other cinephiles can filter by it!

---

## 📬 How to Submit Feedback

You have two easy ways to share feedback:

### Option 1: In the App (Instant)
1. Turn on **Enable Beta Feedback FAB** in **Settings (`⚙️`) > AI / System**.
2. A floating feedback button (`💬`) will appear in the corner of your screen.
3. Tap it anytime to write a message. You can optionally include a screenshot of what's currently on your screen!
4. Hit **Submit** — your note lands directly on our development backlog.

### Option 2: On GitHub
1. Head over to our [**Issues tab**](https://github.com/good-enough-productions/Streamwise/issues/new/choose).
2. Choose one of the templates:
   - **Feature Request**: For new ideas or improvements.
   - **Bug Report**: If something broke or looks off.
   - **General Feedback**: For questions, movie chat, or general thoughts.
3. Fill out the simple prompts and hit **Submit new issue**.

---

## 💻 For Developers & Code Contributors

If you'd like to contribute code, documentation, or unit tests, here is how to get started:

### Development Setup
1. **Fork the repository** on GitHub: `https://github.com/good-enough-productions/Streamwise`
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/<your-username>/Streamwise.git
   cd Streamwise
   ```
3. **Open the project** in Android Studio (Koala Feature Drop or newer recommended).
4. **Compile and run tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

### Coding Guidelines
- **UI Architecture**: All UI is written in declarative **Jetpack Compose** with Material 3 components.
- **Offline First**: All user data (watchlist, vault, logs, preferences) must persist in **Room SQLite** and remain 100% usable without an internet connection.
- **Privacy First**: Streamwise does not collect user telemetry, advertising IDs, or personal accounts. Keep network calls restricted to public APIs (TMDB, Letterboxd RSS) and user-configured webhooks.
- **Branch Naming**:
  - `feat/your-feature-name`
  - `fix/your-bug-fix`
  - `docs/your-doc-update`

### Submitting a Pull Request (PR)
1. Ensure your code compiles cleanly: `./gradlew assembleDebug`
2. Ensure unit tests pass: `./gradlew testDebugUnitTest`
3. Push your branch to your fork and open a Pull Request against `main`.
4. Describe what your PR changes, why it's needed, and include screenshots or GIFs for any UI changes.

---

## 🤝 Community Culture

Streamwise is a passion project built on enthusiasm for cinema and great indie software.  
- **Be respectful and kind**: We welcome participants of all experience levels and technical backgrounds.
- **No question is too simple**: If something in the documentation is confusing, that's a bug in our documentation, not a failing on your part!

Thank you for helping make streaming simpler, cheaper, and more fun for everyone! 🍿
