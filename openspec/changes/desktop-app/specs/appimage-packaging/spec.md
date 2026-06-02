## ADDED Requirements

### Requirement: AppImage distribution
The system SHALL produce an AppImage artifact via Compose Multiplatform `nativeDistributions`.

#### Scenario: AppImage builds successfully
- **WHEN** user runs `./gradlew :apps:desktop:packageReleaseAppImage`
- **THEN** an AppImage file SHALL be produced in `apps/desktop/build/compose/binaries/`

#### Scenario: AppImage is executable on Linux
- **WHEN** user downloads and runs the AppImage on Arch Linux with KDE Plasma
- **THEN** the application SHALL launch correctly
- **THEN** the KDE Breeze detection SHALL work

### Requirement: KDE desktop integration metadata
The AppImage SHALL include proper desktop integration metadata.

#### Scenario: Desktop file is correct
- **WHEN** the AppImage is extracted or installed
- **THEN** the `.desktop` file SHALL contain:
  - Name: DarkNote
  - Comment: Modern snippet manager with Dropbox sync
  - Categories: Utility;
  - MimeType: text/plain;

#### Scenario: Icon is bundled
- **WHEN** the AppImage is built
- **THEN** it SHALL contain a PNG icon at the expected path

### Requirement: ProGuard optimization
The release build SHALL use ProGuard to minimize AppImage size.

#### Scenario: ProGuard reduces JAR size
- **WHEN** building the release AppImage
- **THEN** ProGuard SHALL run on the JAR
- **THEN** the final AppImage size SHALL be less than 200MB
