# DarkNote Desktop

Modern snippet manager with Dropbox sync - Desktop edition built with Compose Multiplatform.

## Features

- **Full CRUD operations** - Create, edit, delete snippets
- **Dropbox Sync** - Automatic bidirectional synchronization
- **Auto-save** - Changes saved automatically as you type
- **Search & Filter** - Find snippets quickly
- **Material 3 Design** - Modern dark theme UI
- **Cross-platform** - Linux, macOS, Windows support

## Requirements

- JDK 17 or higher
- Gradle 8.0+

## Building

### Run in development
```bash
./gradlew :apps:desktop:run
```

### Create distributable package

#### Linux (.deb for Debian/Ubuntu)
```bash
./gradlew :apps:desktop:packageDeb
```
Output: `apps/desktop/build/compose/binaries/main/deb/`

#### Linux (.rpm for Fedora/RHEL/Arch)
```bash
./gradlew :apps:desktop:packageRpm
```
Output: `apps/desktop/build/compose/binaries/main/rpm/`

#### AppImage (universal Linux)
```bash
./gradlew :apps:desktop:packageAppImage
```
Output: `apps/desktop/build/compose/binaries/main/app-image/`

### Create distributable folder
```bash
./gradlew :apps:desktop:createDistributable
```
Output: `apps/desktop/build/compose/binaries/main/app/`

## Architecture

### Stack
- **UI**: Jetpack Compose Desktop
- **Language**: Kotlin JVM
- **Database**: SQLDelight (SQLite via JDBC)
- **DI**: Koin
- **Sync**: Dropbox SDK (Java)
- **Storage**: Platform-specific (XDG dirs on Linux)

### Project Structure
```
apps/desktop/
├── di/                 # Dependency injection modules
├── platform/           # Platform-specific implementations
│   ├── DesktopClipboardManager.kt
│   └── DesktopFileStorageService.kt
├── ui/screens/         # Compose UI screens
│   ├── SnippetListScreen.kt
│   └── EditorScreen.kt
├── viewmodel/          # Business logic
│   └── SnippetListViewModel.kt
└── Main.kt             # Application entry point
```

### Shared Code
- **100%** ViewModels and business logic
- **100%** Repository layer (SQLDelight multiplatform)
- **100%** Sync engine
- **~80%** UI composables (platform adjustments for desktop layouts)

## Data Storage

### Linux
- Database: `~/.local/share/DarkNote/darknote.db`
- Snippets: `~/.local/share/DarkNote/snippets/`
- Preferences: Java Preferences API

### macOS
- Database: `~/Library/Application Support/DarkNote/darknote.db`
- Snippets: `~/Library/Application Support/DarkNote/snippets/`

### Windows
- Database: `%APPDATA%/DarkNote/darknote.db`
- Snippets: `%APPDATA%/DarkNote/snippets/`

## Dropbox Integration

1. Launch the app
2. Go to Settings
3. Click "Connect Dropbox"
4. Authorize in browser
5. Enter auth code
6. Sync starts automatically

Snippets sync to `/darknote/` folder in your Dropbox.

## Development

### Build JAR
```bash
./gradlew :apps:desktop:jar
```

### Run tests
```bash
./gradlew :apps:desktop:test
```

### Clean build
```bash
./gradlew clean :apps:desktop:build
```

## Packaging Notes

- **Requires jpackage**: Available in JDK 17+
- **ProGuard optimization**: Configured for release builds
- **Bundle size**: ~80MB (includes JVM runtime)
- **First startup**: Creates database and storage directories

## Roadmap

- [ ] Menu bar (File, Edit, View, Help)
- [ ] Keyboard shortcuts (Ctrl+N, Ctrl+S, Ctrl+F)
- [ ] Multi-window support
- [ ] System tray integration
- [ ] Markdown preview
- [ ] Syntax highlighting themes
- [ ] Import/export (JSON, Markdown)

## License

See root project LICENSE file.
