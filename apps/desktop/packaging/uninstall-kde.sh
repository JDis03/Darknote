#!/bin/bash
# Uninstall DarkNote from KDE Plasma

set -e

INSTALL_DIR="$HOME/.local/share"
DESKTOP_FILE="$INSTALL_DIR/applications/darknote.desktop"
ICON_DIR="$INSTALL_DIR/icons/hicolor"
BIN_DIR="$HOME/.local/bin"

echo "Uninstalling DarkNote from KDE Plasma..."

# Remove desktop entry
if [ -f "$DESKTOP_FILE" ]; then
    rm "$DESKTOP_FILE"
    echo "✓ Removed desktop entry"
fi

# Remove icons
for size in 512 256 128 48; do
    ICON_FILE="$ICON_DIR/${size}x${size}/apps/darknote.png"
    if [ -f "$ICON_FILE" ]; then
        rm "$ICON_FILE"
    fi
done
echo "✓ Removed icons"

# Remove launcher
if [ -f "$BIN_DIR/darknote" ]; then
    rm "$BIN_DIR/darknote"
    echo "✓ Removed launcher script"
fi

# Remove JAR
if [ -f "$BIN_DIR/darknote.jar" ]; then
    rm "$BIN_DIR/darknote.jar"
    echo "✓ Removed JAR file"
fi

# Update caches
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -f -t "$ICON_DIR" 2>/dev/null || true
fi

if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$INSTALL_DIR/applications" 2>/dev/null || true
fi

if command -v kbuildsycoca6 >/dev/null 2>&1; then
    kbuildsycoca6 2>/dev/null || true
elif command -v kbuildsycoca5 >/dev/null 2>&1; then
    kbuildsycoca5 2>/dev/null || true
fi

echo ""
echo "✓ DarkNote uninstalled successfully!"
echo ""
echo "Note: User data was NOT removed:"
echo "  Database: ~/.local/share/DarkNote/darknote.db"
echo "  Snippets: ~/.local/share/DarkNote/snippets/"
echo ""
echo "To remove data, run:"
echo "  rm -rf ~/.local/share/DarkNote"
echo ""
