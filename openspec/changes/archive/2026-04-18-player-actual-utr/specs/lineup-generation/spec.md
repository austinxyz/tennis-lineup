## MODIFIED Requirements

### Requirement: Lineup combination algorithm
The system SHALL use a two-level algorithm to generate lineup candidates:

**Level 1 — 8-player subset enumeration:** All C(n, 8) subsets of the eligible roster are enumerated. Locked players (includePlayers ∪ pinPlayers keys) are always present in every subset. Subsets are sorted by totalUtr proximity to the 40.5 cap: cap-valid subsets (totalUtr ≤ 40.5) first, then by highest totalUtr descending. The top-20 subsets are processed first; if fewer than 6 results satisfy pin constraints after the top-20, the algorithm extends to top-40 subsets.

**Level 2 — Pair-level backtracking within each subset:** All valid pairs (partner UTR gap ≤ 3.5) are generated and sorted by combined UTR descending. Without pin constraints, only the top-20 pairs are considered for the first two pair slots (which become D1/D2 after UTR-based position assignment); all pairs are used for the remaining slots. With pin constraints, all pairs are used for every slot to ensure pinned-pair combinations are never missed. Positions D1–D4 are assigned in descending combined UTR order after all 4 pairs are selected.

**Candidate pool expansion and actualUtr re-ranking:** After constraint validation, the algorithm collects up to 100 valid lineup candidates (instead of stopping at 6). All candidates are then sorted by `actualUtrSum` (sum of each player's effective actualUtr — `actualUtr ?? utr` — across all 8 players) descending. The top 6 from this re-ranked list are returned. Hard constraints (total UTR cap, ordering, gap, gender, verified) continue to use official `utr` values throughout.

#### Scenario: Primary sort: candidates closest to 40.5 ranked first
- **WHEN** multiple valid candidates exist
- **THEN** candidates SHALL be sorted by `40.5 - totalUtr` ascending (candidates closest to the 40.5 cap rank higher) before actualUtr re-ranking is applied

#### Scenario: actualUtrSum secondary ranking applied after candidate pool collection
- **WHEN** more than 6 valid candidates are found (up to 100)
- **THEN** candidates SHALL be re-ranked by `actualUtrSum` descending before the top 6 are selected
- **THEN** a lineup with lower official `totalUtr` but higher `actualUtrSum` MAY rank above one with higher `totalUtr`

#### Scenario: actualUtrSum uses effective actualUtr fallback
- **WHEN** a player has `actualUtr: null`
- **THEN** `utr` is used as the effective actualUtr for `actualUtrSum` calculation

#### Scenario: No actualUtr set for any player — ranking unchanged
- **WHEN** all players have `actualUtr: null`
- **THEN** `actualUtrSum` equals `totalUtr` for every candidate
- **THEN** final ranking is equivalent to the pre-change proximity-to-40.5 ranking (no observable change for users without actualUtr data)

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
