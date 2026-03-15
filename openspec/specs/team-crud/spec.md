## ADDED Requirements

### Requirement: Create team
The system SHALL allow users to create a new team with a unique name.

#### Scenario: Successful team creation
- **WHEN** user submits team name "上海飞鹰队"
- **THEN** system creates team with unique ID
- **THEN** system stores team creation timestamp
- **THEN** system initializes empty players array

#### Scenario: Duplicate team name
- **WHEN** user submits team name that already exists
- **THEN** system returns error with message "队名已存在"

#### Scenario: Invalid team name format
- **WHEN** user submits empty team name
- **THEN** system returns error with message "队名不能为空"
- **WHEN** user submits team name longer than 50 characters
- **THEN** system returns error with message "队名不能超过50个字符"

---

### Requirement: List teams
The system SHALL return all teams to the user.

#### Scenario: Get all teams
- **WHEN** user requests team list
- **THEN** system returns array of all teams
- **THEN** each team includes id, name, createdAt, and players array
- **THEN** teams are sorted by creation date (newest first)

#### Scenario: Empty team list
- **WHEN** no teams exist
- **THEN** system returns empty array

---

### Requirement: Get team details
The system SHALL return details of a specific team.

#### Scenario: Get existing team
- **WHEN** user requests team with valid ID "team-001"
- **THEN** system returns team details
- **THEN** response includes all players with their complete information

#### Scenario: Get non-existent team
- **WHEN** user requests team with ID "non-existent"
- **THEN** system returns 404 error with code "NOT_FOUND"
- **THEN** error message is "队伍不存在"

---

### Requirement: Update team name
The system SHALL allow users to update team name.

#### Scenario: Successful name update
- **WHEN** user submits new name "上海雄鹰队" for team "team-001"
- **THEN** system updates team name
- **THEN** all other team data remains unchanged

#### Scenario: Update non-existent team
- **WHEN** user tries to update name of non-existent team
- **THEN** system returns 404 error

---

### Requirement: Delete team
The system SHALL allow users to delete a team.

#### Scenario: Successful team deletion
- **WHEN** user confirms deletion of team "team-001"
- **THEN** system removes team from storage
- **THEN** system returns success response

#### Scenario: Delete non-existent team
- **WHEN** user tries to delete non-existent team
- **THEN** system returns 404 error
