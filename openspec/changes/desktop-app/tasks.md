## 1. Editable Syntax Highlighting (VisualTransformation)

- [ ] 1.1 Create `SyntaxHighlightTransformation` class implementing `VisualTransformation`
- [ ] 1.2 Wire it into `CodeEditor.kt` BasicTextField via `visualTransformation` parameter
- [ ] 1.3 Remove read-only mode distinction for highlighting (both modes use VisualTransformation)
- [ ] 1.4 Verify cursor position and text selection still work correctly

## 2. Keyboard Shortcuts

- [ ] 2.1 Create `ShortcutRegistry` class with `register(key, action)` and `handleEvent(event): Boolean`
- [ ] 2.2 Install `onPreviewKeyEvent` on `Window` in `Main.kt`
- [ ] 2.3 Bind Ctrl+N (new snippet), Ctrl+S (save), Ctrl+F (find), Ctrl+Shift+F (search), Ctrl+Q (quit)
- [ ] 2.4 Add unit tests for ShortcutRegistry

## 3. Folder Management

- [ ] 3.1 Add folder sidebar composable to `SnippetListScreen`
- [ ] 3.2 Implement folder CRUD dialogs (create, rename, delete)
- [ ] 3.3 Wire folder selection to filter snippet list
- [ ] 3.4 Implement tag assignment UI in editor screen

## 4. Theme Settings

- [ ] 4.1 Create `SettingsManager` that reads/writes `~/.config/darknote/settings.json`
- [ ] 4.2 Create `ThemeMode` enum and `ThemeViewModel` with persistent state
- [ ] 4.3 Add settings dialog with theme selector (Dark/Light/System)
- [ ] 4.4 Wire theme selection to `MaterialTheme` colorScheme in `Main.kt`
- [ ] 4.5 Add theme to DI module as a singleton

## 5. AppImage Packaging

- [ ] 5.1 Set up `packageReleaseAppImage` target in `build.gradle.kts`
- [ ] 5.2 Add icon file at `src/main/resources/icon.png`
- [ ] 5.3 Configure ProGuard rules for desktop release build
- [ ] 5.4 Build and verify AppImage artifact

## 6. Verification & Polish

- [ ] 6.1 Compile desktop JAR (`./gradlew :apps:desktop:jar`)
- [ ] 6.2 Run all existing tests (88 total)
- [ ] 6.3 Run `openspec status --change desktop-app` to verify completion
