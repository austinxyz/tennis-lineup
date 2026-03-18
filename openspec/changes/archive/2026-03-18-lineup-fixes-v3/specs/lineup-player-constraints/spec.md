## MODIFIED Requirements

### Requirement: Player constraint selector uses dropdown
The `PlayerConstraintSelector` component SHALL use a `<select>` dropdown (not a cycle-toggle button) per player. The emit payload SHALL include three fields: `pinPlayers` (Record<id, position>), `includePlayers` (string[]), `excludePlayers` (string[]).

#### Scenario: Emit payload structure with all constraint types
- **WHEN** player A is set to 一定上, player B to D1, player C to 不上
- **THEN** emitted constraints SHALL be:
  - `includePlayers`: [A.id, B.id]
  - `pinPlayers`: { B.id: "D1" }
  - `excludePlayers`: [C.id]

#### Scenario: 中立 players excluded from all constraint lists
- **WHEN** a player's dropdown is set to 中立
- **THEN** that player's ID SHALL NOT appear in any constraint list

### Requirement: Constraint summary row
The component SHALL show a summary row with counts: "固定位置: N 人 / 一定上场: M 人 / 排除: P 人".

#### Scenario: Summary updates when constraint changes
- **WHEN** a player's dropdown changes from 中立 to D1
- **THEN** the summary SHALL increment the pinned count by 1
