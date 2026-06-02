## Context

DarkNote Desktop is a Compose Multiplatform JVM app targeting KDE Plasma (Wayland). Current state:
- Snippet list + advanced editor with CodeEditor component
- Syntax highlighting via `SyntaxHighlighterFactory` — **only works in read-only mode** (Text), not in `BasicTextField`
- 12 language highlighters (Kotlin, Python, JS, TS, Rust, Go, C++, Java, JSON, XML, SQL, Markdown)
- Dropbox sync via shared module
- KDE Breeze theme auto-detection
- No keyboard shortcuts, no folder/tag management UI, no theme settings persistence
- JAR packaging only (no AppImage or native installer)

## Goals / Non-Goals

**Goals:**
- Declarative keyboard shortcut system bound to composable actions
- Real-time AnnotatedString rendering in editable BasicTextField
- Folder CRUD UI + tag assignment per snippet
- Persistent theme toggle (dark/light/system) with KDE Breeze override
- AppImage distribution via Compose Multiplatform nativeDistributions

**Non-Goals:**
- Don't reimplement the highlighters (already shared with Android)
- Don't add a plugin system or LSP integration
- Don't port to Windows/macOS in this change

## Decisions

### Decision 1: Keyboard shortcuts via `onPreviewKeyEvent` + registry
- **Choice**: Single `ShortcutRegistry` object with `MutableMap<KeyShortcut, () -> Unit>`, installed at `Window { ... }` level via `onPreviewKeyEvent`
- **Alternatives considered**: `MenuBar` with keyboard-accelerated menu items (adds visual clutter); Compose's `LocalSoftwareKeyboardController` (too low-level)
- **Rationale**: `onPreviewKeyEvent` captures shortcuts before any focused composable. Registry pattern keeps bindings decoupled from UI and testable.

### Decision 2: Editable syntax highlighting via `VisualTransformation`
- **Choice**: Apply `VisualTransformation` on `BasicTextField` instead of `AnnotatedString` merging
- **Alternatives considered**: Merge highlighted `AnnotatedString` with cursor (complex cursor position math); Two-layer Text + transparent BasicTextField overlay (accessibility issues)
- **Rationale**: `VisualTransformation` takes the raw string and returns `TransformedText` with an `AnnotatedString` and offset mapping. Compose handles cursor placement automatically. Single source of truth for text.

### Decision 3: Theme persistence via local JSON file
- **Choice**: Store `theme_mode: "dark" | "light" | "system"` in `~/.config/darknote/settings.json`
- **Alternatives considered**: SQLDelight settings table (heavy for 1 value); `java.util.prefs` (platform-specific)
- **Rationale**: JSON file survives uninstall, is easy to debug, and can later expand to other settings (font size, tab width, etc.)

### Decision 4: Folder management in existing snippet list screen
- **Choice**: Add collapsible folder tree in left sidebar of the snippet list screen
- **Alternatives considered**: Separate dialog (too modal for frequent use); Navigation drawer (adds friction)
- **Rationale**: Kate/KWrite use a sidebar folder tree pattern. Users expect to see folders and snippets in the same view.

## Risks / Trade-offs

- **VisualTransformation performance** → AnnotatedString is recomputed on every keystroke. Mitigation: memoize with `remember(value, language)` and highlight only visible lines for large files.
- **Wayland key event capture** → Some key combos (Ctrl+Q, Alt+F4) may be intercepted by the window manager. Mitigation: document known conflicts and provide menu alternatives.
- **AppImage size** → JVM + Compose runtime ≈ 150MB. Mitigation: use ProGuard and avoid unnecessary dependencies.
- **KDE Breeze detection** → `XDG_CURRENT_DESKTOP` check may not detect all KDE sessions. Mitigation: always allow manual override via theme settings.
