# DarkNote - KDE Plasma Integration Guide

DarkNote is fully optimized for KDE Plasma on Arch Linux with native Breeze theme support.

## Features for KDE Users

### 🎨 Native Breeze Theme
- **Auto-detects** your Breeze Dark/Light preference
- **Matches** KDE color scheme exactly
- **Respects** `~/.config/kdeglobals` settings
- **Updates** when you switch themes (restart app)

### 🚀 Quick Install

```bash
# From project root
./gradlew :apps:desktop:jar
cd apps/desktop/packaging
./install-kde.sh
```

That's it! DarkNote will appear in your Application Launcher.

### 📍 Integration Points

**Application Launcher**
- Find "DarkNote" in Utilities category
- Search with Krunner: `darknote`
- Desktop Actions: Right-click for "New Snippet" or "Search"

**Taskbar**
- Pin to panel for quick access
- Shows in task manager as "DarkNote"
- Supports minimize to tray

**File Manager (Dolphin)**
- Right-click text files → "Open with DarkNote" (after first use)
- MimeType association for `text/plain`

### ⚙️ KDE-Specific Settings

DarkNote reads these KDE configuration files:

1. **Color Scheme**: `~/.config/kdeglobals`
   - Section: `[General]` → `ColorScheme=`
   - Section: `[WM]` → `activeBackground=`

2. **Desktop Environment**: Environment variables
   - `XDG_CURRENT_DESKTOP=KDE`
   - `DESKTOP_SESSION=plasma`

### 🎨 Breeze Color Mappings

| UI Element | Breeze Dark | Breeze Light |
|------------|-------------|--------------|
| Primary | #3DAEE9 | #3DAEE9 |
| Background | #232629 | #FCFCFC |
| Surface | #31363B | #EFF0F1 |
| Text | #EFF0F1 | #232629 |
| Error | #DA4453 | #DA4453 |
| Success | #2ECC71 | #27AE60 |

### 📂 Data Locations (XDG Compliant)

```
~/.local/share/DarkNote/
├── darknote.db          # SQLite database
└── snippets/            # Snippet content files

~/.local/share/applications/
└── darknote.desktop     # Desktop entry

~/.local/bin/
├── darknote             # Launcher script
└── darknote.jar         # Application JAR

~/.local/share/icons/hicolor/
├── 512x512/apps/darknote.png
├── 256x256/apps/darknote.png
├── 128x128/apps/darknote.png
└── 48x48/apps/darknote.png
```

### 🔧 Customization

**Change Theme Manually**

If auto-detection fails, you can force a theme:

```kotlin
// Edit apps/desktop/src/main/kotlin/com/darknote/desktop/Main.kt
val colorScheme = darkColorScheme()  // or lightColorScheme()
```

**Custom Colors**

Override Breeze colors in `KDEIntegration.kt`:

```kotlin
darkColorScheme(
    primary = Color(0xFFYourColor),
    // ... other colors
)
```

### 🐛 Troubleshooting

**Theme not detected**
```bash
# Check KDE environment
echo $XDG_CURRENT_DESKTOP
echo $DESKTOP_SESSION

# Check color scheme file
cat ~/.config/kdeglobals | grep ColorScheme
```

**Not showing in launcher**
```bash
# Rebuild KDE cache
kbuildsycoca6  # KDE 6
# or
kbuildsycoca5  # KDE 5
```

**Icons not showing**
```bash
# Update icon cache
gtk-update-icon-cache -f -t ~/.local/share/icons/hicolor/
```

### 🗑️ Uninstall

```bash
cd apps/desktop/packaging
./uninstall-kde.sh
```

This removes the app but keeps your data. To remove data too:

```bash
rm -rf ~/.local/share/DarkNote
```

### ⌨️ KDE Shortcuts (Coming Soon)

We're planning KDE-specific shortcuts:

- `Meta+Alt+N` - New snippet (global)
- `Meta+Alt+S` - Search snippets (global)
- Integration with KDE's custom shortcut system

### 🔄 Sync with Plasma Across Devices

If you use KDE Plasma on multiple machines:

1. Install DarkNote on each machine
2. Connect to same Dropbox account
3. Snippets sync automatically
4. Database stays local (no conflicts)

### 📊 Performance on KDE

**Startup Time**: ~2s on NVMe SSD
**Memory Usage**: ~150MB (includes JVM)
**Disk Usage**: ~80MB installed
**CPU Usage**: <5% idle, <15% typing

Optimized for KDE Plasma 5.27+ and Plasma 6.

### 🆘 Support

Having issues on KDE?

1. Check logs: `journalctl --user -xe | grep darknote`
2. Run from terminal: `darknote` (see errors)
3. File issue on GitHub with KDE version

### 🎯 Tested Configurations

- ✅ Arch Linux + KDE Plasma 6.0
- ✅ Arch Linux + KDE Plasma 5.27
- ✅ Manjaro KDE
- ✅ KDE Neon
- ⚠️ Kubuntu (should work, not tested)

---

**Enjoy using DarkNote on KDE Plasma!** 🎉

For general documentation, see [README.md](README.md)
