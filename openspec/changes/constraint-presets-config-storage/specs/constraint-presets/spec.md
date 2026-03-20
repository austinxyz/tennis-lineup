## ADDED Requirements

### Requirement: Save and manage constraint presets per team
The system SHALL allow users to save named constraint configurations (excludePlayers, includePlayers, pinPlayers) as presets per team. Presets SHALL be persisted in `tennis-config.json` under `ConfigData.constraintPresets` (keyed by teamId) and available across sessions.

#### Scenario: Create a new preset
- **WHEN** user provides a name and current constraint state and clicks save
- **THEN** the system SHALL call `POST /api/teams/{id}/constraint-presets` with `{ name, excludePlayers, includePlayers, pinPlayers }`
- **AND** the server SHALL persist the preset in `ConfigData` with a generated `id` ("preset-" + nanoTime) and `createdAt` timestamp and return HTTP 201 with the created preset

#### Scenario: List presets for a team
- **WHEN** user opens the lineup generation page for a team
- **THEN** the system SHALL call `GET /api/teams/{id}/constraint-presets`
- **AND** return all saved presets for the team ordered by `createdAt` descending

#### Scenario: Load a preset
- **WHEN** user selects a saved preset from the dropdown
- **THEN** the constraint selector SHALL be populated with the preset's excludePlayers, includePlayers, and pinPlayers values

#### Scenario: Delete a preset
- **WHEN** user clicks delete on a preset and confirms
- **THEN** the system SHALL call `DELETE /api/teams/{id}/constraint-presets/{presetId}`
- **AND** the server SHALL remove the preset from `ConfigData` and return HTTP 204

#### Scenario: Preset with deleted players
- **WHEN** a preset references a player ID that no longer exists in the team
- **THEN** those player IDs SHALL be silently ignored when loading the preset
- **AND** the frontend SHALL display a warning: "部分球员已不在队伍中，相关约束已跳过"

#### Scenario: Team has no presets
- **WHEN** `GET /api/teams/{id}/constraint-presets` is called for a team with no presets
- **THEN** the system SHALL return `[]` with HTTP 200

#### Scenario: Preset not found on delete
- **WHEN** `DELETE /api/teams/{id}/constraint-presets/{presetId}` is called with a non-existent preset ID
- **THEN** the system SHALL return HTTP 404 with `{ "code": "NOT_FOUND", "message": "约束预设不存在" }`
