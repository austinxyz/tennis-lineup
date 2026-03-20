## ADDED Requirements

### Requirement: Build candidate pool via three-phase greedy selection
The system SHALL build the 8-player candidate pool using a three-phase layered approach instead of enumerating all C(n,8) subsets.

#### Scenario: Phase 1 — excluded players removed first
- **WHEN** a generate request includes `excludePlayers: ["p1", "p2"]`
- **THEN** p1 and p2 SHALL NOT appear in the eligible player pool before any further selection

#### Scenario: Phase 2 — locked players added first (includePlayers + pinPlayers)
- **WHEN** a generate request includes `includePlayers: ["p3"]` and `pinPlayers: { "p4": "D1" }`
- **THEN** both p3 and p4 SHALL be added to the 8-player queue before any other players

#### Scenario: Phase 3 — remaining slots filled by closeness to 40.5
- **WHEN** the locked set has fewer than 8 players
- **THEN** the remaining slots SHALL be filled from eligible non-locked players, sorted so that the player whose addition brings the running totalUtr closest to 40.5 is selected first

#### Scenario: Locked count exceeds 8 returns 400
- **WHEN** the union of `includePlayers` and `pinPlayers` keys exceeds 8 distinct players
- **THEN** the system SHALL return HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "固定参赛球员超过8人" }`

#### Scenario: Exactly 8 locked players skips phase 3
- **WHEN** `includePlayers` + `pinPlayers` together specify exactly 8 distinct players
- **THEN** the system SHALL use exactly those 8 players as the candidate pool without appending any additional players

#### Scenario: No locked players — full greedy selection from eligible
- **WHEN** neither `includePlayers` nor `pinPlayers` is provided
- **THEN** the 8-player pool SHALL be built entirely by greedy selection from eligible players, sorted by closeness to 40.5

---

### Requirement: Top20/Top40 truncation before constraint validation
Before full pair-level backtracking, the system SHALL apply top20/top40 truncation to reduce the candidate pair set.

#### Scenario: Candidate pairs sorted by combined UTR descending
- **WHEN** the 8-player pool is assembled
- **THEN** all valid pairs (satisfying partner UTR gap ≤ 3.5) SHALL be generated and sorted by combined UTR descending

#### Scenario: Top20 pairs used as D1/D2 candidates
- **WHEN** assigning pairs to positions D1 and D2
- **THEN** only the top 20 pairs (by combined UTR) SHALL be considered as candidates for those positions

#### Scenario: All valid pairs used as D3/D4 candidates
- **WHEN** assigning pairs to positions D3 and D4
- **THEN** all valid pairs (not exceeding 28 total from 8 players) SHALL be considered, effectively treating the limit as top40 which covers the full set

#### Scenario: Greedy fallback on constraint failure
- **WHEN** the initial 8-player greedy pool yields no valid lineup due to hard constraint violations (e.g., fewer than 2 females, D4 unverified)
- **THEN** the system SHALL attempt swapping the last greedily-added player with the next candidate in the sorted remaining list (one-step fallback) before declaring no valid lineup exists
