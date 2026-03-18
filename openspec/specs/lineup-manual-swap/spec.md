## Requirements

### Requirement: Manual player swap between positions
The system SHALL allow users to swap one player from one position with one player from a different position in a displayed lineup. The swap SHALL be client-side only (no persistence until user explicitly saves).

#### Scenario: Valid swap between D1 and D2 players
- **WHEN** user selects player A from D1 and player B from D2 and clicks "互换"
- **THEN** the lineup updates in place with A in D2 and B in D1; combined UTRs are recalculated

#### Scenario: Swap immediately reflected in lineup card
- **WHEN** user executes a swap in the swap panel
- **THEN** the lineup card above the swap panel immediately shows the updated pair assignments, including updated names, UTRs, and gender badges

#### Scenario: Gender badge follows the player on swap
- **WHEN** a female player is swapped from D1 slot 1 to D3 slot 2
- **THEN** the "F" badge appears at D3 slot 2 in the updated lineup card; the "M" badge moves to D1 slot 1 with the swapped player

#### Scenario: Save after swap persists the adjusted lineup
- **WHEN** user swaps one or more players and then clicks "保留此排阵"
- **THEN** the saved lineup reflects the post-swap pair assignments, not the original generated arrangement

#### Scenario: Reset to original lineup
- **WHEN** user clicks "重置" after one or more swaps
- **THEN** the lineup reverts to the originally generated arrangement

#### Scenario: Swap panel shows only current lineup's players
- **WHEN** user selects a lineup card and opens the swap panel
- **THEN** the swap panel lists only the 8 players in that specific lineup, grouped by their current position

#### Scenario: Same-position swap is rejected
- **WHEN** user selects two players from the same position
- **THEN** swap button is disabled with tooltip "请选择不同位置的球员"

---

### Requirement: Auto-sort after swap instead of reject
After a manual player swap in `LineupSwapPanel`, the component SHALL NOT reject the swap if D1≥D2≥D3≥D4 ordering is violated. Instead, the component SHALL re-sort all four pairs by `combinedUtr` descending and reassign positions D1 (highest) through D4 (lowest).

#### Scenario: Swap that inverts ordering triggers auto-sort
- **WHEN** user swaps a player from D1 with a player from D4
- **AND** the resulting D1 pair has lower combinedUtr than the resulting D4 pair
- **THEN** the component SHALL sort all four pairs by combinedUtr descending
- **AND** reassign positions: highest combinedUtr → D1, next → D2, next → D3, lowest → D4
- **AND** emit `update:lineup` with the re-ordered pairs
- **AND** display no error message

#### Scenario: Swap that preserves ordering proceeds normally
- **WHEN** user swaps two players such that all D1≥D2≥D3≥D4 inequalities still hold after swap
- **THEN** the positions SHALL remain unchanged (no re-sort needed)
- **AND** emit `update:lineup` with the swapped (but unchanged-position) pairs

#### Scenario: No error message shown after any valid swap
- **WHEN** any swap is executed (whether or not ordering is preserved)
- **THEN** the component SHALL NOT display a UTR ordering violation error
