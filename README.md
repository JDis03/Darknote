# DarkNote

Modern snippet manager with Dropbox sync. Store, organize, and sync your code snippets across Android and Desktop.

![Platform](https://img.shields.io/badge/platform-Android%20%7C%20Linux-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple)
![License](https://img.shields.io/badge/license-GPL--3.0-green)

## Features

- **Sync** — Dropbox sync with offline-first support
- **Organize** — Folders, tags, favorites, and search
- **Highlight** — 12 languages with syntax highlighting
- **Cross-platform** — Android + Desktop (Compose Multiplatform)
- **KDE ready** — Breeze theme auto-detection on Plasma

## Download

| Platform | Version | Link |
|----------|---------|------|
| Android | 1.0.0 | [Release APK](https://github.com/JDis03/Darknote/releases/tag/v1.0.0) |
| Desktop | 1.0.0 | [JAR](https://github.com/JDis03/Darknote/releases/tag/v1.0.0) |

## Quick Start

### Android
```bash
adb install android-release-signed.apk
```

### Desktop
```bash
./gradlew :apps:desktop:run
```

## Build

```bash
# Android release APK
./gradlew :apps:android:assembleRelease

# Desktop JAR
./gradlew :apps:desktop:jar

# Run all tests
./gradlew test
```

## Architecture

```
apps/
├── android/          # Jetpack Compose + Hilt
└── desktop/          # Compose Multiplatform + Koin
shared/
├── core/             # Models, repositories, sync engine
├── persistence/      # SQLDelight (SQLite)
└── sync/             # Dropbox client + sync logic
```

## Tech Stack

- **UI**: Jetpack Compose / Compose Multiplatform
- **Language**: Kotlin 2.0
- **Database**: SQLDelight (SQLite)
- **Sync**: Dropbox SDK
- **DI**: Hilt (Android) / Koin (Desktop)

## License

GPL-3.0
