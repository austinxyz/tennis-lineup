## MODIFIED Requirements

### Requirement: Generate lineup via API
The system SHALL expose `POST /api/lineups/generate` to produce up to 6 valid lineup candidates for a given team, strategy, and optional player constraints. The response SHALL be a JSON array of Lineup objects (1–6 elements), ordered best-first by the selected strategy.

#### Scenario: Successful generation with preset balanced strategy
- **WHEN** client posts `{ "teamId": "team-001", "strategyType": "preset", "preset": "balanced" }` and the team has ≥ 8 players
- **THEN** system returns HTTP 200 with a JSON array of 1–6 Lineup objects, each containing 4 pairs (D1–D4), `valid: true`, and `violationMessages: []`, ordered by ascending variance (best balanced first)

#### Scenario: Successful generation with custom natural language strategy
- **WHEN** client posts `{ "teamId": "team-001", "strategyType": "custom", "naturalLanguage": "让前三线尽量强" }` and the team has ≥ 8 players
- **THEN** system invokes AI service with the natural language as strategy context, returns HTTP 200 with a JSON array where the AI-selected lineup is first

#### Scenario: AI service unavailable — fallback to heuristic
- **WHEN** client requests generation but the Zhipu AI API call fails or times out (> 3s)
- **THEN** system selects lineups using deterministic heuristic, returns HTTP 200 with a valid Lineup array, and the first element includes `"aiUsed": false`

#### Scenario: Insufficient players
- **WHEN** client posts a generate request for a team with fewer than 8 players
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "队伍球员不足8人，无法生成排阵" }`

#### Scenario: No valid lineup exists
- **WHEN** all player combinations fail at least one hard constraint
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "无法生成满足约束的排阵" }`

#### Scenario: Team not found
- **WHEN** client posts a generate request with a non-existent teamId
- **THEN** system returns HTTP 404 with `{ "code": "NOT_FOUND", "message": "队伍不存在" }`

#### Scenario: With includePlayers and excludePlayers constraints
- **WHEN** client posts valid `includePlayers` and `excludePlayers` lists
- **THEN** system filters the roster before enumeration and returns only lineups that satisfy those constraints
