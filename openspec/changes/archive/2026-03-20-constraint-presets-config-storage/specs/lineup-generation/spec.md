## MODIFIED Requirements

### Requirement: Generate lineup via API
The system SHALL expose `POST /api/lineups/generate` to produce up to 6 lineup candidates for a given team, strategy, and optional player constraints. The response SHALL be a `List<Lineup>` (1–6 items), best candidate first; only the first is persisted to team history.

#### Scenario: Successful generation with preset balanced strategy
- **WHEN** client posts `{ "teamId": "team-001", "strategyType": "preset", "preset": "balanced" }` and the team has ≥ 8 players
- **THEN** system returns HTTP 200 with a JSON array of 1–6 Lineup objects, each containing 4 pairs (D1–D4), `valid: true`, and `violationMessages: []`

#### Scenario: Successful generation with custom natural language strategy
- **WHEN** client posts `{ "teamId": "team-001", "strategyType": "custom", "naturalLanguage": "让前三线尽量强" }` and the team has ≥ 8 players
- **THEN** system invokes AI service with the natural language as strategy context, returns HTTP 200 with a JSON array where the AI-selected lineup is first

#### Scenario: Successful generation with position pin
- **WHEN** client posts `{ "teamId": "t1", "strategyType": "preset", "preset": "balanced", "pinPlayers": { "p1": "D1" } }`
- **THEN** every Lineup in the response has player "p1" in the D1 pair

#### Scenario: AI service unavailable — fallback to heuristic
- **WHEN** client requests generation but the Zhipu AI API call fails or times out (> 3s)
- **THEN** system selects lineups using deterministic heuristic, returns HTTP 200 with a valid Lineup array, and the first element includes `"aiUsed": false`

#### Scenario: With includePlayers and excludePlayers constraints
- **WHEN** client posts valid `includePlayers` and `excludePlayers` lists
- **THEN** system filters the roster before enumeration and returns only lineups that satisfy those constraints

#### Scenario: Insufficient players
- **WHEN** client posts a generate request for a team with fewer than 8 players
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "队伍球员不足8人，无法生成排阵" }`

#### Scenario: No valid lineup exists
- **WHEN** all player combinations fail at least one hard constraint
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "无法生成满足约束的排阵" }`

#### Scenario: Team not found
- **WHEN** client posts a generate request with a non-existent teamId
- **THEN** system returns HTTP 404 with `{ "code": "NOT_FOUND", "message": "队伍不存在" }`

## ADDED Requirements

### Requirement: Constraint preset selector on lineup generation page
The lineup generation page SHALL display a constraint preset selector above the player constraint list. Users SHALL be able to load a saved preset to pre-populate constraints, and save current constraints as a new named preset.

#### Scenario: Load preset populates constraints
- **WHEN** user selects a saved preset from the dropdown
- **THEN** the player constraint selector SHALL update to reflect the preset's excludePlayers, includePlayers, and pinPlayers values

#### Scenario: Save current constraints as preset
- **WHEN** user enters a preset name and clicks save
- **THEN** the system SHALL persist the current constraint state as a new preset and add it to the dropdown

#### Scenario: No presets — dropdown shows placeholder
- **WHEN** the team has no saved presets
- **THEN** the preset dropdown SHALL show "暂无预设" and the save option remains available

### Requirement: Quick navigation link to saved lineups
The lineup generation page SHALL display a link that navigates directly to the team's saved lineups history page.

#### Scenario: Link visible on lineup generation page
- **WHEN** user is on the lineup generation page with a team selected
- **THEN** a "查看已保存排阵 →" link SHALL be visible and navigate to `/teams/:id/lineups` when clicked

#### Scenario: Link not shown when no team is selected
- **WHEN** no team is selected on the lineup generation page
- **THEN** the navigation link SHALL NOT be shown
