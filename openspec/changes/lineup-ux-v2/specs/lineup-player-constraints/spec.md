## MODIFIED Requirements

### Requirement: Player constraint selector display and sorting
The system SHALL display the player constraint selector list sorted by gender first (female before male), then by UTR descending within each gender group. Each player entry SHALL show a verified indicator.

#### Scenario: Females appear before males
- **WHEN** the player constraint selector renders a mixed roster
- **THEN** all female players appear at the top of the list, all male players appear below

#### Scenario: Within each gender, higher UTR players appear first
- **WHEN** the player constraint selector renders players of the same gender
- **THEN** the player with the highest UTR in that gender group appears first; ties are broken by name alphabetically

#### Scenario: Verified players show a badge
- **WHEN** a player has `verified: true`
- **THEN** a "认证" badge (green, compact) is shown next to the player's name in the constraint list

#### Scenario: Unverified players show no badge
- **WHEN** a player has `verified: false`
- **THEN** no badge is shown; the row otherwise looks identical

#### Scenario: Sort updates when players prop changes
- **WHEN** the players prop is updated (e.g., team switched)
- **THEN** the list re-renders in the correct sorted order for the new roster

---

### Requirement: Player position pin constraint
The system SHALL allow users to set a constraint that pins a specific player to a specific position (D1–D4) for lineup generation. Pinned players MUST appear in that position in every returned lineup candidate.

#### Scenario: Pin player to position via toggle
- **WHEN** user clicks the constraint toggle for a player and cycles to a position state (D1/D2/D3/D4)
- **THEN** that player's constraint shows the position label (e.g., "D1") and the constraint summary updates to reflect the pin

#### Scenario: Toggle cycle includes position states
- **WHEN** user clicks the toggle button for a player
- **THEN** the state cycles: 中立 → D1 → D2 → D3 → D4 → 排除 → 中立

#### Scenario: Constraint summary shows pin count
- **WHEN** one or more players are pinned to positions
- **THEN** the summary row shows "固定位置: N 人" in addition to include/exclude counts

#### Scenario: update:constraints emit includes pinPlayers
- **WHEN** any constraint changes
- **THEN** component emits `update:constraints` with payload `{ includePlayers: [], excludePlayers: [], pinPlayers: { playerId: "D1" } }`
