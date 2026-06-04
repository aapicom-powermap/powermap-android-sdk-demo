# PowerMap Android Demo

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)

A demo application showcasing the [PowerMap Android SDK](https://github.com/powermap/powermap-android-sdk) capabilities including maps, search, routing, tracking, and navigation.

---

## 🚀 Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/powermap/powermap-android-demo.git
cd powermap-android-demo
```

### 2. Set up API credentials
Create a `local.properties` file in the **root** of the project (this file is excluded from git):
```properties
# local.properties — DO NOT COMMIT
POWERMAP_MAP_API_KEY=your_map_api_key_here
POWERMAP_CLIENT_ID=your_client_id_here
POWERMAP_CLIENT_SECRET=your_client_secret_here

sdk.dir=/path/to/your/Android/Sdk
```

> **Note:** Contact the PowerMap team to obtain your API credentials.

### 3. Update the SDK AAR
Place the latest `powermap-sdk-release.aar` file inside `app/libs/`:
```
app/
└── libs/
    └── powermap-sdk-release.aar   ← copy here
```

Build the AAR from the [powermap-android-sdk](https://github.com/powermap/powermap-android-sdk) repo:
```bash
cd powermap-android-sdk
./gradlew :powermap-sdk:assembleRelease
# Output: powermap-sdk/build/outputs/aar/powermap-sdk-release.aar
```

### 4. Open in Android Studio
Open the project root in Android Studio, sync Gradle, then run on an emulator or device.

---

## 📦 SDK Dependency

This demo consumes the PowerMap Android SDK as a **local AAR file**:

```kotlin
// app/build.gradle.kts
implementation(files("libs/powermap-sdk-release.aar"))
```

---

## 🗂️ Project Structure

```
app/src/main/java/com/powermap/demo/
├── MainActivity.kt        — Entry point
├── measure/               — Distance/area measurement tools
├── model/                 — Data models
├── navigation/            — Turn-by-turn navigation UI
├── repository/            — POI data repository
├── theme/                 — Compose theme
├── ui/                    — Screens and composables
├── utils/                 — Utility functions
└── viewmodel/             — ViewModels
```

---

**© 2026 PowerMap Development Team. All rights reserved.**