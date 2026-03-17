## ADDED Requirements

### Requirement: Save lineup after generation
The system SHALL automatically save every successfully generated lineup to the team's `lineups` array in persistent storage.

#### Scenario: Lineup persisted after generation
- **WHEN** `POST /api/lineups/generate` returns HTTP 200
- **THEN** the returned lineup is stored in `team.lineups` in `tennis-data.json` and retrievable via history API

#### Scenario: Lineup assigned unique ID and timestamp
- **WHEN** a lineup is saved
- **THEN** it has a unique `id` (format: `"lineup-" + nanoTime`) and `createdAt` as ISO 8601 timestamp

---

### Requirement: Retrieve lineup history for a team
The system SHALL expose `GET /api/teams/{id}/lineups` to return all lineups for a team, ordered by `createdAt` descending (newest first).

#### Scenario: Returns lineups in reverse chronological order
- **WHEN** a team has multiple saved lineups
- **THEN** `GET /api/teams/{id}/lineups` returns them with most recent first

#### Scenario: Returns empty array for team with no lineups
- **WHEN** a team has no saved lineups
- **THEN** `GET /api/teams/{id}/lineups` returns `[]` with HTTP 200

#### Scenario: Team not found
- **WHEN** `GET /api/teams/{nonexistent-id}/lineups` is called
- **THEN** system returns HTTP 404

---

### Requirement: Delete a lineup
The system SHALL expose `DELETE /api/lineups/{id}` to remove a lineup from storage.

#### Scenario: Successful deletion
- **WHEN** `DELETE /api/lineups/{id}` is called with an existing lineup ID
- **THEN** system removes the lineup from the owning team's `lineups` array, returns HTTP 204 No Content

#### Scenario: Lineup not found
- **WHEN** `DELETE /api/lineups/{id}` is called with a non-existent ID
- **THEN** system returns HTTP 404 with `{ "code": "NOT_FOUND", "message": "排阵不存在" }`
