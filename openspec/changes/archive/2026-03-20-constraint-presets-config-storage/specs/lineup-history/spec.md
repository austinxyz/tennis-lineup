## MODIFIED Requirements

### Requirement: Retrieve lineup history for a team
The system SHALL expose `GET /api/teams/{id}/lineups` to return all lineups for a team, ordered by `createdAt` descending (newest first). Each pair SHALL be enriched with the player's current `utr` and `gender` from the team roster. Each lineup SHALL also include `currentValid` (boolean) and `currentViolations` (string[]) fields computed by re-running `ConstraintService.validateLineup` against the team's current player UTR values at response time. These fields are NOT persisted — they are computed on every GET.

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
- **THEN** each Pair in the response has `player1Utr`, `player1Gender`, `player2Utr`, `player2Gender` populated from the current team player data

#### Scenario: Valid lineup after UTR update
- **WHEN** a saved lineup still satisfies all hard constraints with current player UTRs
- **THEN** the lineup SHALL have `currentValid: true` and `currentViolations: []`

#### Scenario: Invalid lineup after UTR update
- **WHEN** a saved lineup violates one or more hard constraints with current player UTRs (e.g., total UTR now exceeds 40.5)
- **THEN** the lineup SHALL have `currentValid: false` and `currentViolations` SHALL list the specific violated constraints

#### Scenario: History page shows validity status
- **WHEN** user navigates to `/teams/:id/lineups`
- **THEN** each lineup card SHALL display a visual indicator: green badge "合法" when `currentValid: true`, red badge "已失效" when `currentValid: false`
- **AND** when `currentValid: false`, the card SHALL show the violation reasons from `currentViolations`
