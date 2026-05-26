## ADDED Requirements

### Requirement: Theme toggle in settings
The system SHALL provide a theme selector with three options: Dark, Light, and System (follow KDE).

#### Scenario: Theme selector visible in settings
- **WHEN** user opens the Settings dialog
- **THEN** a theme selector with "Dark", "Light", "System" options SHALL be visible

#### Scenario: Dark theme renders correctly
- **WHEN** user selects "Dark" theme
- **THEN** the editor background SHALL be dark
- **THEN** text SHALL be light (onSurface)
- **THEN** syntax colors SHALL use the Darcula-inspired scheme

#### Scenario: Light theme renders correctly
- **WHEN** user selects "Light" theme
- **THEN** the editor background SHALL be light
- **THEN** text SHALL be dark
- **THEN** syntax colors SHALL use the Light scheme

#### Scenario: System theme follows KDE Breeze
- **WHEN** user selects "System" and the desktop is KDE Plasma with Breeze Dark
- **THEN** the app SHALL use a dark color scheme
- **WHEN** the desktop is KDE Plasma with Breeze Light
- **THEN** the app SHALL use a light color scheme

### Requirement: Persistent theme preference
The selected theme SHALL persist across app restarts.

#### Scenario: Theme survives restart
- **WHEN** user selects "Light" theme and restarts the app
- **THEN** the app SHALL start with the Light theme

#### Scenario: Settings file location
- **WHEN** the app first starts
- **THEN** it SHALL create `~/.config/darknote/settings.json`
- **THEN** the default theme SHALL be "system"
