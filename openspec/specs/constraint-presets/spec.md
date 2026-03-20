## Requirements

### Requirement: Save and manage constraint presets per team
The system SHALL allow users to save named constraint configurations (excludePlayers, includePlayers, pinPlayers) as presets per team. Presets SHALL be persisted in `tennis-config.json` under `ConfigData.constraintPresets` (keyed by teamId) and available across sessions.

#### Scenario: Create a new preset
- **WHEN** user provides a name and current constraint state and clicks save
- **THEN** the system SHALL call `POST /api/teams/{id}/constraint-presets` with `{ name, excludePlayers, includePlayers, pinPlayers }`
- **AND** the server SHALL persist the preset in `ConfigData` with a generated `id` ("preset-" + nanoTime) and `createdAt` timestamp
- **AND** return HTTP 200 with the created preset

#### Scenario: List presets for a team
- **WHEN** a request is made to `GET /api/teams/{id}/constraint-presets`
- **THEN** all saved presets for the team are returned ordered by `createdAt` descending (newest first)

#### Scenario: Team has no presets
- **WHEN** `GET /api/teams/{id}/constraint-presets` is called for a team with no presets
- **THEN** the system SHALL return `[]` with HTTP 200

#### Scenario: Load a preset
- **WHEN** user selects a saved preset from the `ConstraintPresetSelector` dropdown and clicks 加载
- **THEN** the `PlayerConstraintSelector` states are updated via `loadStates()` to reflect the preset's excludePlayers, includePlayers, and pinPlayers values

#### Scenario: Delete a preset
- **WHEN** user clicks 删除 on a selected preset
- **THEN** the system SHALL call `DELETE /api/teams/{id}/constraint-presets/{presetId}`
- **AND** the server SHALL remove the preset from `ConfigData` and return HTTP 204
- **AND** the dropdown selection is cleared

#### Scenario: Preset not found on delete
- **WHEN** `DELETE /api/teams/{id}/constraint-presets/{presetId}` is called with a non-existent preset ID
- **THEN** the system SHALL return HTTP 404 with `{ "code": "NOT_FOUND", "message": "约束预设不存在" }`

#### Scenario: Preset with deleted players
- **WHEN** a preset references a player ID that no longer exists in the team
- **THEN** those player IDs SHALL be silently ignored when loading the preset on the frontend
- **AND** the frontend SHALL display a warning: "部分球员已不在队伍中，相关约束已跳过"

---

### Requirement: Constraint preset data model
The `ConstraintPreset` model SHALL contain: `id` (String), `name` (String), `createdAt` (Instant), `excludePlayers` (List<String>), `includePlayers` (List<String>), `pinPlayers` (Map<String, String> — player ID to position D1–D4).

Presets are stored in `ConfigData` (the config file), NOT in `TeamData` (the core data file), so player roster changes do not affect preset storage.

---

### Requirement: Preset UI auto-loads on team selection
The `ConstraintPresetSelector` component SHALL fetch presets from the backend whenever a team is selected on the lineup generation page.

#### Scenario: Presets fetched on team change
- **WHEN** user selects a different team
- **THEN** `GET /api/teams/{id}/constraint-presets` is called automatically
- **AND** the preset dropdown is populated with the fetched presets
