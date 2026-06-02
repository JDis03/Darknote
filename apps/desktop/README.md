# DarkNote Desktop

Modern snippet manager with Dropbox sync - Desktop edition built with Compose Multiplatform.

## Features

- **Full CRUD operations** - Create, edit, delete snippets
- **Folder organization** - Collapsible sidebar with folder tree (Kate-style)
- **Tag management** - Add/remove tags with chip UI
- **Syntax highlighting** - Real-time highlighting in editable mode (12 languages)
- **Keyboard shortcuts** - Ctrl+N (new), Ctrl+S (save), Ctrl+F (find), Ctrl+Shift+F (search)
- **Dropbox Sync** - Automatic bidirectional synchronization with tombstone deletion
- **Auto-save** - Changes saved automatically as you type
- **Search & Filter** - Find snippets by title, content, tags, or language
- **Theme settings** - Dark/Light/System with persistent JSON storage
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

**Requirements**: JDK 17+ with `jpackage` tool (not included in Android Studio JBR)

```bash
# Build release AppImage
./gradlew :apps:desktop:packageReleaseAppImage

# Run the AppImage
chmod +x apps/desktop/build/compose/binaries/main-release/app-image/*.AppImage
./apps/desktop/build/compose/binaries/main-release/app-image/*.AppImage
```

**Note**: If you get "jpackage is missing", install a full JDK:
```bash
# Arch Linux
sudo pacman -S jdk-openjdk

# Ubuntu/Debian
sudo apt install openjdk-17-jdk
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
- Settings: `~/.config/darknote/settings.json`

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

- [x] Keyboard shortcuts (Ctrl+N, Ctrl+S, Ctrl+F, Ctrl+Shift+F)
- [x] Syntax highlighting (12 languages)
- [x] Folder organization with sidebar
- [x] Tag management
- [x] Theme settings (Dark/Light/System)
- [ ] Menu bar (File, Edit, View, Help)
- [ ] Multi-window support
- [ ] System tray integration
- [ ] Markdown preview
- [ ] Import/export (JSON, Markdown)
- [ ] Code formatting (via external tools)

## License

See root project LICENSE file.
