## Requirements

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

### Requirement: Player constraint selector display and sorting
The `PlayerConstraintSelector` component SHALL display players sorted by gender first (females before males), then by UTR descending within each gender group. Gender badges SHALL use 女 (pink) and 男 (blue).

#### Scenario: Females appear before males
- **WHEN** the player constraint selector renders a mixed roster
- **THEN** all female players appear at the top of the list, all male players appear below

#### Scenario: Within each gender, higher UTR players appear first
- **WHEN** the player constraint selector renders players of the same gender
- **THEN** the player with the highest UTR in that gender group appears first; ties are broken by name alphabetically

#### Scenario: Gender badge labels
- **WHEN** the player constraint selector renders a female player
- **THEN** a "女" badge (pink) is shown; for male players a "男" badge (blue) is shown

#### Scenario: Verified players show a badge
- **WHEN** a player has `verified: true`
- **THEN** a "认证" badge (green, compact) is shown next to the player's name in the constraint list

#### Scenario: Sort updates when players prop changes
- **WHEN** the players prop is updated (e.g., team switched)
- **THEN** the list re-renders in the correct sorted order for the new roster

---

### Requirement: Player constraint selector uses dropdown
The `PlayerConstraintSelector` component SHALL use a `<select>` dropdown per player. The emit payload SHALL include three fields: `pinPlayers` (Record<id, position>), `includePlayers` (string[]), `excludePlayers` (string[]). Pinned players are implicitly included.

#### Scenario: Emit payload structure with all constraint types
- **WHEN** player A is set to 一定上, player B to D1, player C to 不上
- **THEN** emitted constraints SHALL be:
  - `includePlayers`: [A.id, B.id]
  - `pinPlayers`: { B.id: "D1" }
  - `excludePlayers`: [C.id]

#### Scenario: 中立 players excluded from all constraint lists
- **WHEN** a player's dropdown is set to 中立
- **THEN** that player's ID SHALL NOT appear in any constraint list

---

### Requirement: Constraint summary row
The component SHALL show a summary row with counts: "固定位置: N 人 / 一定上场: M 人 / 排除: P 人".

#### Scenario: Summary updates when constraint changes
- **WHEN** a player's dropdown changes from 中立 to D1
- **THEN** the summary SHALL increment the pinned count by 1

---

### Requirement: Constraint selector disabled until team is selected
#### Scenario: Constraint selector disabled until team is selected
- **WHEN** no team is selected
- **THEN** the player constraint selector shows a placeholder "请先选择队伍"

---

### Requirement: External preset loading
The `PlayerConstraintSelector` component SHALL expose a `loadStates(newStates)` method (via `defineExpose`) allowing a parent component to programmatically set all player constraint states at once (e.g., when a saved preset is loaded).

#### Scenario: Parent loads preset into selector
- **WHEN** parent calls `playerConstraintRef.loadStates({ "player-a": "D1", "player-b": "exclude" })`
- **THEN** the selector SHALL reflect those states immediately and emit an updated constraints payload

---

### Requirement: Constraint presets
The system SHALL allow users to save named constraint configurations per team and reload them later. Presets are managed via `ConstraintPresetSelector` component and persisted in `tennis-config.json`.

#### Scenario: Save current constraints as preset
- **WHEN** user enters a name and clicks save in `ConstraintPresetSelector`
- **THEN** `POST /api/teams/{id}/constraint-presets` is called with `{ name, excludePlayers, includePlayers, pinPlayers }`
- **AND** the new preset appears at the top of the preset dropdown

#### Scenario: Load a saved preset
- **WHEN** user selects a preset from the dropdown and clicks 加载
- **THEN** `PlayerConstraintSelector` states are updated to match the preset's constraints

#### Scenario: Delete a preset
- **WHEN** user selects a preset and clicks 删除
- **THEN** `DELETE /api/teams/{id}/constraint-presets/{presetId}` is called and the preset is removed from the dropdown

#### Scenario: Empty preset list
- **WHEN** a team has no saved presets
- **THEN** the dropdown shows "暂无预设"

#### Scenario: Preset references deleted players
- **WHEN** a preset contains player IDs no longer on the team
- **THEN** those IDs are silently filtered out when loading
- **AND** a warning "部分球员已不在队伍中，相关约束已跳过" is displayed
