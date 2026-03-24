## ADDED Requirements

### Requirement: Batch personal notes editing
The system SHALL provide a UI that displays all players in a team with their personal notes in a single editable list, and SHALL save all notes in one action.

#### Scenario: View personal notes for all players
- **WHEN** the user opens the 个人笔记 panel for a team
- **THEN** each player in the team is shown with their current notes (empty string if none) in an editable text field

#### Scenario: Edit and save personal notes
- **WHEN** the user edits one or more players' note fields and clicks the save button
- **THEN** the system sends a PATCH request to `/api/teams/{id}/players/notes` with all player note values
- **THEN** the system shows a success confirmation

#### Scenario: Save is idempotent
- **WHEN** the user clicks save without changing any notes
- **THEN** the system still succeeds and all existing notes remain unchanged

### Requirement: Personal notes persisted on backend
The system SHALL persist personal notes in the existing `notes` field of each Player record.

#### Scenario: Notes survive page reload
- **WHEN** the user saves personal notes and reloads the page
- **THEN** each player's notes field shows the previously saved value

### Requirement: Bulk notes API
The backend SHALL expose `PATCH /api/teams/{teamId}/players/notes` accepting a JSON array of `{playerId, notes}` objects.

#### Scenario: Successful bulk update
- **WHEN** a PATCH request is sent with a valid array of player-note pairs
- **THEN** the backend updates each named player's notes field and returns 200

#### Scenario: Unknown player ignored
- **WHEN** a PATCH request includes a playerId not belonging to the team
- **THEN** the backend skips that entry and still returns 200 for the valid entries
