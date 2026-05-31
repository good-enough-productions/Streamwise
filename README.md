<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/7d6cb489-a977-49ca-bd7c-0dbcff292f1a

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device

---

## CI/CD — Auto-build & Deploy to Phone

Every push to `main` triggers a GitHub Actions workflow that builds the debug APK and installs it wirelessly on your phone.

Because ADB needs to reach your phone over local Wi-Fi, the workflow runs on a **self-hosted runner** (your Windows machine). One-time setup below.

### 1 · Install the self-hosted runner

1. Go to your repo → **Settings → Actions → Runners → New self-hosted runner**
2. Select **Windows / x64** and follow the download + configure commands shown
3. Run `./run.cmd` once to verify it connects, then install it as a service:
   ```powershell
   ./svc.ps1 install
   ./svc.ps1 start
   ```
   The runner will now start automatically with Windows and pick up jobs in the background.

### 2 · Enable wireless debugging on your phone

1. **Settings → About phone** — tap **Build number** 7× to enable Developer Options
2. **Settings → Developer Options → Wireless debugging** — toggle on
3. Tap **Wireless debugging** → note the **IP address and Port** shown at the top (e.g. `192.168.1.42:5555`)
4. Pair once from your PC (only needed after a factory reset):
   ```powershell
   # Tap "Pair device with pairing code" on the phone, then:
   adb pair 192.168.1.42:<pairing-port>   # enter the 6-digit code shown
   adb connect 192.168.1.42:5555          # confirm it says "connected"
   ```

### 3 · Add repo secrets

In your repo → **Settings → Secrets and variables → Actions → Secrets**:

| Name | Value |
|------|-------|
| `GEMINI_API_KEY` | your Gemini API key |
| `TMDB_API_KEY` | your TMDB API key |

> No IP/port variables needed — the runner discovers your phone automatically via ADB wireless debugging (mDNS).

### 4 · Push and watch it deploy

Push any commit to `main`. Visit the **Actions** tab to watch the build and see the APK install live on your phone. You can also trigger it manually with **Run workflow**.

The built APK is also uploaded as a downloadable artifact (retained 7 days) in case you want to sideload it on another device.
