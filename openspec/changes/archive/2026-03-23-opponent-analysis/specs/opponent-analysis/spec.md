## Requirements

### Requirement: Analyze lineup candidates against an opponent lineup
The system SHALL expose `POST /api/lineups/analyze-opponent` accepting a team ID, opponent team ID, opponent lineup ID, optional strategy, and optional player constraints. The endpoint SHALL return both a UTR-comparison recommendation and an AI recommendation.

#### Scenario: Successful analysis returns two recommendations
- **WHEN** `POST /api/lineups/analyze-opponent` is called with valid teamId, opponentTeamId, and opponentLineupId
- **THEN** the system SHALL return HTTP 200 with `utrRecommendation` and `aiRecommendation` fields

#### Scenario: Team not found
- **WHEN** the `teamId` does not match any team
- **THEN** the system SHALL return HTTP 404 with `{ "code": "NOT_FOUND", "message": "队伍不存在" }`

#### Scenario: Opponent team not found
- **WHEN** the `opponentTeamId` does not match any team
- **THEN** the system SHALL return HTTP 404 with `{ "code": "NOT_FOUND", "message": "对手队伍不存在" }`

#### Scenario: Opponent lineup not found
- **WHEN** the `opponentLineupId` does not reference a lineup saved on the opponent team
- **THEN** the system SHALL return HTTP 404 with `{ "code": "NOT_FOUND", "message": "对手排阵不存在" }`

#### Scenario: Not enough players for own team
- **WHEN** the own team has fewer than 8 eligible players after applying constraints
- **THEN** the system SHALL return HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "排除球员后可用球员不足8人" }`

---

### Requirement: UTR-comparison recommendation
The system SHALL compute a UTR-comparison recommendation by calculating per-position win probabilities and an expected score, then selecting the own-team lineup with the highest expected score.

#### Scenario: Win probability thresholds applied per position
- **WHEN** computing per-position win probability for a candidate lineup
- **THEN** the system SHALL compare `ownPair.combinedUtr − opponentPair.combinedUtr` (delta) and assign:
  - delta > 1.0 → 80% win
  - delta > 0.5 → 60% win
  - delta ≥ −0.5 → 50% win
  - delta ≥ −1.0 → 40% win
  - delta < −1.0 → 20% win

#### Scenario: Expected score calculated per lineup
- **WHEN** evaluating a candidate lineup
- **THEN** expected score = Σ (position_points × win_probability) where D1=1pt, D2=2pt, D3=3pt, D4=4pt
- **AND** the lineup with the highest expected score SHALL be selected as the UTR recommendation

#### Scenario: UTR recommendation includes line analysis
- **WHEN** the response is returned
- **THEN** `utrRecommendation.lineAnalysis` SHALL be an array of 4 objects, one per position, each containing: `position`, `ownCombinedUtr`, `opponentCombinedUtr`, `delta`, `winProbability`, `label` (Chinese display string, e.g. "80% 赢" / "60% 赢" / "对等" / "60% 输" / "80% 输")

#### Scenario: Opponent lineup UTRs enriched from current roster
- **WHEN** the analysis runs
- **THEN** opponent pair UTRs SHALL be recalculated from the opponent team's current player roster (not stored values)

---

### Requirement: AI recommendation
The system SHALL produce an AI recommendation by calling Zhipu AI with all valid own-team lineup candidates and the opponent lineup context, and returning the AI-selected lineup with an explanation string.

#### Scenario: AI selects lineup with opponent context
- **WHEN** AI is available (API key configured and call succeeds within 3 seconds)
- **THEN** `aiRecommendation.lineup` SHALL be the AI-selected lineup and `aiRecommendation.aiUsed` SHALL be `true`

#### Scenario: AI fallback to UTR recommendation
- **WHEN** AI is unavailable (API key not configured, timeout, or error)
- **THEN** `aiRecommendation.lineup` SHALL be the same as the UTR recommendation
- **AND** `aiRecommendation.aiUsed` SHALL be `false`
- **AND** `aiRecommendation.explanation` SHALL be "AI 不可用，已用UTR分析代替"

---

### Requirement: Opponent analysis frontend page
The system SHALL provide a frontend page at `/opponent-analysis` for selecting teams, choosing an opponent lineup, and viewing both recommendation modes.

#### Scenario: Own team and opponent team selectors
- **WHEN** user navigates to `/opponent-analysis`
- **THEN** two dropdowns are shown: one for own team, one for opponent team, populated from `GET /api/teams`

#### Scenario: Opponent lineup selector populated after opponent team selection
- **WHEN** user selects an opponent team
- **THEN** a lineup dropdown is populated with that team's saved lineups (from `GET /api/teams/{id}/lineups`)
- **AND** if the opponent team has no saved lineups, the dropdown shows "对手队伍暂无保存排阵" and the analyze button is disabled

#### Scenario: Analyze button triggers API call
- **WHEN** user clicks "分析" with own team and opponent lineup selected
- **THEN** `POST /api/lineups/analyze-opponent` is called
- **AND** a loading state is shown during the call

#### Scenario: UTR recommendation panel displayed
- **WHEN** the API returns successfully
- **THEN** the UTR recommendation lineup is shown in a LineupCard
- **AND** a line analysis table shows: position, own UTR, opponent UTR, delta, and win probability label per line
- **AND** the expected score is displayed (e.g. "预期得分: 6.2 / 10")

#### Scenario: AI recommendation panel displayed
- **WHEN** the API returns successfully
- **THEN** the AI recommendation lineup is shown in a separate LineupCard
- **AND** if `aiUsed: true`, the explanation text is shown
- **AND** if `aiUsed: false`, a warning "AI 不可用" is shown

#### Scenario: Error displayed on API failure
- **WHEN** the API call fails (4xx or 5xx)
- **THEN** an error message is shown; the result panels are hidden

#### Scenario: Nav link present in sidebar
- **WHEN** user views any page
- **THEN** a nav link "对手分析" is shown in the sidebar pointing to `/opponent-analysis`
