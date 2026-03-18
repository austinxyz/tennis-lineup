## REMOVED Requirements

### Requirement: Save lineup after generation
**Reason**: Auto-save replaced by user-initiated manual save. Users want to review all generated candidates before deciding which to keep.
**Migration**: Frontend replaces implicit auto-save with explicit "保留" button per lineup card; backend removes persistence call from generation flow.

---

## ADDED Requirements

### Requirement: Enrich historical lineup pairs with player data
The system SHALL enrich each Pair in returned lineup history with current player data for fields missing from older stored records (`player1Utr`, `player2Utr`, `player1Gender`, `player2Gender`).

#### Scenario: Old lineups missing UTR fields are enriched
- **WHEN** `GET /api/teams/{id}/lineups` is called and a stored Pair has `player1Utr: null`
- **THEN** the system looks up the player by `player1Id` in `team.players` and sets `player1Utr` from the player's current UTR value before returning

#### Scenario: Old lineups missing gender fields show correct gender
- **WHEN** `GET /api/teams/{id}/lineups` is called and a stored Pair has `player1Gender: null`
- **THEN** the system looks up the player by `player1Id` in `team.players` and sets `player1Gender` from the player's current gender value before returning

#### Scenario: Player deleted — fields remain null
- **WHEN** a stored Pair references a player ID that no longer exists in `team.players`
- **THEN** the fields remain null; the enrichment is skipped for that player

#### Scenario: Already-enriched fields are not overwritten
- **WHEN** a stored Pair already has non-null `player1Utr` and `player1Gender`
- **THEN** those values are returned as-is without overwriting from current player data
