## ADDED Requirements

### Requirement: Exclude players from lineup generation
The system SHALL accept an `excludePlayers` list of player IDs in the generate request. Excluded players SHALL NOT appear in any generated lineup.

#### Scenario: Excluded player absent from all results
- **WHEN** client posts `{ "excludePlayers": ["player-abc"] }` and player-abc is in the team
- **THEN** player-abc does not appear in any pair across all returned lineups

#### Scenario: Excluding too many players makes generation impossible
- **WHEN** `excludePlayers` contains enough players that fewer than 8 remain eligible
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "排除球员后可用球员不足8人" }`

#### Scenario: Empty excludePlayers behaves as no constraint
- **WHEN** `excludePlayers` is `[]` or absent
- **THEN** all team players are eligible for lineup generation

---

### Requirement: Pin players as must-play in lineup generation
The system SHALL accept an `includePlayers` list of player IDs in the generate request. All listed players SHALL appear in every generated lineup.

#### Scenario: Pinned players appear in all results
- **WHEN** client posts `{ "includePlayers": ["player-x", "player-y"] }` with valid players
- **THEN** both player-x and player-y appear in every lineup in the returned array

#### Scenario: Too many pinned players
- **WHEN** `includePlayers` contains more than 8 player IDs
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "必须上场球员超过8人" }`

#### Scenario: Pinned player is also excluded
- **WHEN** the same player ID appears in both `includePlayers` and `excludePlayers`
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "同一球员不能同时被包含和排除" }`

#### Scenario: Empty includePlayers behaves as no constraint
- **WHEN** `includePlayers` is `[]` or absent
- **THEN** lineup generation proceeds without pinning any players

---

### Requirement: Frontend player constraint selector
The lineup generator page SHALL show a player list in the left column where each player can be toggled to one of three states: neutral (default), 必须上场 (pinned), or 排除 (excluded).

#### Scenario: All players initially in neutral state
- **WHEN** a team is selected and the constraint selector renders
- **THEN** all players show as neutral (no pin or exclude badge)

#### Scenario: Clicking a player cycles through states
- **WHEN** user clicks a neutral player
- **THEN** the player becomes 必须上场 (green indicator)
- **WHEN** user clicks the same player again
- **THEN** the player becomes 排除 (red indicator)
- **WHEN** user clicks the same player again
- **THEN** the player returns to neutral

#### Scenario: Constraint selector disabled until team is selected
- **WHEN** no team is selected
- **THEN** the player constraint selector shows a placeholder "请先选择队伍"

#### Scenario: Constraint counts shown in summary
- **WHEN** N players are pinned and M players are excluded
- **THEN** the selector shows "必须上场: N 人 / 排除: M 人" summary line above the player list
