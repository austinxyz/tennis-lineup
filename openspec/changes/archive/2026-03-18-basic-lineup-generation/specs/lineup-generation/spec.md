## ADDED Requirements

### Requirement: Generate lineup via API
The system SHALL expose `POST /api/lineups/generate` to produce a valid lineup for a given team and strategy. The response SHALL be the saved Lineup object directly (no wrapper).

#### Scenario: Successful generation with preset balanced strategy
- **WHEN** client posts `{ "teamId": "team-001", "strategyType": "preset", "preset": "balanced" }` and the team has ≥ 8 players
- **THEN** system returns HTTP 200 with a Lineup object containing 4 pairs (D1–D4), `valid: true`, and `violationMessages: []`

#### Scenario: Successful generation with custom natural language strategy
- **WHEN** client posts `{ "teamId": "team-001", "strategyType": "custom", "naturalLanguage": "让前三线尽量强" }` and the team has ≥ 8 players
- **THEN** system invokes AI service with the natural language as strategy context, returns HTTP 200 with a valid Lineup object

#### Scenario: AI service unavailable — fallback to heuristic
- **WHEN** client requests generation but the Zhipu AI API call fails or times out (> 3s)
- **THEN** system selects lineup using deterministic heuristic (balanced → min variance; aggressive → max D1+D2+D3 UTR), returns HTTP 200 with a valid Lineup, and includes `"aiUsed": false` in the response

#### Scenario: Insufficient players
- **WHEN** client posts a generate request for a team with fewer than 8 players
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "队伍球员不足8人，无法生成排阵" }`

#### Scenario: No valid lineup exists
- **WHEN** all player combinations fail at least one hard constraint
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "无法生成满足约束的排阵" }`

#### Scenario: Team not found
- **WHEN** client posts a generate request with a non-existent teamId
- **THEN** system returns HTTP 404 with `{ "code": "NOT_FOUND", "message": "队伍不存在" }`

---

### Requirement: Hard constraint validation
The system SHALL validate all hard constraints and reject any lineup that violates any of them.

#### Scenario: UTR ordering constraint enforced
- **WHEN** a candidate lineup has D1 combined UTR < D2 combined UTR
- **THEN** the lineup is excluded from valid candidates (not returned to user)

#### Scenario: Total UTR cap enforced
- **WHEN** the sum of 8 players' individual UTRs exceeds 40.5
- **THEN** the lineup is excluded from valid candidates

#### Scenario: Minimum female players enforced
- **WHEN** a candidate lineup has fewer than 2 female players on court
- **THEN** the lineup is excluded from valid candidates

#### Scenario: Partner UTR gap enforced
- **WHEN** any pair in a candidate lineup has |player1.utr - player2.utr| > 3.5
- **THEN** that pair is pruned during backtracking (not used in any lineup)

#### Scenario: D4 verified doubles UTR enforced
- **WHEN** either player in the D4 pair has `verified: false`
- **THEN** the lineup is excluded from valid candidates

#### Scenario: Player uniqueness enforced
- **WHEN** the algorithm assigns a player to more than one pair
- **THEN** that assignment is rejected during backtracking

---

### Requirement: Lineup combination algorithm
The system SHALL use a backtracking algorithm to enumerate all valid 4-pair combinations from the team's player roster, then assign positions D1–D4 in descending combined UTR order.

#### Scenario: 8-player team produces combinations within 5 seconds
- **WHEN** a team has exactly 8 eligible players
- **THEN** the system generates all valid combinations and returns a result within 5 seconds

#### Scenario: Position assignment follows UTR order
- **WHEN** 4 pairs are selected, one combination is returned
- **THEN** the pair with highest combined UTR is assigned D1, next D2, next D3, lowest D4

---

### Requirement: Strategy types
The system SHALL support two strategy types: `preset` and `custom`.

#### Scenario: Preset balanced strategy selects min-variance lineup
- **WHEN** `strategyType = "preset"` and `preset = "balanced"` and AI is unavailable
- **THEN** system selects the valid lineup with smallest variance across D1–D4 combined UTRs

#### Scenario: Preset aggressive strategy maximizes top-three lines
- **WHEN** `strategyType = "preset"` and `preset = "aggressive"` and AI is unavailable
- **THEN** system selects the valid lineup with highest sum of D1 + D2 + D3 combined UTRs

#### Scenario: Custom strategy with AI available
- **WHEN** `strategyType = "custom"` and `naturalLanguage` is provided and AI is available
- **THEN** system passes the natural language and valid lineup list to Zhipu AI, returns AI-selected lineup

#### Scenario: Custom strategy AI fallback
- **WHEN** `strategyType = "custom"` and AI is unavailable
- **THEN** system falls back to `balanced` heuristic and includes `"aiUsed": false` in response
