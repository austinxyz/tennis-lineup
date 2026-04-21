## MODIFIED Requirements

### Requirement: Delete team
The system SHALL allow users to delete a team ONLY when the team is empty — that is, the team's `players` array is empty AND its `lineups` array is empty. When either is non-empty, the system SHALL reject the deletion with HTTP 409 and an error payload identifying the reason.

#### Scenario: Successful team deletion of empty team
- **WHEN** user confirms deletion of team "team-001" where `players.length === 0` AND `lineups.length === 0`
- **THEN** system removes team from storage
- **THEN** system returns HTTP 204 No Content

#### Scenario: Delete non-existent team
- **WHEN** user tries to delete non-existent team
- **THEN** system returns 404 error with code "NOT_FOUND"
- **THEN** error message is "队伍不存在"

#### Scenario: Reject deletion of team with players
- **WHEN** user tries to delete a team whose `players` array contains at least one player
- **THEN** system returns HTTP 409 Conflict
- **THEN** response body is `{"code": "TEAM_NOT_EMPTY", "message": "队伍中还有球员或已保存的排阵，无法删除", "details": {"playerCount": N, "lineupCount": M}}` where N and M are the actual counts
- **THEN** the team is NOT removed from storage

#### Scenario: Reject deletion of team with saved lineups
- **WHEN** user tries to delete a team whose `players` array is empty but whose `lineups` array contains at least one saved lineup
- **THEN** system returns HTTP 409 Conflict with code "TEAM_NOT_EMPTY"
- **THEN** `details.playerCount` is 0 and `details.lineupCount` is at least 1
- **THEN** the team is NOT removed from storage

#### Scenario: Reject deletion when both players and lineups are non-empty
- **WHEN** user tries to delete a team that has both players and saved lineups
- **THEN** system returns HTTP 409 Conflict with code "TEAM_NOT_EMPTY"
- **THEN** `details.playerCount` and `details.lineupCount` both reflect actual counts greater than 0

#### Scenario: Delete button disabled in UI for non-empty team
- **WHEN** the user views the team list and a team shows `players.length > 0` OR `lineups.length > 0`
- **THEN** the team's delete button SHALL be rendered in a disabled state
- **THEN** hovering or long-pressing the disabled button SHALL reveal a message informing the user they must first remove all players and saved lineups before the team can be deleted

#### Scenario: Delete button enabled in UI for empty team
- **WHEN** the user views the team list and a team has both `players.length === 0` AND `lineups.length === 0`
- **THEN** the team's delete button SHALL be enabled and clickable

#### Scenario: Frontend handles 409 response gracefully
- **WHEN** the frontend sends `DELETE /api/teams/{id}` and receives HTTP 409 with code "TEAM_NOT_EMPTY"
- **THEN** the frontend SHALL display the server-provided message to the user
- **THEN** the team SHALL remain visible in the team list (not optimistically removed)
