## ADDED Requirements

### Requirement: Partner note data model
The system SHALL store partner notes as records with the fields: `id` (string), `teamId` (string), `player1Id` (string), `player2Id` (string), `note` (string), `createdAt` (ISO timestamp), `updatedAt` (ISO timestamp).

#### Scenario: Partner note uniqueness per pair
- **WHEN** a partner note is created for players A and B
- **THEN** the backend normalizes the pair so that (A,B) and (B,A) refer to the same record

### Requirement: Partner notes CRUD API
The backend SHALL expose the following endpoints:
- `GET /api/teams/{teamId}/partner-notes` — returns all partner notes for the team
- `POST /api/teams/{teamId}/partner-notes` — creates or updates a note for a player pair
- `PUT /api/teams/{teamId}/partner-notes/{noteId}` — updates the note text for an existing record
- `DELETE /api/teams/{teamId}/partner-notes/{noteId}` — deletes a partner note

#### Scenario: List partner notes
- **WHEN** GET `/api/teams/{teamId}/partner-notes` is called
- **THEN** the response is a JSON array of all partner note records for that team

#### Scenario: Create partner note
- **WHEN** POST `/api/teams/{teamId}/partner-notes` is called with `{player1Id, player2Id, note}`
- **THEN** the backend creates a new record (or updates the existing record for that pair) and returns 200 with the saved record

#### Scenario: Update partner note
- **WHEN** PUT `/api/teams/{teamId}/partner-notes/{noteId}` is called with `{note}`
- **THEN** the backend updates only the note text and `updatedAt`, returns 200 with the updated record

#### Scenario: Delete partner note
- **WHEN** DELETE `/api/teams/{teamId}/partner-notes/{noteId}` is called
- **THEN** the backend removes the record and returns 204

### Requirement: Partner notes UI — add/edit
The system SHALL provide a UI panel where the user can select two players from the team roster and write notes about their partnership.

#### Scenario: Select a player pair and add a note
- **WHEN** the user opens the 搭档笔记 panel, selects player A from the first dropdown and player B from the second dropdown, and types a note
- **THEN** the same player SHALL NOT appear in both dropdowns simultaneously (player A is excluded from the player B dropdown)

#### Scenario: Save partner note
- **WHEN** the user fills in both players and writes a note and clicks save
- **THEN** the system POSTs to `/api/teams/{teamId}/partner-notes` and the note appears in the list

#### Scenario: Edit existing partner note
- **WHEN** the user clicks edit on a displayed partner note
- **THEN** the note text becomes editable in place and a save button appears

#### Scenario: Delete partner note
- **WHEN** the user clicks delete on a partner note and confirms
- **THEN** the note is removed from the list and deleted on the backend

### Requirement: Partner notes for opponent teams
The partner notes system SHALL be available for any team, including opponent teams. Users can record scouting observations about opponent player pairings.

#### Scenario: Add scouting note for opponent pair
- **WHEN** the user opens the 搭档笔记 panel for an opponent team (e.g., from the opponent analysis page)
- **THEN** the system loads that opponent team's roster for player selection
- **AND** the user can add/edit/delete partner notes for the opponent team using the same UI

#### Scenario: Opponent partner notes stored under opponent team ID
- **WHEN** a partner note is saved for an opponent team
- **THEN** it is stored under `data/teams/{opponentTeamId}/partner-notes.json`
- **AND** it is returned by `GET /api/teams/{opponentTeamId}/partner-notes`

### Requirement: Partner notes fed into AI lineup advisor
When the AI lineup advisor is invoked from the opponent analysis page, own-team and opponent partner notes SHALL be included in the AI prompt to improve recommendation quality.

#### Scenario: Partner notes included in AI matchup request
- **WHEN** the user triggers the AI recommendation (最佳三阵 AI button or 逐线对比 AI button)
- **THEN** the frontend fetches own-team partner notes and opponent-team partner notes
- **AND** includes them as `ownPartnerNotes` and `opponentPartnerNotes` arrays in the `POST /api/lineups/matchup` request body
- **AND** each entry contains `player1Name`, `player2Name`, and `note`

#### Scenario: AI prompt contains partner notes context
- **WHEN** the matchup API receives non-empty `ownPartnerNotes` or `opponentPartnerNotes`
- **THEN** the AI prompt includes a "搭档笔记" section formatted as `[player1 + player2]: note` lines
- **AND** own-team notes and opponent notes appear in separate labeled sub-sections

#### Scenario: Partner notes capped at 10 per team
- **WHEN** a team has more than 10 partner notes
- **THEN** the frontend sends only the 10 most recently updated notes in the request
- **AND** no error is raised for the remaining notes

#### Scenario: AI request still works with no partner notes
- **WHEN** no partner notes exist for either team
- **THEN** `ownPartnerNotes` and `opponentPartnerNotes` are omitted or empty arrays
- **AND** the AI call proceeds normally without a partner notes section in the prompt

### Requirement: Partner notes persisted in JSON storage
The backend SHALL store partner notes in `data/teams/{teamId}/partner-notes.json` as a JSON array.

#### Scenario: Notes survive server restart
- **WHEN** partner notes are saved and the server is restarted
- **THEN** the notes are still returned by the GET endpoint
