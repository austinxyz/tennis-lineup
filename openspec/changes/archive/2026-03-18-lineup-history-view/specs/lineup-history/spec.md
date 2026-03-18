## ADDED Requirements

### Requirement: Lineup history page
The system SHALL provide a frontend page at `/teams/:id/lineups` that lists all saved lineups for the selected team in reverse chronological order.

#### Scenario: History page shows saved lineups
- **WHEN** user navigates to `/teams/:id/lineups` for a team with saved lineups
- **THEN** page renders each lineup as a card showing: strategy label, total UTR, creation timestamp, and all 4 pairs with position (D1–D4), player names, individual UTRs, gender badges, and combined UTR

#### Scenario: History page empty state
- **WHEN** user navigates to `/teams/:id/lineups` for a team with no saved lineups
- **THEN** page shows placeholder message "暂无保存的排阵"

#### Scenario: Delete lineup from history
- **WHEN** user clicks the delete button on a lineup card in the history page
- **THEN** system calls `DELETE /api/lineups/{id}`, the lineup is removed from the list without a full page reload

#### Scenario: Delete non-existent lineup shows error
- **WHEN** `DELETE /api/lineups/{id}` returns 404
- **THEN** frontend shows an error message; the lineup list remains unchanged

### Requirement: Enriched player info in history lineup cards
Each pair row in the history view SHALL display individual player UTR and gender badge using the enriched fields returned by `GET /api/teams/{id}/lineups`.

#### Scenario: Gender badge shown in history card
- **WHEN** history view renders a lineup card
- **THEN** each player entry shows a gender badge ("M" in blue for male, "F" in pink for female) alongside the player name

#### Scenario: Individual UTR shown in history card
- **WHEN** history view renders a lineup card
- **THEN** each player entry shows their UTR in parentheses, e.g. "张三 (6.5)"
