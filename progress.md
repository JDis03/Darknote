## 2026-08-07 02:26 — DarkNote
**Summary**: Ran clean build verification after the keyboard-aware editor fix. Executed ./gradlew clean followed by ./init.sh; all 175 tasks rebuilt from scratch and all tests passed with no regressions.
**Verified**: ./gradlew clean green; ./init.sh green (175 tasks executed, all tests pass).
**Completed**: none
---
---
## 2026-08-07 02:24 — DarkNote
**Summary**: Fixed Android editor keyboard covering typed text. Inspected the Obsidian reference APK's manifest and confirmed its MainActivity uses windowSoftInputMode=adjustResize (0x00000010). Switched DarkNote's MainActivity to adjustResize, applied imePadding() to the Scaffold content Surface, and restructured EditorScreen so the content region fills remaining viewport height via Box(weight(1f)); MarkdownPreview is independently scrollable and the edit BasicTextField uses bounded fillMaxSize(), giving it internal cursor-following scroll.
**Verified**: Inspected obsidian/base.apk manifest: md.obsidian.MainActivity uses windowSoftInputMode=0x00000010 (adjustResize). compileDebugKotlin green; full ./init.sh green with all tests passing. Pushed to main at 171eeef.
**Completed**: none
---
---
## 2026-08-01 03:46 — DarkNote
**Summary**: Added GFM-style Markdown table support: MdBlock.Table in shared/core MarkdownParser (with ColumnAlignment enum, table detection safely ordered in the parse loop), MarkdownSerializer Table case for the existing block-copy feature, and TableRenderer/TableCell composables in Android's MarkdownPreview (bold header row, per-column alignment, row padding, copy-button integration). Fixed unescaped regex literals and an internal-symbol Modifier.weight() import mistake from the user's proposed code during implementation.
**Verified**: shared/core tests: 40/40 MarkdownParserTest (10 new), 21/21 MarkdownSerializerTest (4 new); compileDebugKotlin green; apps:desktop:compileKotlin unaffected; full ./init.sh green. Pushed to main at 224c1bf.
**Completed**: none
---
---
## 2026-08-01 03:19 — DarkNote
**Summary**: Implemented block-level copy for MarkdownPreview (Notion/Obsidian/GitHub style): new MarkdownSerializer.kt in shared/core reconstructs raw markdown from MdBlock/MdInline AST for clipboard use. MarkdownPreview.kt wraps each rendered block with a copy button (always visible for code blocks, tap-to-reveal + long-press-to-copy for others) using Compose's LocalClipboardManager. Added 17 new serializer tests (exact output + semantic round-trip).
**Verified**: compileDebugKotlin succeeded; shared/core tests green (17/17 new MarkdownSerializerTest + 30/30 existing MarkdownParserTest); full ./init.sh green, no regressions. Pushed to main at d0632ab.
**Completed**: none
---
---
## 2026-07-31 20:46 — DarkNote
**Summary**: Final architecture: replaced richeditor-compose with own MarkdownParser-based real markdown render. EditorScreen.kt now has two clean modes — eye (edit: BasicTextField + syntax highlighting) and pencil (preview: MarkdownPreview using shared/core MarkdownParser AST rendered as real Compose UI with headings, code blocks, blockquotes, lists, inline styles). Zero external dependencies, zero reformatting risk, no sync logic. Pushed to main at 3039a17.
**Verified**: compileDebugKotlin green, full init.sh green, shared/core tests green, pushed to main (3039a17).
**Completed**: none
---
---
## 2026-07-31 20:09 — DarkNote
**Summary**: Architecture saga conclusion: the richeditor-compose WYSIWYG approach (commit 25bf35e) was correct all along. The VisualTransformation replacement was an architectural dead end — BasicTextField in Compose BOM 2024.06.00 ignores visualTransformation entirely. Spent multiple commits debugging (blue text test, mode indicator bar) before confirming the framework-level limitation, then reverted to the working richeditor-compose RichTextEditor. The markdown-reformatting trade-off is standard across all AST-based editors and was accepted by the user.
**Verified**: After full revert to richeditor-compose: compileDebugKotlin succeeded, full ./init.sh green, no regressions. Committed as b39f3a2.
**Completed**: none
---
---
## 2026-07-31 19:48 — DarkNote
**Summary**: Fixed fenced code blocks being invisible in live markdown preview — root cause was Color(0x1F66BB6A) background (12% opacity, imperceptible). Changed to: dimmed fence markers (grey), visible ~15% grey code block background + monospace + normal text colour (not green), pinkish inline code colour — all matching Obsidian's visual treatment. Split regex into 3 capture groups to style fences/opening/content separately. Updated user-report test to verify background is actually visible (non-Transparent).
**Verified**: Compile succeeded, 13/13 unit tests green (MarkdownVisualTransformationTest), full ./init.sh green. Committed as 7243860.
**Completed**: none
---
---
## 2026-07-31 19:28 — DarkNote
**Summary**: Reverted the richeditor-compose WYSIWYG editor entirely after user identified the AST round-trip approach unfixably reformats untouched markdown on every edit. Replaced with the real Obsidian/Joplin architecture: a single BasicTextField bound to contentField.text in both eye/pencil modes, with only the VisualTransformation (identity offset mapping, pure display decoration) differing. New MarkdownLiveStyle/MarkdownVisualTransformation.kt provides live markdown styling (dimmed markers, scaled headers, styled emphasis/lists/code/quotes/links) without ever touching the source string. Added 12 tests pinning byte-for-byte preservation, including the exact blockquote paste that surfaced the original bug.
**Verified**: ./gradlew :apps:android:compileDebugKotlin succeeded; :apps:android:testDebugUnitTest ran MarkdownVisualTransformationTest with tests=12 failures=0 errors=0 (verified via JUnit XML, not the misleading 'N actionable tasks' Gradle line); full ./init.sh green, no regressions. Committed as 2a58d76.
**Completed**: none
---
---
## 2026-07-31 18:50 — DarkNote
**Summary**: Fixed two bugs the user found after testing the WYSIWYG editor commit: (1) eye/pencil view mapping was inverted — WYSIWYG is now correctly the default view with the toolbar, raw source is the toggle target; (2) a data-mutation bug where entering WYSIWYG mode silently rewrote pasted markdown via an unguarded bidirectional sync echo, fixed with a one-shot suppression flag around setMarkdown() loads.
**Verified**: ./gradlew :apps:android:compileDebugKotlin succeeded; full ./init.sh green, 175/175 tests passing. Committed as 25bf35e.
**Completed**: none
---
---
## 2026-07-31 17:40 — DarkNote
**Summary**: Implemented Android WYSIWYG markdown editor (eye mode) in EditorScreen.kt using richeditor-compose:1.0.0-rc05-k2 (version pinned for Kotlin 2.0.0 compat, not latest 1.0.0 tag). Replaced read-only MarkdownPreview with editable RichTextEditor, bidirectionally synced to contentField.text (source of truth) via setMarkdown()/toMarkdown() with trimEnd()-normalized comparison to prevent auto-save loops. Added a format toolbar (bold/italic/strikethrough/code/lists) using only README-confirmed stable RichTextState API. Deleted the now-dead MarkdownPreview.kt composable.
**Verified**: ./gradlew :apps:android:compileDebugKotlin succeeded; full ./init.sh ran twice (before and after final comment edit), both BUILD SUCCESSFUL with 175/175 tests passing, no regressions vs session start baseline (also 175 green).
**Completed**: none
---
---
## 2026-07-29 23:04 — DarkNote
**Summary**: Audited DarkNote editor for improvements/regressions/copy-paste/markdown. Found and fixed: Android's live editor had zero syntax highlighting (added SyntaxHighlightTransformation, identity-mapped VisualTransformation, safe for paste). Built a real Markdown preview toggle for Android: MarkdownParser.kt in shared/core (stateful two-phase parser, design informed by studying CodeMirror's MIT-licensed markdown.js bundled in the Obsidian APK, but original Kotlin code) plus MarkdownPreview.kt composable and a toggle in EditorScreen.kt's TopAppBar. Confirmed copy/paste itself has no regression risk (plain-string TextField state + identity offset mapping everywhere).
**Verified**: ./gradlew :apps:android:compileDebugKotlin, :shared:core:testDebugUnitTest, and full ./init.sh all green; 175 total tests passing (was 138 at session start). Two commits pushed to main (c22f74e syntax highlighting, 8fa5dd0 markdown preview).
**Completed**: none
---
---
# Session Progress Log

## Current State

**Last Updated:** YYYY-MM-DD HH:MM
**Session ID:** [optional]
**Active Feature:** [feat-XXX - Feature Name]

## Status

### What's Done

- [x] [Completed item 1]
- [x] [Completed item 2]

### What's In Progress

- [ ] [Current work item]
  - Details: [specific task]
  - Blockers: [if any]

### What's Next

1. [Next action item]
2. [Following action item]

## Blockers / Risks

- [ ] [Blocker 1]: [description, impact]
- [ ] [Risk 1]: [description, mitigation]

## Decisions Made

- **[Decision 1]**: [description]
  - Context: [why this decision was made]
  - Alternatives considered: [what else was discussed]

## Files Modified This Session

- `path/to/file1.ts` - [brief description of change]
- `path/to/file2.ts` - [brief description of change]

## Evidence of Completion

- [ ] Tests pass: `[command and output]`
- [ ] Type check clean: `[command and output]`
- [ ] Manual verification: `[what was tested]`

## Notes for Next Session

[Free-form notes that will help the next session pick up context]
