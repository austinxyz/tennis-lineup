## MODIFIED Requirements

### Requirement: Display multiple lineup candidates simultaneously
The system SHALL display all returned lineup candidates at once in a grid layout without tab navigation. Each lineup card SHALL show all pair details including individual player UTRs.

#### Scenario: All candidates visible without interaction
- **WHEN** generation returns 6 lineups
- **THEN** all 6 lineup cards are visible on screen simultaneously in a 2-column grid (desktop); no tab clicking required

#### Scenario: Per-player UTR shown in each pair row
- **WHEN** a lineup card is displayed
- **THEN** each pair row shows: position label (D1–D4), player1Name (player1Utr) / player2Name (player2Utr), combined UTR

#### Scenario: Lineup cards labeled with plan number
- **WHEN** multiple lineup cards are shown
- **THEN** each card has a header showing "方案 1", "方案 2", etc. in order

#### Scenario: First lineup card highlighted as best
- **WHEN** multiple lineup cards are displayed
- **THEN** "方案 1" card has a distinct visual treatment (e.g., green border or badge) indicating it is the best/persisted candidate

#### Scenario: Single lineup returned
- **WHEN** generation returns exactly 1 lineup
- **THEN** that lineup card is displayed normally without empty placeholders for missing slots

#### Scenario: No lineups yet
- **WHEN** no generation has been triggered
- **THEN** the results area shows an empty-state placeholder message
