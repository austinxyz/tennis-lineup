## ADDED Requirements

### Requirement: Import players from CSV
The system SHALL allow users to import players from CSV file.

#### Scenario: Successful CSV import
- **WHEN** user uploads CSV with valid player data
- **THEN** system processes each row
- **THEN** valid players are added to team
- **THEN** system returns summary with success count

#### Scenario: CSV format validation
- **WHEN** CSV has missing "姓名" column
- **THEN** system skips invalid rows
- **THEN** system logs error "缺少必需字段: 姓名"

#### Scenario: Invalid CSV data
- **WHEN** CSV row has utr="abc" (non-numeric)
- **THEN** system skips that row
- **THEN** system logs error "UTR格式错误: abc"

#### Scenario: Partial CSV import success
- **WHEN** CSV has 10 rows, 2 invalid
- **THEN** system adds 8 valid players
- **THEN** system returns summary: "成功导入8人，失败2人"

---

### Requirement: Import players from JSON
The system SHALL allow users to import players from JSON file.

#### Scenario: Successful JSON import
- **WHEN** user uploads JSON with valid team and players data
- **THEN** system creates team if not exists
- **THEN** system adds all players to team
- **THEN** system returns success response

#### Scenario: Invalid JSON format
- **WHEN** JSON is malformed
- **THEN** system returns 400 error
- **THEN** error message is "JSON格式错误"

#### Scenario: JSON with invalid player data
- **WHEN** JSON player has gender="unknown"
- **THEN** system returns error with validation details

---

### Requirement: Import validation and error reporting
The system SHALL validate all imported data and report errors.

#### Scenario: All invalid data
- **WHEN** all rows in CSV are invalid
- **THEN** system returns 400 error
- **THEN** error message is "没有有效的数据可导入"
- **THEN** no players are added to team

#### Scenario: Duplicate player names
- **WHEN** CSV contains duplicate names
- **THEN** system creates players with unique IDs
- **THEN** duplicate names are allowed (same name different players)

---

### Requirement: CSV format specification
The system SHALL accept CSV with specific column structure.

#### Scenario: Valid CSV columns
- **WHEN** CSV contains columns: 姓名, 性别, UTR, Verified
- **THEN** system successfully processes the file

#### Scenario: UTF-8 encoding support
- **WHEN** CSV contains Chinese characters
- **THEN** system correctly parses UTF-8 encoded file

#### Scenario: Extra columns ignored
- **WHEN** CSV contains additional columns not in specification
- **THEN** system ignores extra columns
- **THEN** only specified columns are processed
