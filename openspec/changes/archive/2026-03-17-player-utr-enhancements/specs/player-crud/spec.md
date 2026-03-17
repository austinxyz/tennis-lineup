## MODIFIED Requirements

### Requirement: Add player to team
The system SHALL allow users to add a player to a team. The player data model SHALL include an optional `profileUrl` field for linking to the player's UTR profile page. UTR values SHALL be stored and displayed with two decimal places precision.

#### Scenario: Successful player addition
- **WHEN** user submits player data: name="张三", gender="male", utr=5.23, verified=true, profileUrl="https://app.utrsports.net/profiles/12345"
- **THEN** system adds player to team with unique ID
- **THEN** system assigns verifiedDoublesUtr value if available
- **THEN** response includes complete player object including profileUrl

#### Scenario: Add player with missing verifiedDoublesUtr
- **WHEN** user submits player without verifiedDoublesUtr
- **THEN** system stores player with verifiedDoublesUtr=null
- **THEN** player.verified reflects verified flag value

#### Scenario: Add player without profileUrl
- **WHEN** user submits player data without profileUrl field
- **THEN** system stores player with profileUrl=null
- **THEN** response includes profileUrl: null

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
The system SHALL allow users to update player details including profileUrl.

#### Scenario: Successful player update with UTR precision
- **WHEN** user updates player utr from 5.20 to 5.35
- **THEN** system updates player information storing the exact two-decimal value
- **THEN** all other fields remain unchanged

#### Scenario: Successful player update with profileUrl
- **WHEN** user updates player profileUrl to "https://app.utrsports.net/profiles/99999"
- **THEN** system stores the new profileUrl value
- **THEN** all other fields remain unchanged

#### Scenario: Clear profileUrl
- **WHEN** user submits profileUrl as null or empty string
- **THEN** system stores profileUrl as null

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
The system SHALL return all players in a team including profileUrl.

#### Scenario: Get team players
- **WHEN** user requests players of team "team-001"
- **THEN** system returns array of all players in that team
- **THEN** each player includes all fields: id, name, gender, utr, verifiedDoublesUtr, verified, profileUrl

#### Scenario: Team with no players
- **WHEN** team has no players
- **THEN** system returns empty array

---

### Requirement: Display UTR with two decimal places
The system SHALL display all UTR values with exactly two decimal places in the UI.

#### Scenario: UTR display on player list
- **WHEN** a player has utr=5.2 stored
- **THEN** the player list displays "5.20"

#### Scenario: UTR input accepts two decimal places
- **WHEN** user enters UTR in the player form
- **THEN** the input field uses step=0.01 allowing two decimal precision

---

### Requirement: Display player profile URL link
The system SHALL display a clickable link to the player's UTR profile when profileUrl is set.

#### Scenario: Player with profileUrl shows link
- **WHEN** a player has a non-null profileUrl
- **THEN** the player list row shows a clickable link (e.g., icon or "UTR主页" label)
- **THEN** clicking the link opens the URL in a new browser tab

#### Scenario: Player without profileUrl shows no link
- **WHEN** a player has profileUrl=null
- **THEN** no link is displayed in the player list row
