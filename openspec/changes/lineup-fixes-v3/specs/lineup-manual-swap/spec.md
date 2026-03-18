## MODIFIED Requirements

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
