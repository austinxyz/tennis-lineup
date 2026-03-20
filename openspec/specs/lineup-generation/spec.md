## Requirements

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
The system SHALL use a two-level algorithm to generate lineup candidates:

**Level 1 — 8-player subset enumeration:** All C(n, 8) subsets of the eligible roster are enumerated. Locked players (includePlayers ∪ pinPlayers keys) are always present in every subset. Subsets are sorted by totalUtr proximity to the 40.5 cap: cap-valid subsets (totalUtr ≤ 40.5) first, then by highest totalUtr descending. The top-20 subsets are processed first; if fewer than 6 results satisfy pin constraints after the top-20, the algorithm extends to top-40 subsets.

**Level 2 — Pair-level backtracking within each subset:** All valid pairs (partner UTR gap ≤ 3.5) are generated and sorted by combined UTR descending. Without pin constraints, only the top-20 pairs are considered for the first two pair slots (which become D1/D2 after UTR-based position assignment); all pairs are used for the remaining slots. With pin constraints, all pairs are used for every slot to ensure pinned-pair combinations are never missed. Positions D1–D4 are assigned in descending combined UTR order after all 4 pairs are selected.

#### Scenario: Primary sort: candidates closest to 40.5 ranked first
- **WHEN** multiple valid candidates exist
- **THEN** candidates SHALL be sorted by `40.5 - totalUtr` ascending (candidates closest to the 40.5 cap rank higher) before strategy-specific secondary sorting is applied

#### Scenario: 8-player team produces combinations within 5 seconds
- **WHEN** a team has exactly 8 eligible players
- **THEN** the system SHALL generate all valid combinations and return a result within 5 seconds

#### Scenario: Position assignment follows UTR order
- **WHEN** 4 pairs are selected
- **THEN** the pair with highest combined UTR SHALL be assigned D1, next D2, next D3, lowest D4

#### Scenario: Locked players always present in every subset
- **WHEN** `includePlayers` and/or `pinPlayers` are provided
- **THEN** all locked players (includePlayers ∪ pinPlayers keys) SHALL appear in every enumerated subset and therefore in every returned lineup candidate

#### Scenario: Top-20 subsets processed first, extends to top-40 on pin constraint
- **WHEN** pin constraints are active and fewer than 6 results are found from the top-20 subsets
- **THEN** the algorithm SHALL extend processing to up to top-40 subsets before returning results

#### Scenario: Top-20 pair truncation for D1/D2 (no pin)
- **WHEN** no pin constraints are specified
- **THEN** only the top-20 pairs by combined UTR SHALL be considered as candidates for the first two pair slots (D1/D2), while all valid pairs are used for D3/D4 slots

#### Scenario: All pairs used when pin constraints are present
- **WHEN** pin constraints are specified
- **THEN** all valid pairs SHALL be considered for every slot so that pinned-pair combinations are never excluded regardless of their combined UTR rank

---

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

### Requirement: Maximize total UTR toward 40.5 cap
The lineup generation algorithm SHALL prioritize 8-player subsets whose totalUtr is closest to (but not exceeding) 40.5. Subsets are sorted with cap-valid subsets (totalUtr ≤ 40.5) first and, within that group, by highest totalUtr descending. This ensures the highest-scoring valid subset is explored first.

#### Scenario: Cap-valid subsets ranked before over-cap subsets
- **WHEN** some 8-player subsets have totalUtr ≤ 40.5 and others exceed 40.5
- **THEN** all cap-valid subsets SHALL be ranked ahead of over-cap subsets in exploration order

#### Scenario: Closest-to-40.5 subset explored first
- **WHEN** multiple cap-valid subsets exist
- **THEN** the subset with the highest totalUtr (closest to 40.5) SHALL be explored first, so its lineups appear as the top candidates

#### Scenario: Closest-to-40.5 lineup ranks first
- **WHEN** multiple valid lineups are generated
- **THEN** the lineup whose totalUtr is closest to (but not exceeding) 40.5 SHALL be ranked as plan 1

---

---

### Requirement: Pair-level same-position pin
When two players are both pinned to the same position (e.g., both pinned to D4), the algorithm SHALL require that those two players form a pair at that position.

#### Scenario: Two players pinned to same position form a pair
- **WHEN** player A and player B are both pinned to D4
- **THEN** only lineups where A and B are paired together at D4 SHALL be returned

#### Scenario: More than two players pinned to same position is invalid
- **WHEN** three players are all pinned to D4
- **THEN** the system SHALL return a 400 error: "不能将超过2名球员固定到同一位置"

---

### Requirement: includePlayers constraint (must-include any position)
The generate endpoint SHALL accept an `includePlayers` field (array of player IDs) that marks players as must-appear in every lineup without restricting their position.

#### Scenario: Included player always appears in lineup
- **WHEN** player X is in `includePlayers`
- **THEN** every returned lineup SHALL contain player X in one of the 4 pairs

#### Scenario: Pinned player implicitly included
- **WHEN** player Y is in `pinPlayers` (mapped to D2)
- **THEN** player Y SHALL automatically be treated as included (present in lineup at D2)

---

### Requirement: Position pin constraint on generation request
The system SHALL accept a `pinPlayers` map in the generate lineup request that assigns specific players to specific positions (D1–D4). All returned lineup candidates MUST honor these assignments.

#### Scenario: Single player pinned to D1
- **WHEN** client posts `{ "teamId": "t1", "strategyType": "preset", "preset": "balanced", "pinPlayers": { "p1": "D1" } }`
- **THEN** every lineup in the response has player "p1" appearing in the D1 pair

#### Scenario: Multiple players pinned to different positions
- **WHEN** client posts `pinPlayers: { "p1": "D1", "p3": "D3" }`
- **THEN** every lineup in the response places "p1" in D1 and "p3" in D3

#### Scenario: Pin conflicts with exclude list → 400
- **WHEN** a player ID appears in both `pinPlayers` and `excludePlayers`
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "同一球员不能同时被固定位置和排除" }`

#### Scenario: No valid lineup satisfies position pin → 400
- **WHEN** the pinned player's UTR is incompatible with the requested position (e.g., lowest-UTR player pinned to D1 violates ordering constraint)
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "无法生成满足位置约束的排阵" }`

#### Scenario: Invalid position value → 400
- **WHEN** a `pinPlayers` value is not one of "D1", "D2", "D3", "D4"
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "位置必须为 D1/D2/D3/D4" }`

---

### Requirement: gender and UTR fields in Pair model
The `Pair` model SHALL expose `player1Gender` and `player2Gender` fields (values: "male" | "female") alongside the existing `player1Utr` and `player2Utr` fields.

#### Scenario: Gender fields populated in response
- **WHEN** a lineup is generated
- **THEN** each pair in the response SHALL include `player1Gender` and `player2Gender` matching the corresponding player's gender
