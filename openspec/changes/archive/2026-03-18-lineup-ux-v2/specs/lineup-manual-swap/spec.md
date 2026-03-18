## ADDED Requirements

### Requirement: Manual player swap between positions
The system SHALL allow users to swap one player from one position with one player from a different position in a displayed lineup. The swap SHALL be client-side only (no persistence) and SHALL validate that the resulting arrangement satisfies all hard constraints.

#### Scenario: Valid swap between D1 and D2 players
- **WHEN** user selects player A from D1 and player B from D2 and clicks "互换"
- **THEN** the lineup updates in place with A in D2 and B in D1; combined UTRs are recalculated; if D1 combined UTR ≥ D2 combined UTR the swap is accepted

#### Scenario: Swap that violates UTR ordering is rejected
- **WHEN** the swap would result in D1 combined UTR < D2 combined UTR (or any ordering violation D1≥D2≥D3≥D4)
- **THEN** the swap is rejected with an inline warning message "互换后不满足UTR排序约束 (D1 ≥ D2 ≥ D3 ≥ D4)"

#### Scenario: Reset to original lineup
- **WHEN** user clicks "重置" after one or more swaps
- **THEN** the lineup reverts to the originally generated arrangement

#### Scenario: Swap panel shows only current lineup's players
- **WHEN** user selects a lineup card and opens the swap panel
- **THEN** the swap panel lists only the 8 players in that specific lineup, grouped by their current position

#### Scenario: Same-position swap is rejected
- **WHEN** user selects two players from the same position
- **THEN** swap button is disabled with tooltip "请选择不同位置的球员"
