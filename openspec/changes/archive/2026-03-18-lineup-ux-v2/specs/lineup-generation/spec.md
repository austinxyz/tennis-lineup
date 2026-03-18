## MODIFIED Requirements

### Requirement: Strategy types
The system SHALL support two strategy types: `preset` and `custom`.

#### Scenario: Preset balanced strategy ranks by per-line evenness toward 10.125
- **WHEN** `strategyType = "preset"` and `preset = "balanced"` and AI is unavailable
- **THEN** system ranks all valid candidates by the sum of `|pair.combinedUtr - 10.125|` across D1–D4 (ascending — lower deviation = better); 方案 1 has the smallest total deviation from 10.125 per pair

#### Scenario: Preset aggressive strategy maximizes top-three lines
- **WHEN** `strategyType = "preset"` and `preset = "aggressive"` and AI is unavailable
- **THEN** system selects the valid lineup with highest sum of D1 + D2 + D3 combined UTRs

#### Scenario: Custom strategy with AI available
- **WHEN** `strategyType = "custom"` and `naturalLanguage` is provided and AI is available
- **THEN** system passes the natural language and valid lineup list to Zhipu AI, returns AI-selected lineup

#### Scenario: Custom strategy AI fallback
- **WHEN** `strategyType = "custom"` and AI is unavailable
- **THEN** system falls back to `balanced` heuristic and includes `"aiUsed": false` in response

---

### Requirement: Lineup combination algorithm
The system SHALL use a backtracking algorithm to enumerate all valid 4-pair combinations from the team's player roster, then assign positions D1–D4 in descending combined UTR order. Before applying strategy-specific sorting, all candidates SHALL be primarily ranked by proximity to the 40.5 total UTR cap.

#### Scenario: Primary sort: candidates closest to 40.5 ranked first
- **WHEN** multiple valid candidates exist
- **THEN** candidates are sorted by `40.5 - totalUtr` ascending (candidates closest to the 40.5 cap rank higher) before strategy-specific secondary sorting is applied

#### Scenario: 8-player team produces combinations within 5 seconds
- **WHEN** a team has exactly 8 eligible players
- **THEN** the system generates all valid combinations and returns a result within 5 seconds

#### Scenario: Position assignment follows UTR order
- **WHEN** 4 pairs are selected
- **THEN** the pair with highest combined UTR is assigned D1, next D2, next D3, lowest D4

---

### Requirement: Generate lineup via API
The system SHALL expose `POST /api/lineups/generate` to produce up to 6 lineup candidates for a given team and strategy. The response SHALL be a `List<Lineup>` (1–6 items), best candidate first, only the first persisted to team history.

#### Scenario: Successful generation with preset balanced strategy
- **WHEN** client posts `{ "teamId": "team-001", "strategyType": "preset", "preset": "balanced" }` and the team has ≥ 8 players
- **THEN** system returns HTTP 200 with a JSON array of 1–6 Lineup objects, each containing 4 pairs (D1–D4), `valid: true`, and `violationMessages: []`

#### Scenario: Successful generation with position pin
- **WHEN** client posts `{ "teamId": "t1", "strategyType": "preset", "preset": "balanced", "pinPlayers": { "p1": "D1" } }`
- **THEN** every Lineup in the response has player "p1" in the D1 pair

#### Scenario: AI service unavailable — fallback to heuristic
- **WHEN** client requests generation but the Zhipu AI API call fails or times out (> 3s)
- **THEN** system selects lineup using deterministic heuristic, returns HTTP 200 with a valid Lineup array, and includes `"aiUsed": false` in the response

#### Scenario: Insufficient players
- **WHEN** client posts a generate request for a team with fewer than 8 players
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "队伍球员不足8人，无法生成排阵" }`

#### Scenario: No valid lineup exists
- **WHEN** all player combinations fail at least one hard constraint
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "无法生成满足约束的排阵" }`

#### Scenario: Team not found
- **WHEN** client posts a generate request with a non-existent teamId
- **THEN** system returns HTTP 404 with `{ "code": "NOT_FOUND", "message": "队伍不存在" }`
