# DarkNote Desktop

Modern snippet manager with Dropbox sync - Desktop edition built with Compose Multiplatform.

## Features

- **Full CRUD operations** - Create, edit, delete snippets
- **Dropbox Sync** - Automatic bidirectional synchronization
- **Auto-save** - Changes saved automatically as you type
- **Search & Filter** - Find snippets quickly
- **Material 3 Design** - Modern dark theme UI
- **KDE Integration** - Breeze theme support, native look on KDE Plasma
- **Cross-platform** - Linux, macOS, Windows support

## Requirements

- JDK 17 or higher
- Gradle 8.0+

## Installation

### KDE Plasma (Recommended for Arch Linux + KDE)

Quick install for KDE users:

```bash
# 1. Build the JAR
./gradlew :apps:desktop:jar

# 2. Run install script
cd apps/desktop/packaging
./install-kde.sh
```

This will:
- Install desktop entry with KDE integration
- Add launcher to Application Menu
- Configure Breeze theme colors
- Set up right-click actions (New Snippet, Search)

**Uninstall:**
```bash
cd apps/desktop/packaging
./uninstall-kde.sh
```

### Manual Installation (All Linux)

#### Using package managers

**Debian/Ubuntu (.deb)**
```bash
./gradlew :apps:desktop:packageDeb
sudo dpkg -i apps/desktop/build/compose/binaries/main/deb/*.deb
```

**Arch Linux (.rpm → convert to pkg)**
```bash
./gradlew :apps:desktop:packageRpm
# Use alien or manually extract
```

**Universal (AppImage)**
```bash
./gradlew :apps:desktop:packageAppImage
chmod +x apps/desktop/build/compose/binaries/main/app-image/*.AppImage
./apps/desktop/build/compose/binaries/main/app-image/*.AppImage
```

### Development

Run without installation:
```bash
./gradlew :apps:desktop:run
```

## Architecture

### Stack
- **UI**: Jetpack Compose Desktop
- **Language**: Kotlin JVM
- **Database**: SQLDelight (SQLite via JDBC)
- **DI**: Koin
- **Sync**: Dropbox SDK (Java)
- **Storage**: Platform-specific (XDG dirs on Linux)
- **Theme**: KDE Breeze integration (auto-detects dark/light)

### KDE Plasma Integration

When running on KDE Plasma, DarkNote automatically:
- Detects Breeze Dark/Light theme from `~/.config/kdeglobals`
- Applies Breeze color palette to UI
- Uses Qt-compatible color scheme
- Registers desktop actions (New Snippet, Search)
- Integrates with Application Launcher
- Supports KDE startup notifications

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
