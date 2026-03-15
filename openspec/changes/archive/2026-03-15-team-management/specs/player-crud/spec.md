## ADDED Requirements

### Requirement: Add player to team
The system SHALL allow users to add a player to a team.

#### Scenario: Successful player addition
- **WHEN** user submits player data: name="张三", gender="male", utr=5.2, verified=true
- **THEN** system adds player to team with unique ID
- **THEN** system assigns verifiedDoublesUtr value if available
- **THEN** response includes complete player object

#### Scenario: Add player with missing verifiedDoublesUtr
- **WHEN** user submits player without verifiedDoublesUtr
- **THEN** system stores player with verifiedDoublesUtr=null
- **THEN** player.verified reflects verified flag value

#### Scenario: Add player to non-existent team
- **WHEN** user tries to add player to non-existent team
- **THEN** system returns 404 error

#### Scenario: Invalid gender value
- **WHEN** user submits gender="unknown"
- **THEN** system returns error with message "性别必须是male或female"

#### Scenario: Invalid UTR range
- **WHEN** user submits utr=17.0
- **THEN** system returns error with message "UTR必须在0.0到16.0之间"
- **WHEN** user submits utr=-1.0
- **THEN** system returns error with message "UTR必须在0.0到16.0之间"

#### Scenario: Missing required fields
- **WHEN** user submits player without name
- **THEN** system returns error with message "姓名不能为空"

---

### Requirement: Update player information
The system SHALL allow users to update player details.

#### Scenario: Successful player update
- **WHEN** user updates player utr from 5.2 to 5.5
- **THEN** system updates player information
- **THEN** all other fields remain unchanged

#### Scenario: Update non-existent player
- **WHEN** user tries to update non-existent player
- **THEN** system returns 404 error

---

### Requirement: Delete player from team
The system SHALL allow users to delete a player from a team.

#### Scenario: Successful player deletion
- **WHEN** user confirms deletion of player "player-001"
- **THEN** system removes player from team
- **THEN** system returns success response

#### Scenario: Delete player used in lineup
- **WHEN** user tries to delete player that is referenced in existing lineup
- **THEN** system returns error with message "该球员在排阵中使用，无法删除"

#### Scenario: Delete non-existent player
- **WHEN** user tries to delete non-existent player
- **THEN** system returns 404 error

---

### Requirement: List team players
The system SHALL return all players in a team.

#### Scenario: Get team players
- **WHEN** user requests players of team "team-001"
- **THEN** system returns array of all players in that team
- **THEN** each player includes all fields: id, name, gender, utr, verifiedDoublesUtr, verified

#### Scenario: Team with no players
- **WHEN** team has no players
- **THEN** system returns empty array
