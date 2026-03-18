## MODIFIED Requirements

### Requirement: Generate lineup via API
The system SHALL expose `POST /api/lineups/generate` to produce multiple candidate lineups for a given team and strategy. The response SHALL be an array of up to 6 Lineup objects (no wrapper, no auto-persistence).

#### Scenario: Successful generation returns multiple candidates
- **WHEN** client posts `{ "teamId": "team-001", "strategyType": "preset", "preset": "balanced" }` and the team has ≥ 8 players
- **THEN** system returns HTTP 200 with an array of up to 6 Lineup objects, each containing 4 pairs (D1–D4), `valid: true`, and `violationMessages: []`; the lineups are NOT saved to storage automatically

#### Scenario: Successful generation with custom natural language strategy
- **WHEN** client posts `{ "teamId": "team-001", "strategyType": "custom", "naturalLanguage": "让前三线尽量强" }` and the team has ≥ 8 players
- **THEN** system invokes AI service with the natural language as strategy context, returns HTTP 200 with an array of valid Lineup objects (not persisted)

#### Scenario: AI service unavailable — fallback to heuristic
- **WHEN** client requests generation but the Zhipu AI API call fails or times out (> 3s)
- **THEN** system selects lineup using deterministic heuristic (balanced → min variance; aggressive → max D1+D2+D3 UTR), returns HTTP 200 with a valid Lineup array, and the first lineup includes `"aiUsed": false`

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

### Requirement: Lineup combination algorithm
The system SHALL enumerate 8-player subsets from the eligible roster, sorted by total UTR proximity to 40.5 (descending, cap-valid first). It SHALL first process top-20 subsets; if valid results < 6, extend to top-40. Within each subset, backtracking is used to find valid 4-pair arrangements.

#### Scenario: Subsets sorted by UTR proximity to 40.5 cap
- **WHEN** the roster has more than 8 eligible players
- **THEN** the system enumerates all C(n,8) subsets, places subsets with totalUtr ≤ 40.5 before those exceeding the cap, and within cap-valid subsets sorts by totalUtr descending (highest first)

#### Scenario: Top-20 subsets processed first
- **WHEN** roster has > 8 players and top-20 subsets yield ≥ 6 valid lineups after constraint filtering
- **THEN** the system stops after processing the 20th subset and returns results without processing further subsets

#### Scenario: Expand to top-40 when results insufficient
- **WHEN** after processing top-20 subsets the filtered valid lineup count is < 6
- **THEN** the system continues processing subsets 21–40 before applying final filters

#### Scenario: 8-player roster skips subset enumeration
- **WHEN** the eligible roster has exactly 8 players
- **THEN** the system runs backtracking directly on those 8 players without subset enumeration

#### Scenario: Position assignment follows UTR order
- **WHEN** 4 pairs are selected from a subset
- **THEN** the pair with highest combined UTR is assigned D1, next D2, next D3, lowest D4
