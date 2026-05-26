## ADDED Requirements

### Requirement: Global keyboard shortcuts
The system SHALL support the following global keyboard shortcuts regardless of which component is focused.

#### Scenario: Ctrl+N creates new snippet
- **WHEN** user presses Ctrl+N
- **THEN** system creates a new empty snippet
- **THEN** system navigates to the editor screen for that snippet

#### Scenario: Ctrl+S saves current snippet
- **WHEN** user presses Ctrl+S in the editor screen
- **THEN** system immediately saves the current snippet content and title

#### Scenario: Ctrl+F opens find dialog
- **WHEN** user presses Ctrl+F in the editor screen
- **THEN** system shows the find/replace dialog

#### Scenario: Ctrl+Shift+F focuses search bar
- **WHEN** user presses Ctrl+Shift+F in the list screen
- **THEN** system focuses the search bar

#### Scenario: Ctrl+Q quits application
- **WHEN** user presses Ctrl+Q
- **THEN** system exits the application

#### Scenario: Shortcuts do not fire when typing in text fields
- **WHEN** user is typing text in the editor or title field
- **THEN** single-key shortcuts SHALL NOT fire
- **THEN** modifier-key shortcuts (Ctrl+*) SHALL still fire

### Requirement: Declarative shortcut registry
The system SHALL use a central `ShortcutRegistry` that maps key combinations to actions.

#### Scenario: Shortcuts are testable
- **WHEN** a test registers a shortcut and simulates a key event
- **THEN** the corresponding action SHALL execute

#### Scenario: Duplicate shortcut registration logs warning
- **WHEN** two components register the same key combination
- **THEN** the system SHALL log a warning
- **THEN** the last registered action SHALL take precedence
