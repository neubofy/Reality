<div align="center">

<img src="https://res.cloudinary.com/dnh4fonis/image/upload/v1781091079/ck39669alz53z3vkeiaq.png" width="160" alt="Reality Logo" style="border-radius: 20px; margin-bottom: 20px;">

# Reality
### Reality: Deep Focus & Routine Engine

**Developed by Pawan Washudev | Neubofy**

[![GitHub release](https://img.shields.io/github/v/release/neubofy/Reality?style=flat-square&color=orange)](https://github.com/neubofy/Reality/releases)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=flat-square)](https://www.android.com)
[![Privacy](https://img.shields.io/badge/Privacy-Local--First-teal.svg?style=flat-square)]()
[![Open Source](https://img.shields.io/badge/Source--Available-100%25-blue.svg?style=flat-square)]()
[![Ads](https://img.shields.io/badge/Ads-ZERO-red.svg?style=flat-square)]()

### 🌟 100% Source-Available • Zero Ads • Privacy-First • Transparent AI

**[🌐 Official Website](https://reality.neubofy.in)** • **[📥 Download Latest APK](https://reality.neubofy.in/download)** • **[🎯 Tapashya Web Focus Timer](https://reality.neubofy.in/tapashya)** • **[👑 Elite Membership](https://reality.neubofy.in/promembers)**

**[🔒 Privacy Policy](https://reality.neubofy.in/privacypolicy)** • **[📜 Terms of Service](https://reality.neubofy.in/termsofservice)**

</div>

---

## 💡 About Reality

**Reality** is an all-in-one, local-first Android app designed to build self-discipline, eliminate digital distractions, and streamline your daily routine.

Unlike apps that lock essential tools behind forced popups, **Reality is designed to work smoothly right out of the box without any subscription barriers or pushy upgrade prompts**. 

**What makes Reality truly unique:**
* **100% Source-Available**: Every line of code is open for public audit on GitHub. No hidden backend servers processing your data.
* **Open-Source Infrastructure**: The AI worker runs on Cloudflare Workers, website, APK builds, and subscription management—all visible in this repository.
* **Zero-Server Architecture**: Reality processes all data locally on your device. Your habits, journals, and calendar data never pass through Neubofy servers.
* **Transparent AI**: Our Reality Intelligence Assistant uses open-source heavy models like Ollama with LLaMA on your device, while leveraging Cloudflare Worker edge servers for speed and privacy.
* **Bring Your Own Cloud (BYOC)**: You sync directly through your own Google Cloud OAuth credentials—zero developer involvement.

* **Modular Feature Toggles**: By default, advanced integrations are kept disabled so the app stays clean and lightweight for everyday use.
* **On-Demand Permissions**: In Reality, you only grant permissions when you actually need them. As you toggle on additional tools in the settings panel, the Permission Manager dynamically guides you through each required step.
* **Separated Sign-In Flow**: 
  * If you choose to support development by purchasing an optional **Reality Elite Membership** (https://reality.neubofy.in/promembers), you only perform a standard, quick Google login on the Elite page.
  * Deep integrations (such as the Nightly Protocol, automated Calendar sync, Gamification, and Drive backups) require additional workspace permissions. To keep your access transparent and complete, you configure your own Google Cloud keys instead of authenticating through our backend.

---

## 🏗️ System Requirements & Setup

### Device Footprint
* **RAM**: 256MB minimum (typical footprint runs lightweight around 50–100MB).
* **Storage**: ~150MB for application and local SQLite database files.
* **Battery**: Less than 1% daily drain due to native Android accessibility optimizations.
* **Connectivity**: Core focus tools work 100% offline; cloud backup and AI assistant features require network access.

### Getting Started
1. **Security Intro**: Step through the quick onboarding flow in `SecurityIntroActivity.kt`.
2. **Grant Basic Permissions**: Visit `PermissionManagerActivity.kt` to allow:
   * **Accessibility Service**: Enables real-time window tracking and blocker overlays.
   * **System Alert Window**: Allows rendering focus reminders over blocked apps.
   * **Usage Statistics**: Calculates focus grades and screen time trends.
3. **Configure Blocklists**: Choose distracting apps to block in `UnifiedBlocklistActivity.kt` or `SelectAppsActivity.kt`.
4. **Enable Optional Features**: Toggle advanced tools as needed in Settings, granting extra permissions only as your workflow grows.

---

## 🌟 Flagship Feature: The 6-Step Nightly Protocol

The core rhythm of Reality revolves around the **Nightly Protocol**—an automated evening workflow coordinated via Android WorkManager (`NightlyWorker.kt` and `NightlyActivity.kt`) that helps you reflect, plan, and execute with precision.

* **Step 1: Activity Aggregation** — Gathers daily app usage, calendar logs, and fitness metrics into a clean summary.
* **Step 2: Guided Reflection Diary** — Prompts you with thoughtful, contextual journaling questions tailored to your day (`NightlyPromptsActivity.kt`).
* **Step 3: Analytics & XP Scoring** — Grades evening reflection consistency, computes daily scores, and awards XP.
* **Step 4: Smart Tomorrow Plan** — Autonomously structures a draft schedule for the next day directly inside your Google Drive.
* **Step 5: Automated Schedule Sync** — Parses your approved plan and maps it to Google Tasks, Google Calendar events, and morning alarms.
* **Step 6: Archiving & Daily Summary** — Exports a clean PDF summary report and appends your daily progress directly into Google Sheets.

---

## 🏆 Key Features

### 1. 🚫 App Blocker & Strict Mode
A dependable blocking system built directly on native Android APIs to prevent impulsive unlocks during study or deep-work sessions.
* Monitored seamlessly in `AppBlockerService.kt` and verified via `RealityBlocker.kt`.
* Optional anti-bypass controls via Device Admin (`StrictModeActivity.kt`), anti-tamper clock checking, and lockout cooldown timers.

### 2. ⚡ Tapasya (Deep Focus Timer)
A distraction-free focus timer built for sustained work and study sessions.
* Managed through `TapasyaService.kt` and `TapasyaManager.kt`.
* Features a distraction-free AMOLED black mode (`AmoledFocusActivity.kt`) with strict 15-minute effective work tracking.
* **Cross-Platform Sync**: Use the web timer at [**reality.neubofy.in/tapashya**](https://reality.neubofy.in/tapashya) and sync back to your Android app using QR code scanning (`QRScannerActivity.kt`) with encrypted session exports.

### 3. 🛌 Bedtime & Sleep Tracking
A local-first bedtime companion designed to encourage consistent sleep habits.
* Integrates directly with Android Health Connect via `HealthManager.kt` to read rest and activity without requiring third-party cloud wearables.
* Includes a convenient Quick Settings tile (`RealitySleepTileService.kt`) to trigger your wind-down routine with a single swipe.

### 4. 🔔 Math Wake-Up Alarms
A morning alarm designed to break sleep inertia and stop unconscious snoozing.
* Scheduled by `AlarmService.kt` and managed in `WakeupAlarmService.kt`.
* Renders a clean arithmetic problem (`WakeupAlarmRingingActivity.kt`) with auto-scaling difficulty based on how early you wake up.

### 5. 🤖 In-App Assistant (Tool Agent)
A local-first assistant that executes practical in-app actions rather than just generating chat text.
* Runs in `AIChatActivity.kt` and `PopupAIChatActivity.kt`, configured via `AISettingsActivity.kt`.
* Uses Model Context Protocol (MCP) tool registrations (`ToolRegistry.kt`) to adjust alarms, add tasks, and manage app blocks directly on your device.
* **AI Architecture**: Uses open-source heavy models on-device (like Ollama with LLaMA) for privacy, while leveraging Cloudflare Worker edge servers for speed and inference acceleration.

### 6. 🎨 Custom Appearance & Themes
Tailor the look and feel of the app to match your setup in `AppearanceActivity.kt` with custom fonts, AMOLED dark palettes, and Material3 styling.

---

## 🔒 Privacy, BYOC & Cloud Sync

Reality is built local-first: your habits, journal logs, and history stay inside an encrypted Room database on your device.

### Bring Your Own Cloud (BYOC) Architecture
You can sync your workflow directly through your own Google Cloud Console project without passing data through external developer servers.

**Required OAuth Scopes for Full Sync:**
* `https://www.googleapis.com/auth/calendar.events` — To coordinate focus blocks with your calendar schedule.
* `https://www.googleapis.com/auth/drive.file` — To create and store Nightly Protocol plan documents.
* `https://www.googleapis.com/auth/tasks` — To create and manage your daily task lists.
* `https://www.googleapis.com/auth/userinfo.email`, `https://www.googleapis.com/auth/userinfo.profile`, `openid` — For profile identity and authentication.

**Setting Up Your Cloud Keys:**
1. Open Reality on your Android device.
2. Navigate to the **Profile Page** or the **Elite Page** (https://reality.neubofy.in/promembers).
3. Tap the **Settings / Cloud** icon in the top-right corner.
4. Paste your custom Client ID and Client Secret, then tap **Save**.
5. Tap **Connect** to link your workspace directly.

### Cloudflare Edge Workers & JIT Cryptography
To ensure maximum security and prevent physical device-level data extraction:
* Deterministic encryption keys are computed Just-In-Time on Cloudflare Workers edge nodes.
* These workers execute HMAC-SHA256 calculations using secret peppers and transient login tokens.
* No database, configuration files, or user data are persisted on edge servers—only cryptographic operations are performed.

---

## 🏗️ Technical Architecture

### Technology Stack
```
Platform:        Android 8.0+ (API 26 to 36)
Language:        Kotlin 100% (Type-Safe)
UI Framework:    AndroidX + Material3
Database:        Room ORM + SQLite (Encrypted)
Threading:       Kotlin Coroutines + Flow
Networking:      OkHttp + Retrofit
Background:      WorkManager + AlarmManager
Parsers:         GSON, JSoup, Markwon
Web Stack:       Next.js + React + TypeScript (Website & Tapashya Timer)
AI Infrastructure: Cloudflare Workers (JIT Cryptography & Inference)
```

### Key Libraries
| Category | Library | Purpose |
| :--- | :--- | :--- |
| **Google APIs** | google-api-client-android, Tasks, Calendar, Drive | Direct Workspace synchronization |
| **Health** | Health Connect Client | Native fitness and sleep metrics |
| **Database** | Room ORM | Secure local data persistence |
| **Background** | WorkManager & AlarmManager | Scheduled routines and precise alarms |
| **Markdown** | Markwon | High-performance text rendering |
| **Web AI Timer** | Next.js, Tailwind CSS | Tapashya web interface with QR sync |

### Build & Deployment
* **GitHub Actions Workflow**: Automated APK builds and direct uploads to GitHub Releases.
* **Website Hosting**: Next.js app deployed to Vercel.
* **Cloudflare Workers**: Open-source AI inference and JIT cryptography endpoints.
* **All workflows and scripts are visible in this repository**—zero hidden CI/CD magic.

---

## 📞 Support & Contact Options

Feel free to reach out for feature suggestions, feedback, or bug reports:

* **Official Website**: [https://reality.neubofy.in](https://reality.neubofy.in)
* **Privacy Policy**: [https://reality.neubofy.in/privacypolicy](https://reality.neubofy.in/privacypolicy)
* **Terms of Service**: [https://reality.neubofy.in/termsofservice](https://reality.neubofy.in/termsofservice)
* **Elite Membership**: [https://reality.neubofy.in/promembers](https://reality.neubofy.in/promembers)
* **Tapashya Web Timer**: [https://reality.neubofy.in/tapashya](https://reality.neubofy.in/tapashya)
* **Email**: [support@neubofy.in](mailto:support@neubofy.in)
* **GitHub Issues**: [Report a Bug / Request a Feature](https://github.com/neubofy/Reality/issues)
* **Telegram**: [@pawanwashudev](https://t.me/pawanwashudev)
* **WhatsApp**: [@pawanwashudev](https://wa.me/pawanwashudev)
* **Instagram**: [@pawanwashudev](https://instagram.com/pawanwashudev)
* **LinkedIn**: [@pawanwashudev](https://linkedin.com/in/pawanwashudev)

---

## ⚖️ Source Availability & Terms

Reality is **100% source-available** for transparency, security review, and personal study. 

**Key principles:**
* You are welcome to inspect, learn from, and audit the entire codebase for security vulnerabilities.
* Unauthorized cloning, commercial redistribution, or publishing modified builds is strictly prohibited.
* Official pre-compiled APKs are distributed only through [reality.neubofy.in/download](https://reality.neubofy.in/download) and GitHub Releases.
* All infrastructure code (Cloudflare Workers, website, CI/CD workflows, subscription management) is visible in this repository.

**For complete legal details, see [Terms of Service](https://reality.neubofy.in/termsofservice).**

---

## 🚀 Why This Matters

Reality represents a new standard for mobile productivity:
* **No Data Harvesting**: Your focus metrics, journal entries, and reflections are yours alone—never monetized or analyzed by Neubofy.
* **Verifiable Privacy**: With source code available, security researchers and users can independently verify our privacy claims.
* **True Local-First Architecture**: Unlike many "privacy-first" apps, Reality has zero backend servers analyzing your behavior.
* **Transparent Monetization**: The optional Reality Elite Membership (https://reality.neubofy.in/promembers) is the only revenue model—no hidden data sales or premium feature paywalls.

---

## 📊 Community & Contribution

This is an open-source project developed by Pawan Washudev. While we don't accept code contributions (source-available model), we deeply value:
* Security audit reports
* Bug reports via GitHub Issues
* Feature requests and feedback
* Documentation improvements

> **"Stop managing your life. Start commanding it."** — Built by someone who lost control of their own fingers, designed for those who want it back.

---

**Made with ❤️ by Pawan Washudev @ Neubofy**
