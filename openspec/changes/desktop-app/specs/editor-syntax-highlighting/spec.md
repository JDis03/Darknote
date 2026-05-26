## ADDED Requirements

### Requirement: Real-time syntax highlighting in editable mode
The editor SHALL display syntax-highlighted text while the user types, using the existing `SyntaxHighlighterFactory` highlighters.

#### Scenario: Syntax highlighting renders while typing
- **WHEN** user types code in the editor with a language selected (e.g., `fun main()`)
- **THEN** keywords SHALL appear in the configured keyword color
- **THEN** strings SHALL appear in the configured string color

#### Scenario: Highlighting updates on every keystroke
- **WHEN** user deletes or inserts a character
- **THEN** the highlighting SHALL update within the same frame

#### Scenario: Cursor position is correct with highlighted text
- **WHEN** user clicks in the middle of highlighted text
- **THEN** the cursor SHALL appear at the exact click position
- **THEN** the cursor SHALL have the configured cursor color (primary)

#### Scenario: Language change triggers re-highlight
- **WHEN** user changes the language selector from "Kotlin" to "Python"
- **THEN** the editor SHALL re-highlight with Python syntax rules

#### Scenario: No language selected shows plain text
- **WHEN** language is null or empty
- **THEN** the editor SHALL display plain unhighlighted text

### Requirement: VisualTransformation approach
The system SHALL implement editable syntax highlighting using `VisualTransformation`, converting raw text into an `AnnotatedString` with syntax tokens.

#### Scenario: VisualTransformation returns correct offset mapping
- **WHEN** a transformation is applied to text with multi-byte characters
- **THEN** the cursor offset mapping SHALL be 1:1 (no characters are added or removed)

#### Scenario: Selection works correctly
- **WHEN** user selects a range of text that spans highlighted tokens
- **THEN** the selection highlight SHALL be visible above the syntax colors
