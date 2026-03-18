## ADDED Requirements

### Requirement: Dropdown constraint selector with seven states
The `PlayerConstraintSelector` component SHALL replace the cycle-toggle button with a `<select>` dropdown for each player row. The dropdown SHALL offer exactly seven options: 中立, 不上, 一定上, D1, D2, D3, D4.

#### Scenario: Default state is 中立
- **WHEN** the player constraint list is rendered
- **THEN** each player's dropdown SHALL default to 中立

#### Scenario: Selecting 不上 excludes the player
- **WHEN** user selects 不上 for a player
- **THEN** the component SHALL emit `update:constraints` with that player's ID in `excludePlayers`

#### Scenario: Selecting 一定上 marks player as must-include
- **WHEN** user selects 一定上 for a player
- **THEN** the component SHALL emit `update:constraints` with that player's ID in `includePlayers`
- **AND** the player SHALL NOT appear in `pinPlayers`

#### Scenario: Selecting D1–D4 pins player to position
- **WHEN** user selects D1 (or D2/D3/D4) for a player
- **THEN** the component SHALL emit `update:constraints` with that player's ID in `pinPlayers` mapped to the selected position
- **AND** the player's ID SHALL also appear in `includePlayers` (a pinned player is implicitly must-include)

#### Scenario: Row background color reflects state
- **WHEN** dropdown value is D1/D2/D3/D4 or 一定上
- **THEN** the row background SHALL be blue-tinted
- **WHEN** dropdown value is 不上
- **THEN** the row background SHALL be red-tinted
- **WHEN** dropdown value is 中立
- **THEN** the row background SHALL be white/gray

### Requirement: Gender badge in constraint list
Each player row in `PlayerConstraintSelector` SHALL display a gender badge showing "M" (male) or "F" (female) adjacent to the player name.

#### Scenario: Male player shows M badge
- **WHEN** a player has `gender === 'male'`
- **THEN** the row SHALL display a badge with text "M" in a blue color scheme

#### Scenario: Female player shows F badge
- **WHEN** a player has `gender === 'female'`
- **THEN** the row SHALL display a badge with text "F" in a pink/red color scheme
