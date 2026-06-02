## ADDED Requirements

### Requirement: Folder sidebar in snippet list
The list screen SHALL show a collapsible folder tree in a left sidebar, similar to Kate/KWrite.

#### Scenario: Folders appear in sidebar
- **WHEN** user opens the snippet list screen
- **THEN** a sidebar SHALL show all folders sorted alphabetically
- **THEN** "All Snippets" SHALL be the default selected item

#### Scenario: Clicking folder filters snippets
- **WHEN** user clicks a folder in the sidebar
- **THEN** the snippet list SHALL show only snippets in that folder

#### Scenario: Clicking "All Snippets" shows all
- **WHEN** user clicks "All Snippets" in the sidebar
- **THEN** all snippets SHALL be visible

### Requirement: Folder CRUD
The system SHALL support creating, renaming, and deleting folders.

#### Scenario: Create new folder
- **WHEN** user clicks "New Folder" button in the sidebar
- **THEN** a dialog SHALL prompt for folder name
- **WHEN** user enters a name and confirms
- **THEN** the folder SHALL appear in the sidebar
- **THEN** the snippet editor SHALL allow assigning this folder

#### Scenario: Rename folder
- **WHEN** user right-clicks a folder and selects "Rename"
- **THEN** a dialog SHALL allow editing the folder name

#### Scenario: Delete folder
- **WHEN** user right-clicks a folder and selects "Delete"
- **THEN** a confirmation dialog SHALL appear
- **WHEN** user confirms
- **THEN** the folder SHALL be deleted
- **THEN** snippets in that folder SHALL NOT be deleted (they become folderless)

### Requirement: Tag assignment
The system SHALL support adding and removing tags on each snippet.

#### Scenario: Add tag to snippet
- **WHEN** user opens a snippet in the editor
- **THEN** a "Tags" field SHALL be visible
- **WHEN** user types a tag name and presses Enter
- **THEN** the tag SHALL be added to the snippet

#### Scenario: Remove tag from snippet
- **WHEN** user clicks the "×" on an existing tag chip
- **THEN** the tag SHALL be removed from the snippet

#### Scenario: Filter snippets by tag
- **WHEN** user clicks a tag in the sidebar or tag list
- **THEN** the snippet list SHALL filter to show only snippets with that tag
