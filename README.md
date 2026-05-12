# Turbo Downloader

A powerful, modern Android download manager built with Kotlin and Jetpack Compose. Inspired by 1DM, featuring multi-threaded downloads, a built-in browser, and a beautiful Material Design 3 UI.

## Features

### Core Download Engine
- **Multi-threaded downloading** — Split files into chunks and download simultaneously for maximum speed
- **Pause, Resume, Cancel** — Full control over your downloads
- **Auto-retry** — Failed downloads can be retried automatically
- **Background downloads** — Downloads continue in the background via foreground service
- **Queue management** — Control concurrent download limits

### Built-in Browser
- **Full-featured web browser** — Browse and discover downloadable files
- **Auto-detect downloads** — Automatically intercepts downloadable file URLs
- **Search integration** — Search the web or enter URLs directly
- **Navigation controls** — Back, forward, home, refresh

### File Management
- **Smart categorization** — Files automatically sorted into Video, Audio, Image, Document, Archive, APK, and Other
- **Filter by category** — Quick filter chips to find specific file types
- **Search downloads** — Find files by name or URL
- **Download history** — Track all past downloads

### Beautiful UI
- **Material Design 3** — Modern, clean interface with dynamic theming
- **Dark mode support** — Automatic or manual dark theme
- **Animated progress** — Smooth progress bars and speed gauges
- **Responsive layout** — Adapts to different screen sizes

### Settings
- **Concurrent downloads** — Control how many files download at once (1-5)
- **Threads per download** — Adjust thread count per file (1-8)
- **Wi-Fi only mode** — Prevent downloads on mobile data
- **Speed limiting** — Control bandwidth usage
- **Custom download location** — Choose where files are saved
- **Notification controls** — Manage download notifications

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt (Dagger) |
| Database | Room |
| Networking | OkHttp |
| Async | Kotlin Coroutines + Flow |
| Navigation | Navigation Compose |
| Image Loading | Coil |
| Preferences | DataStore |

## Project Structure

```
app/src/main/java/com/turbodownloader/
├── TurboDownloaderApp.kt          # Application class
├── MainActivity.kt                # Main entry point
├── data/
│   ├── database/                  # Room database, DAO, converters
│   ├── model/                     # Data models (DownloadItem, etc.)
│   └── repository/                # Data repositories
├── di/                            # Hilt dependency injection modules
├── download/                      # Download engine (multi-threaded)
├── service/                       # Foreground download service
├── ui/
│   ├── theme/                     # Material 3 theme (colors, typography)
│   ├── components/                # Reusable UI components
│   ├── navigation/                # App navigation
│   └── screens/
│       ├── home/                  # Home screen with stats & downloads
│       ├── downloads/             # Active/Completed downloads tabs
│       ├── browser/               # Built-in web browser
│       └── settings/              # App settings
└── util/                          # Utility classes (FileUtil, NetworkUtil)
```

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Minimum SDK 26 (Android 8.0)

### Build & Run
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on device or emulator (API 26+)

```bash
git clone https://github.com/faridexholic92-netizen/TurboDownloader.git
cd TurboDownloader
./gradlew assembleDebug
```

## Permissions

| Permission | Purpose |
|-----------|---------|
| INTERNET | Download files |
| ACCESS_NETWORK_STATE | Check network connectivity |
| FOREGROUND_SERVICE | Background downloads |
| POST_NOTIFICATIONS | Download progress notifications |
| READ/WRITE_EXTERNAL_STORAGE | Save downloaded files |
| WAKE_LOCK | Keep downloads running |

## License

This project is licensed under the MIT License.
