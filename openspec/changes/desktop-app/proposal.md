## Why

The DarkNote desktop app (Compose Multiplatform) has a solid foundation — snippet list, editor with syntax highlighting, and Dropbox sync — but lacks keyboard shortcuts, proper packaging, and full editor feature parity with professional tools like Kate. These gaps block daily workflow usage on KDE Plasma.

## What Changes

- Add global keyboard shortcuts (Ctrl+N new snippet, Ctrl+S save, Ctrl+F find, Ctrl+Shift+F full-text search, Ctrl+Q quit)
- Implement syntax highlighting in the editable `BasicTextField` (currently only works in read-only mode)
- Add clipboard operations (Ctrl+C/V/X) with proper Desktop clipboard integration
- Add snippet folder/tag management UI
- Create proper KDE Plasma `.desktop` integration with MIME type associations
- Add dark/light/system theme toggle in Settings
- Add auto-save indicator improvements (debounce, conflict detection)
- Package distributable AppImage for Linux

## Capabilities

### New Capabilities
- `keyboard-shortcuts`: Declarative keyboard shortcut system for all editor and global actions
- `editor-syntax-highlighting`: Real-time syntax highlighting in editable mode (not just read-only)
- `folder-tag-management`: Create, rename, delete folders and assign tags to snippets
- `theme-settings`: Persistent dark/light/system theme toggle with KDE Breeze auto-detection
- `appimage-packaging`: CI-ready AppImage build with KDE integration metadata

### Modified Capabilities
- (none — no existing specs to modify)

## Impact

- **Desktop build**: New dependencies may include `kotlinx-serialization` for settings persistence
- **DI module**: New services for theme management, keyboard shortcut registry
- **Shared modules**: FolderRepository already exists; tag support may require DB schema extension
- **Packaging**: AppImage config in `compose.desktop.nativeDistributions`
- **KDE integration**: Desktop entry file with `MimeType=` for code/snippet file associations
