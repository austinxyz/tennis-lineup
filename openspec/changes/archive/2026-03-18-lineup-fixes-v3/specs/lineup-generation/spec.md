## MODIFIED Requirements

### Requirement: Maximize total UTR toward 40.5 cap
The lineup generation algorithm SHALL select 8 players from the eligible roster that maximize total UTR (sum of all 8 players' individual UTRs) subject to the constraint that total UTR ≤ 40.5. When the eligible roster has more than 8 players, the algorithm SHALL explore multiple 8-player subsets to find the highest-scoring valid lineups.

#### Scenario: Roster larger than 8 — best 8 selected
- **WHEN** the eligible roster has 10 players with UTRs summing to 45.0
- **AND** the top 8 by UTR sum to 38.0 (≤ 40.5)
- **THEN** generated lineups SHALL use those 8 players (totalUtr = 38.0), not a lower-UTR combination

#### Scenario: Closest-to-40.5 lineup ranks first
- **WHEN** multiple valid lineups are generated
- **THEN** the lineup whose totalUtr is closest to (but not exceeding) 40.5 SHALL be ranked as plan 1

### Requirement: Prefer minimum female players in selection
When selecting 8 players from a larger roster, the algorithm SHALL prefer subsets containing exactly 2 female players (the minimum required) unless user constraints force additional females.

#### Scenario: Prefer 2-female subset over 3-female subset
- **WHEN** a 2-female subset and a 3-female subset both satisfy all hard constraints
- **AND** neither has materially higher totalUtr
- **THEN** the 2-female subset SHALL be preferred

#### Scenario: 3 or more females selected when forced by constraints
- **WHEN** the user pins 3 female players to specific positions
- **THEN** the algorithm SHALL include all 3 pinned females and still generate valid lineups

### Requirement: Pair-level same-position pin
When two players are both pinned to the same position (e.g., both pinned to D4), the algorithm SHALL require that those two players form a pair at that position.

#### Scenario: Two players pinned to same position form a pair
- **WHEN** player A and player B are both pinned to D4
- **THEN** only lineups where A and B are paired together at D4 SHALL be returned

#### Scenario: More than two players pinned to same position is invalid
- **WHEN** three players are all pinned to D4
- **THEN** the system SHALL return a 400 error: "不能将超过2名球员固定到同一位置"

### Requirement: includePlayers constraint (must-include any position)
The generate endpoint SHALL accept an `includePlayers` field (array of player IDs) that marks players as must-appear in every lineup without restricting their position.

#### Scenario: Included player always appears in lineup
- **WHEN** player X is in `includePlayers`
- **THEN** every returned lineup SHALL contain player X in one of the 4 pairs

#### Scenario: Pinned player implicitly included
- **WHEN** player Y is in `pinPlayers` (mapped to D2)
- **THEN** player Y SHALL automatically be treated as included (present in lineup at D2)

### Requirement: gender and UTR fields in Pair model
The `Pair` model SHALL expose `player1Gender` and `player2Gender` fields (values: "male" | "female") alongside the existing `player1Utr` and `player2Utr` fields.

#### Scenario: Gender fields populated in response
- **WHEN** a lineup is generated
- **THEN** each pair in the response SHALL include `player1Gender` and `player2Gender` matching the corresponding player's gender
