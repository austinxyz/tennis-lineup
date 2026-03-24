## Requirements

### Requirement: Saved lineup matchup API
The system SHALL expose `POST /api/lineups/matchup` accepting `teamId`, `opponentTeamId`, `opponentLineupId`, and two new optional fields: `ownLineupId` (string) and `includeAi` (boolean, default false). The endpoint SHALL evaluate saved lineups from the own team against the opponent lineup using the UTR win probability algorithm, and return all results sorted by expected score descending.

#### Scenario: ownLineupId filters to single own lineup
- **WHEN** `ownLineupId` is provided
- **THEN** only that saved lineup is evaluated and the response contains exactly one result (or empty if not found)

#### Scenario: includeAi triggers AI recommendation
- **WHEN** `includeAi: true` and no `ownLineupId`
- **THEN** AI evaluates top 5 own saved lineups by expected score and picks the best
- **AND** the response includes an `aiRecommendation` field alongside `results`

#### Scenario: AI recommendation includes reasoning
- **WHEN** AI returns a result
- **THEN** `aiRecommendation.explanation` SHALL contain a brief reasoning string in Chinese (not just a fixed "AI 根据对手排阵选择最优方案")
- **AND** `aiRecommendation.lineAnalysis` and `aiRecommendation.expectedScore` SHALL be populated for the AI-selected lineup

#### Scenario: includeAi with ownLineupId is ignored
- **WHEN** both `ownLineupId` and `includeAi: true` are provided
- **THEN** `includeAi` is ignored; only the specified lineup is evaluated; no AI recommendation is returned

#### Scenario: Results still sorted by expected score descending
- **WHEN** no `ownLineupId` is provided (Mode A)
- **THEN** results are sorted by `expectedScore` descending regardless of `includeAi`

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

### Requirement: Matchup API supports partner notes context for AI
`POST /api/lineups/matchup` SHALL accept two new optional fields: `ownPartnerNotes` (array) and `opponentPartnerNotes` (array). Each entry is `{player1Name, player2Name, note}`.

#### Scenario: Partner notes forwarded to AI prompt
- **WHEN** `includeAi: true` and `ownPartnerNotes` or `opponentPartnerNotes` are non-empty
- **THEN** the AI prompt includes a "搭档笔记" section with own-team and opponent notes formatted as `[player1 + player2]: note`

#### Scenario: Request without partner notes still works
- **WHEN** `ownPartnerNotes` and `opponentPartnerNotes` are absent or empty arrays
- **THEN** the API behaves exactly as before — no partner notes section in the prompt
