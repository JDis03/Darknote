#!/bin/bash
# Install DarkNote for KDE Plasma
# This script installs the desktop entry and icon for KDE integration

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_DIR="$HOME/.local/share"
DESKTOP_DIR="$INSTALL_DIR/applications"
ICON_DIR="$INSTALL_DIR/icons/hicolor"
BIN_DIR="$HOME/.local/bin"

echo "Installing DarkNote for KDE Plasma..."

# Create directories
mkdir -p "$DESKTOP_DIR"
mkdir -p "$ICON_DIR/512x512/apps"
mkdir -p "$ICON_DIR/256x256/apps"
mkdir -p "$ICON_DIR/128x128/apps"
mkdir -p "$ICON_DIR/48x48/apps"
mkdir -p "$BIN_DIR"

# Find the JAR file
if [ -f "$SCRIPT_DIR/../build/compose/jars/desktop-linux-x64-1.0.0.jar" ]; then
    JAR_PATH="$SCRIPT_DIR/../build/compose/jars/desktop-linux-x64-1.0.0.jar"
elif [ -f "$SCRIPT_DIR/../build/libs/desktop-1.0.0.jar" ]; then
    JAR_PATH="$SCRIPT_DIR/../build/libs/desktop-1.0.0.jar"
else
    echo "Error: JAR file not found. Please build the project first:"
    echo "  ./gradlew :apps:desktop:jar"
    exit 1
fi

# Copy JAR to local bin
cp "$JAR_PATH" "$BIN_DIR/darknote.jar"
echo "Installed JAR to $BIN_DIR/darknote.jar"

# Create launcher script
cat > "$BIN_DIR/darknote" << 'EOF'
#!/bin/bash
java -jar "$HOME/.local/bin/darknote.jar" "$@"
EOF
chmod +x "$BIN_DIR/darknote"
echo "Created launcher script at $BIN_DIR/darknote"

# Install desktop entry
cat > "$DESKTOP_DIR/darknote.desktop" << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=DarkNote
GenericName=Snippet Manager
Comment=Modern snippet manager with Dropbox sync
Exec=$BIN_DIR/darknote %U
Icon=darknote
Terminal=false
Categories=Utility;TextEditor;Development;Qt;KDE;
Keywords=snippet;code;clipboard;sync;dropbox;
MimeType=text/plain;
StartupNotify=true
StartupWMClass=DarkNote

# KDE specific
X-KDE-StartupNotify=true
X-DBUS-StartupType=
X-DBUS-ServiceName=
X-KDE-SubstituteUID=false
X-KDE-Username=

# Actions for KDE
Actions=NewSnippet;Search;

[Desktop Action NewSnippet]
Name=New Snippet
Exec=$BIN_DIR/darknote --new

[Desktop Action Search]
Name=Search Snippets
Exec=$BIN_DIR/darknote --search
EOF
chmod +x "$DESKTOP_DIR/darknote.desktop"
echo "Installed desktop entry to $DESKTOP_DIR/darknote.desktop"

# Copy icon if available
if [ -f "$SCRIPT_DIR/../src/main/resources/icon.png" ]; then
    ICON_SOURCE="$SCRIPT_DIR/../src/main/resources/icon.png"
    
    # Install different sizes (KDE uses multiple sizes)
    for size in 512 256 128 48; do
        SIZE_DIR="$ICON_DIR/${size}x${size}/apps"
        if command -v convert >/dev/null 2>&1; then
            convert "$ICON_SOURCE" -resize ${size}x${size} "$SIZE_DIR/darknote.png"
        else
            cp "$ICON_SOURCE" "$SIZE_DIR/darknote.png"
        fi
    done
    echo "Installed icon to $ICON_DIR"
fi

# Update icon cache for KDE
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -f -t "$ICON_DIR" 2>/dev/null || true
fi

# Update desktop database
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$DESKTOP_DIR" 2>/dev/null || true
fi

# Notify KDE to update application menu
if command -v kbuildsycoca6 >/dev/null 2>&1; then
    kbuildsycoca6 2>/dev/null || true
elif command -v kbuildsycoca5 >/dev/null 2>&1; then
    kbuildsycoca5 2>/dev/null || true
fi

echo ""
echo "✓ DarkNote installed successfully for KDE!"
echo ""
echo "You can now:"
echo "  1. Find 'DarkNote' in your KDE Application Launcher"
echo "  2. Run from terminal: darknote"
echo "  3. Pin to taskbar or add to favorites"
echo ""
echo "Data locations:"
echo "  Database: ~/.local/share/DarkNote/darknote.db"
echo "  Snippets: ~/.local/share/DarkNote/snippets/"
echo ""
