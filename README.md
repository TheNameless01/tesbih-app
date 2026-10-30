# 📿 Musabbih (مُسَبِّح) — Premium Native Android Tasbeeh Companion

<p align="center">
  <img src="docs/screenshots/app_icon.png" width="140" height="140" alt="Musabbih Icon" style="border-radius: 28px;" />
</p>

<p align="center">
  <strong>تطبيق تسبيح وأذكار إسلامي فاخر، هادئ، ومتقن بنظام أندرويد الأصيل</strong><br>
  <em>A peaceful, offline-first, privacy-respecting native Android Tasbeeh companion engineered with modern Android architecture.</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0%2B_(API_26%2B)-success?style=flat-square&logo=android" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-blue?style=flat-square&logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-emerald?style=flat-square" alt="Compose" />
  <img src="https://img.shields.io/badge/Database-Room_(SQLite)-amber?style=flat-square&logo=sqlite" alt="Room" />
  <img src="https://img.shields.io/badge/Privacy-100%25_Offline_&_Local-green?style=flat-square" alt="Offline" />
</p>

---

## 📸 Screenshots Showcase

<p align="center">
  <img src="docs/screenshots/home_screen.jpg" width="30%" alt="Home Counter Screen" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="docs/screenshots/adhkar_screen.jpg" width="30%" alt="Dhikr Library Screen" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="docs/screenshots/stats_screen.jpg" width="30%" alt="Statistics Screen" />
</p>

<p align="center">
  <em>(1) شاشة التسبيح التفاعلية | (2) مكتبة الأذكار الموثقة | (3) لوحة الإحصائيات وسلاسل الإنجاز</em>
</p>

---

## ✨ Core Highlights & Philosophy

### 1. 📿 Robust Counter Engine
- **Atomic Database Transactions**: Every increment and undo operation executes inside a Room database transaction protected by a mutex-serialized processor to guarantee zero lost taps even during rapid tapping.
- **Target Crossing & Continuity**: Reaching a target (e.g., 33, 100, custom target, or unlimited `∞`) triggers subtle milestone haptics and visual badges without halting or resetting the flow.
- **Transactional Undo & Safe Reset**: Undo restores the previous count and reverses daily tallies. Reset requires explicit user confirmation.
- **Session Durability**: Unfinished sessions automatically survive process death, screen rotation, and device restarts.

### 2. 📖 Authentic Dhikr Library
- **13 Verified Categories**:
  1. أذكار الصباح (Morning Adhkar)
  2. أذكار المساء (Evening Adhkar)
  3. أذكار بعد الصلاة (After Prayer)
  4. التسبيح (Tasbeeh)
  5. التحميد (Tahmeed)
  6. التكبير والتهليل (Takbeer & Tahleel)
  7. الاستغفار والتوبة (Istighfar)
  8. الصلاة على النبي ﷺ (Salawat)
  9. الأدعية القرآنية (Quranic Duas)
  10. الأدعية النبوية (Prophetic Duas)
  11. أذكار النوم (Sleep Adhkar)
  12. أذكار الاستيقاظ (Waking Adhkar)
  13. أذكار عامة (General Adhkar)
- **Strict Religious Accuracy**: All built-in entries carry exact citations from Sahih al-Bukhari, Sahih Muslim, Sunan Abi Dawud, and Quranic Surah & Ayah with `REVIEWED` status.
- **Diacritic-Insensitive Search**: Normalizes Arabic diacritics (tashkeel, tatweel, alef variations) for instant search while preserving authentic Quranic typography.
- **Custom Adhkar**: Add, edit, and delete personal Adhkar with custom repetition targets.

### 3. 📊 Accurate Statistics & Date Boundary Handling
- **Across-Midnight Attribution**: Uses `session_daily_totals` to record repetitions per local date and timezone, correctly attributing counts when sessions cross midnight boundaries.
- **Insightful Metrics**: Today’s repetitions, all-time totals, consecutive day streaks, longest streak, and top 5 most-used Adhkar.
- **Weekly Activity Chart**: 7-day activity bar chart with localized Arabic weekday indicators.

### 4. 🔒 Privacy-First & 100% Offline
- **Zero Internet Requirement**: Completely functional without network connectivity.
- **No Analytics / No Tracking**: No ads, telemetry, or user data collection.
- **User-Controlled JSON Backup & Restore**: Export and import full app state using the Android Storage Access Framework (SAF) with pre-import validation and preview.

### 5. 🎨 Luxury Islamic Design System
- **Emerald & Champagne Gold Palette**: Inspired by Islamic architecture with deep forest green surfaces (`#111811`, `#1B241D`, `#263128`) and subtle champagne gold accents (`#D5C29B`).
- **RTL-First**: Designed natively from the ground up for right-to-left Arabic typography and layouts.
- **Bundled Fonts**: Locally bundled **Noto Naskh Arabic** for authentic Quranic/Hadith typography and **Noto Sans Arabic** for navigation and controls.
- **Adaptive Icons**: Custom adaptive vector launcher icons matching the deep emerald mihrab arch and gold prayer beads.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.2+ (100%)
- **UI Framework**: Jetpack Compose (BOM 2024.09.00)
- **Design System**: Material Design 3 (M3) with Dark / Light / System theme support
- **Architecture**: Clean Architecture + MVVM + Repository Pattern
- **Persistence**: Room 2.7.0 with Kotlin Symbol Processing (KSP) + DataStore Preferences
- **Concurrency**: Coroutines & Kotlin StateFlow / SharedFlow
- **Background Tasks**: WorkManager 2.9 for non-exact reminders
- **Testing**: JUnit 4, Robolectric 4.16+, AndroidX Test

```
com.example/
├── core/
│   ├── backup/        # JSON export/import & validation logic
│   ├── database/      # Room entities, DAOs, Database & seed data
│   ├── datastore/     # Preferences repository (theme, numerals, haptics)
│   ├── engine/        # Atomic CounterEngine & HapticFeedbackHelper
│   ├── reminder/      # WorkManager reminder worker & scheduler
│   └── ui/            # Arabic number formatters & text normalizers
├── feature/
│   ├── home/          # Interactive counter dial, target crossing, undo
│   ├── adhkar/        # Library browsing, category filters & custom Dhikr
│   ├── statistics/    # Streak analytics, summary cards & weekly chart
│   ├── history/       # Session archives & detail dialogs
│   └── settings/      # Theming, reminders, backup/restore & privacy
├── navigation/        # Bottom navigation bar & type-safe routes
├── ui/theme/          # M3 color tokens, Noto typography & RTL theme wrapper
├── MainActivity.kt    # Single activity entry point with edge-to-edge
└── MusabbihApplication.kt
```

---

## 🚀 Building & Running the Project

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17+
- Android SDK 36 (Minimum SDK: 26 / Android 8.0)

### Build Commands
```bash
# Build the Debug APK
gradle assembleDebug

# Run Unit & Robolectric Tests
gradle :app:testDebugUnitTest
```

The compiled APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License & Privacy
- **Privacy**: Musabbih collects **0** bytes of user information and makes no network requests.
- **Religious Content**: Built-in Adhkar and citations are verified from canonical Sunni Hadith collections and the Holy Quran.
