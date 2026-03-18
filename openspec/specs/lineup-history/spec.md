## Requirements

### Requirement: Manual save of a lineup
The system SHALL expose `POST /api/teams/{id}/lineups` to persist a user-selected lineup. Generated lineups are NOT auto-saved; the user must explicitly choose to save one.

#### Scenario: Lineup saved on explicit request
- **WHEN** user clicks "保留此排阵" on a lineup card
- **THEN** the frontend calls `POST /api/teams/{id}/lineups` with the current lineup (including any manual swap adjustments)
- **AND** the system persists the lineup and returns it with a unique `id` (format: `"lineup-" + nanoTime`) and `createdAt` ISO 8601 timestamp

#### Scenario: Generated candidates not auto-saved
- **WHEN** `POST /api/lineups/generate` returns successfully
- **THEN** no lineup is automatically written to `team.lineups`; persistence only happens via explicit save

---

### Requirement: Retrieve lineup history for a team
The system SHALL expose `GET /api/teams/{id}/lineups` to return all lineups for a team, ordered by `createdAt` descending (newest first). Each pair SHALL be enriched with the player's current `utr` and `gender` from the team roster.

#### Scenario: Returns lineups in reverse chronological order
- **WHEN** a team has multiple saved lineups
- **THEN** `GET /api/teams/{id}/lineups` returns them with most recent first

#### Scenario: Returns empty array for team with no lineups
- **WHEN** a team has no saved lineups
- **THEN** `GET /api/teams/{id}/lineups` returns `[]` with HTTP 200

#### Scenario: Team not found
- **WHEN** `GET /api/teams/{nonexistent-id}/lineups` is called
- **THEN** system returns HTTP 404

#### Scenario: Pairs enriched with current player UTR and gender
- **WHEN** `GET /api/teams/{id}/lineups` is called
- **THEN** each Pair in the response has `player1Utr`, `player1Gender`, `player2Utr`, `player2Gender` populated from the current team player data (backfills null fields for lineups saved before enrichment was added)

---

### Requirement: Delete a lineup
The system SHALL expose `DELETE /api/lineups/{id}` to remove a lineup from storage.

#### Scenario: Successful deletion
- **WHEN** `DELETE /api/lineups/{id}` is called with an existing lineup ID
- **THEN** system removes the lineup from the owning team's `lineups` array, returns HTTP 204 No Content

#### Scenario: Lineup not found
- **WHEN** `DELETE /api/lineups/{id}` is called with a non-existent ID
- **THEN** system returns HTTP 404 with `{ "code": "NOT_FOUND", "message": "排阵不存在" }`

---

### Requirement: Lineup history frontend page
The system SHALL provide a frontend page at `/teams/:id/lineups` listing all saved lineups for a team with delete capability and full player detail display.

#### Scenario: History page shows saved lineups with enriched detail
- **WHEN** user navigates to `/teams/:id/lineups`
- **THEN** each saved lineup is rendered as a card showing: strategy, total UTR, creation timestamp, and all 4 pairs with position, player names, individual UTRs, gender badges, and combined UTR

#### Scenario: History page empty state
- **WHEN** the team has no saved lineups
- **THEN** page displays "暂无保存的排阵"

#### Scenario: Delete from history page
- **WHEN** user clicks delete on a lineup card and confirms
- **THEN** system calls `DELETE /api/lineups/{id}` and removes the card from the list without a full page reload

#### Scenario: Delete error shown inline
- **WHEN** delete call returns an error (404 or network failure)
- **THEN** an error message is shown; the lineup list remains unchanged
