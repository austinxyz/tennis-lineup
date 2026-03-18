## MODIFIED Requirements

### Requirement: Gender badge in lineup result cards
Each player name displayed in a lineup result card (LineupCard) SHALL be accompanied by a gender badge ("M" or "F") using the `player1Gender`/`player2Gender` fields from the Pair model.

#### Scenario: Male player shows M badge in card
- **WHEN** `pair.player1Gender === 'male'`
- **THEN** the player1 entry SHALL display an "M" badge in a blue color scheme

#### Scenario: Female player shows F badge in card
- **WHEN** `pair.player2Gender === 'female'`
- **THEN** the player2 entry SHALL display an "F" badge in a pink/red color scheme

#### Scenario: Gender badge visible alongside UTR
- **WHEN** `showPlayerUtr` is true
- **THEN** the display format SHALL be: `[M/F badge] Name (UTR)`
