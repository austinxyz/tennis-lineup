## MODIFIED Requirements

### Requirement: Lineup combination algorithm
The system SHALL use a backtracking algorithm to enumerate valid 4-pair combinations from the 8-player candidate pool built by the lineup-queue-builder. The 8-player pool SHALL be constructed via three-phase greedy selection (exclude → locked → greedy-fill-to-40.5) rather than exhaustive C(n,8) subset enumeration. Before applying strategy-specific sorting, all candidates SHALL be primarily ranked by proximity to the 40.5 total UTR cap.

#### Scenario: Primary sort: candidates closest to 40.5 ranked first
- **WHEN** multiple valid candidates exist
- **THEN** candidates SHALL be sorted by `40.5 - totalUtr` ascending (candidates closest to the 40.5 cap rank higher) before strategy-specific secondary sorting is applied

#### Scenario: 8-player team produces combinations within 5 seconds
- **WHEN** a team has exactly 8 eligible players
- **THEN** the system SHALL generate all valid combinations and return a result within 5 seconds

#### Scenario: Position assignment follows UTR order
- **WHEN** 4 pairs are selected
- **THEN** the pair with highest combined UTR SHALL be assigned D1, next D2, next D3, lowest D4

#### Scenario: Greedy pool used instead of subset enumeration
- **WHEN** the eligible roster has more than 8 players and no include/pin constraints are specified
- **THEN** the system SHALL build the 8-player pool via greedy selection (closeness to 40.5) rather than enumerating all C(n,8) subsets

#### Scenario: Locked players always present in generated lineup
- **WHEN** `includePlayers` and/or `pinPlayers` are provided
- **THEN** all locked players SHALL be present in the final 8-player pool and appear in every returned lineup candidate

---

### Requirement: Maximize total UTR toward 40.5 cap
The lineup generation algorithm SHALL select 8 players from the eligible roster that maximize total UTR (sum of all 8 players' individual UTRs) subject to the constraint that total UTR ≤ 40.5. When the eligible roster has more than 8 players, the algorithm SHALL use greedy closeness-to-40.5 selection to build the highest-scoring valid 8-player pool without exhaustive subset enumeration.

#### Scenario: Roster larger than 8 — best 8 selected via greedy
- **WHEN** the eligible roster has 10 players with UTRs summing to 45.0
- **AND** the greedy selection (players whose individual UTR brings the running sum closest to 40.5) results in a pool summing to ≤ 40.5
- **THEN** generated lineups SHALL use those greedily selected 8 players

#### Scenario: Closest-to-40.5 lineup ranks first
- **WHEN** multiple valid lineups are generated
- **THEN** the lineup whose totalUtr is closest to (but not exceeding) 40.5 SHALL be ranked as plan 1
