## Requirements

### Requirement: Saved lineup matchup API
The system SHALL expose `POST /api/lineups/matchup` accepting `teamId`, `opponentTeamId`, and `opponentLineupId`. The endpoint SHALL evaluate all saved lineups from the own team against the opponent lineup using the UTR win probability algorithm, and return all results sorted by expected score descending.

#### Scenario: Returns all own saved lineups ranked by expected score
- **WHEN** `POST /api/lineups/matchup` is called with valid IDs
- **THEN** the system SHALL return HTTP 200 with a `results` array
- **AND** each result SHALL contain `lineup`, `lineAnalysis`, `expectedScore`, `opponentExpectedScore`, and `verdict`
- **AND** results SHALL be ordered by `expectedScore` descending (highest first)

#### Scenario: Own team has no saved lineups
- **WHEN** `POST /api/lineups/matchup` is called for an own team with no saved lineups
- **THEN** the system SHALL return HTTP 200 with `results: []`

#### Scenario: Team not found
- **WHEN** `teamId` does not match any team
- **THEN** the system SHALL return HTTP 404 with `{ "code": "NOT_FOUND", "message": "队伍不存在" }`

#### Scenario: Opponent team not found
- **WHEN** `opponentTeamId` does not match any team
- **THEN** the system SHALL return HTTP 404 with `{ "code": "NOT_FOUND", "message": "对手队伍不存在" }`

#### Scenario: Opponent lineup not found
- **WHEN** `opponentLineupId` does not reference a saved lineup on the opponent team
- **THEN** the system SHALL return HTTP 404 with `{ "code": "NOT_FOUND", "message": "对手排阵不存在" }`

---

### Requirement: Matchup result data model
Each result in the matchup response SHALL contain: `lineup` (own saved Lineup), `lineAnalysis` (List<LineAnalysis> — one entry per position), `expectedScore` (double, rounded to 1 decimal), `opponentExpectedScore` (double, rounded to 1 decimal), and `verdict` (String).

#### Scenario: Verdict assigned by expected score
- **WHEN** `expectedScore > 6` (out of 10 total points)
- **THEN** `verdict` SHALL be "能赢"

#### Scenario: Verdict for close matchup
- **WHEN** `expectedScore` is between 4 and 6 inclusive
- **THEN** `verdict` SHALL be "势均力敌"

#### Scenario: Verdict for losing matchup
- **WHEN** `expectedScore < 4`
- **THEN** `verdict` SHALL be "劣势"

#### Scenario: Own saved lineup UTRs enriched from current roster
- **WHEN** the matchup runs
- **THEN** own saved lineup pair UTRs SHALL be recalculated from the own team's current player roster
- **AND** opponent saved lineup pair UTRs SHALL be recalculated from the opponent team's current player roster

---

### Requirement: Saved lineup comparison mode on opponent analysis page
The opponent analysis page at `/opponent-analysis` SHALL display a mode toggle with two tabs: **排阵生成** (existing generate mode) and **已保存对比** (saved lineup comparison mode).

#### Scenario: Mode toggle switches between tabs
- **WHEN** user clicks "已保存对比" tab
- **THEN** the saved lineup comparison controls and results area are shown
- **AND** the generate-mode results and controls are hidden

#### Scenario: Compare button triggers matchup API
- **WHEN** user has selected own team + opponent team + opponent lineup and clicks "对比"
- **THEN** `POST /api/lineups/matchup` is called
- **AND** loading state is shown during the call

#### Scenario: Own team has no saved lineups
- **WHEN** the own team has no saved lineups
- **THEN** the page SHALL show "己方队伍暂无保存排阵，请先保存排阵" and disable the "对比" button

#### Scenario: Matchup results displayed ranked
- **WHEN** results are returned
- **THEN** each own saved lineup is shown as a card containing: verdict badge, expected score, LineupCard with pairs, and per-line analysis table
- **AND** results are displayed in order of expected score (highest first)

#### Scenario: Verdict badge color coding
- **WHEN** `verdict` is "能赢"
- **THEN** badge is shown in green
- **WHEN** `verdict` is "势均力敌"
- **THEN** badge is shown in yellow/orange
- **WHEN** `verdict` is "劣势"
- **THEN** badge is shown in red

#### Scenario: Error displayed on API failure
- **WHEN** the matchup API call fails
- **THEN** an error message is shown and the results area is cleared
