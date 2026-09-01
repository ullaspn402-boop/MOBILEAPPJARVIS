# JARVIS — AI Voice Agent for Android

<div align="center">
  <img width="1200" height="475" alt="JARVIS Banner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

> **JARVIS** is a production-scale personal AI voice assistant for Android — built with Jetpack Compose, Gemini AI, Room, and a custom on-device intent router.

---

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Setup](#setup)
- [Build the APK](#build-the-apk)
- [Install & Run](#install--run)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Known Fixes Applied](#known-fixes-applied)
- [Architecture](#architecture)
- [GitHub Actions CI/CD](#github-actions-cicd)

---

## Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Android Studio | Hedgehog 2023.1.1+ | [developer.android.com/studio](https://developer.android.com/studio) |
| JDK | 17 | Bundled with Android Studio |
| Android SDK | API 35 | Via Android Studio SDK Manager |
| Gradle | 8.7.x | Auto-downloaded by wrapper |
| Gemini API Key | — | [aistudio.google.com](https://aistudio.google.com) |

**Minimum Android version:** Android 8.0 (API 26)  
**Target Android version:** Android 15 (API 35)

---

## Setup

### 1. Clone or extract the project

```bash
git clone https://github.com/YOUR_USERNAME/JARVISMOBILE.git
cd JARVISMOBILE
```

### 2. Configure your Gemini API key

Create a `.env` file in the **project root** (next to `build.gradle.kts`):

```bash
# Copy the example file
cp .env.example .env
```

Then open `.env` and set your key:

```
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

> ⚠️ **Never commit your `.env` file.** It is already listed in `.gitignore`.

### 3. Open in Android Studio

1. Open Android Studio → **File → Open**
2. Select the project root folder
3. Wait for Gradle sync to complete (first sync downloads ~500 MB of dependencies)
4. If prompted about SDK mismatches, click **OK** to let Studio resolve them

---

## Build the APK

### Debug APK (recommended for testing)

```bash
# From the project root:
./gradlew assembleDebug

# Output:
# app/build/outputs/apk/debug/app-debug.apk
```

### Release APK (unsigned)

```bash
./gradlew assembleRelease

# Output:
# app/build/outputs/apk/release/app-release-unsigned.apk
```

> **Note:** Release APK is unsigned. To install directly, either sign it or use `adb install` with `--allow-test-only`.

### Run lint checks before building

```bash
./gradlew lint
# HTML report: app/build/reports/lint-results-debug.html
```

---

## Install & Run

### Via Android Studio (easiest)

1. Connect a device via USB with **Developer Options → USB Debugging** enabled, OR start an emulator
2. Click the **Run ▶** button in Android Studio

### Via ADB (sideload APK)

```bash
# Install debug APK on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the app immediately
adb shell am start -n com.aistudio.jarvis.voiceagent/.MainActivity

# Watch logcat for crash output
adb logcat -s "MainActivity" "JarvisApplication" "JarvisViewModel" "JarvisVoiceEngine" "AppDatabase"
```

### Grant required permissions after first launch

JARVIS requires the following permissions — grant them when prompted or via Settings:

| Permission | Purpose |
|-----------|---------|
| `RECORD_AUDIO` | Voice recognition |
| `CALL_PHONE` | Direct call feature |
| `READ_CONTACTS` | Look up contacts by name |
| `SEND_SMS` | Messaging feature |
| `ACCESS_FINE_LOCATION` | Navigation/Maps |
| `READ_CALENDAR` / `WRITE_CALENDAR` | Calendar queries |
| `POST_NOTIFICATIONS` | Notification reader |

---

## Testing

### Run unit tests

```bash
./gradlew testDebugUnitTest

# HTML report:
# app/build/reports/tests/testDebugUnitTest/index.html
```

### Run all checks (lint + tests + debug build)

```bash
./gradlew lint testDebugUnitTest assembleDebug
```

### Regression test: Startup

1. Install the APK fresh: `adb install app/build/outputs/apk/debug/app-debug.apk`
2. Launch: `adb shell am start -n com.aistudio.jarvis.voiceagent/.MainActivity`
3. ✅ App should show the JARVIS main screen within 2 seconds
4. Check logcat — no `ClassNotFoundException` or `ResourcesNotFoundException` should appear

### Regression test: Call feature

1. Open the app and grant `CALL_PHONE` + `READ_CONTACTS` permissions
2. Tap the microphone and say **"Call [contact name]"**
3. ✅ The phone dialer should open with the contact's number pre-filled (or direct call if permission granted)
4. Alternatively say **"Call 9876543210"** — dialer should open with that number

---

## Troubleshooting

### ❌ App crashes immediately on launch (APK install)

**Most common cause:** `namespace` ≠ `applicationId` mismatch.  
**Status:** ✅ Fixed in this version — `namespace = "com.aistudio.jarvis.voiceagent"` now matches `applicationId`.

Check logcat:
```bash
adb logcat | grep -E "ActivityNotFoundException|ClassNotFoundException|FATAL"
```

### ❌ App shows black screen and freezes

**Cause:** `setTheme()` was called before `super.onCreate()` — violates Android lifecycle on API 31+.  
**Status:** ✅ Fixed — `setTheme()` removed; theme is now applied correctly via `AndroidManifest.xml`.

### ❌ Call feature opens dialer instead of calling directly

**Cause:** `ACTION_CALL` requires `CALL_PHONE` runtime permission AND an Activity context (not application context) on Android 10+.  
**Status:** ✅ Fixed — `MainActivity` now registers its Activity context with `JarvisViewModel` via `onResume`/`onPause`.  
Make sure you have granted `CALL_PHONE` permission in device Settings.

### ❌ "Speech recognition not available" error

This means Google's Speech Recognition service is not installed on the device.  
- On emulators: go to **Settings → Apps → Google** and update it
- On real devices: update **Google app** via Play Store

### ❌ Gradle build fails with "Could not connect to Kotlin compile daemon"

Already fixed in `gradle.properties`:
```
kotlin.compiler.execution.strategy=in-process
```

### ❌ Build fails: "Unresolved reference: R"

Caused by old `namespace = "com.example"`. Make sure you are using the updated code where `namespace = "com.aistudio.jarvis.voiceagent"`.

---

## Known Fixes Applied

This release includes the following critical fixes:

| Fix | File Changed | Description |
|-----|-------------|-------------|
| 🔴 Namespace/ApplicationId mismatch | `app/build.gradle.kts` | `namespace` now matches `applicationId` — resolves startup `ClassNotFoundException` |
| 🔴 Pre-`super` setTheme crash | `MainActivity.kt` | Removed `setTheme()` before `super.onCreate()` — resolves `ResourcesNotFoundException` on API 31+ |
| 🔴 Package declarations | All `*.kt` files | All packages renamed from `com.example` → `com.aistudio.jarvis.voiceagent` |
| 🟡 Call feature Activity context | `JarvisViewModel.kt`, `MainActivity.kt` | `ACTION_CALL` now gets proper Activity context via `onResume`/`onPause` lifecycle |
| 🟡 Database StrictMode crash | `AppDatabase.kt` | Removed `allowMainThreadQueries()` from in-memory fallback |
| 🟠 ProGuard keep rules | `proguard-rules.pro` | Added missing rules for tools, services, UI, and new namespace |
| 🟠 SDK mismatch | `build.gradle.kts`, `gradle.properties` | `targetSdk` aligned to 35, removed `suppressUnsupportedCompileSdk` |
| 🟠 CI/CD pipeline | `.github/workflows/build.yml` | Added lint, unit test, and release build jobs |

---

## Architecture

```
User Voice Input
       │
       ▼
JarvisVoiceEngine (SpeechRecognizer + TTS)
       │
       ▼
JarvisViewModel (state manager)
       │
       ▼
JarvisAgentEngine
  ├── Tier 1: LocalIntentRouter  ← <1ms, 0 API tokens, offline
  └── Tier 2: UllasBackendGateway
        ├── UllasAuthManager
        ├── UllasRateLimiter
        ├── UllasSemanticCache
        ├── UllasRequestQueue (backoff)
        └── GeminiAiProvider (Gemini API)
               │
               ▼
         Tool Execution (CallContactTool, AppLauncherTool, etc.)
               │
               ▼
         Room Database (history, memory, notes, reminders)
```

---

## GitHub Actions CI/CD

The CI pipeline runs on every push to `main`/`master` and every PR:

| Job | Trigger | What it does |
|-----|---------|-------------|
| `lint-and-test` | All pushes & PRs | Runs lint + unit tests |
| `build-debug` | After lint passes | Builds debug APK, uploads as artifact |
| `build-release` | Push to main only | Builds unsigned release APK, catches ProGuard issues |

### Add your Gemini API key to CI

In your GitHub repo: **Settings → Secrets and variables → Actions → New repository secret**

```
Name:  GEMINI_API_KEY
Value: your_actual_key_here
```

### Download built APK from CI

1. Go to your repo on GitHub → **Actions** tab
2. Click the latest workflow run
3. Scroll to **Artifacts** → download `JARVIS-debug-apk`

---

## View in AI Studio

[Open in AI Studio](https://ai.studio/apps/49c065fb-d060-40b3-9d96-47afb125c0d0)
