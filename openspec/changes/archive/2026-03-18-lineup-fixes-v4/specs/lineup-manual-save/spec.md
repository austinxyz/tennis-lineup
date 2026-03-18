## ADDED Requirements

### Requirement: Manual save lineup via API
The system SHALL expose `POST /api/teams/{teamId}/lineups` to persist a specific Lineup object chosen by the user. The endpoint SHALL accept the complete Lineup in the request body and store it in the team's `lineups` array.

#### Scenario: Successful manual save
- **WHEN** client posts a valid Lineup object to `POST /api/teams/{teamId}/lineups`
- **THEN** system stores the lineup in `team.lineups`, returns HTTP 200 with the saved Lineup (including `id` and `createdAt` if not already set)

#### Scenario: Team not found
- **WHEN** client posts to `POST /api/teams/{nonexistent-id}/lineups`
- **THEN** system returns HTTP 404 with `{ "code": "NOT_FOUND", "message": "队伍不存在" }`

---

### Requirement: Save button on each generated lineup card
The frontend SHALL display a "保留此排阵" button on each lineup card in the generation results grid. Clicking it SHALL call the manual save API for that lineup.

#### Scenario: Button visible on each candidate card
- **WHEN** lineup generation returns results
- **THEN** each lineup card in the result grid shows a "保留此排阵" button

#### Scenario: Button shows saved state after successful save
- **WHEN** user clicks "保留此排阵" and the API call succeeds
- **THEN** the button text changes to "已保留 ✓" and becomes disabled (no further clicks)

#### Scenario: Button shows error on failed save
- **WHEN** user clicks "保留此排阵" and the API call fails
- **THEN** an error message appears inline near the button; the button remains enabled for retry

#### Scenario: Gender null fallback display
- **WHEN** a Pair's `player1Gender` or `player2Gender` is null
- **THEN** the gender indicator in `LineupCard` is hidden (not shown as "男" by default)
