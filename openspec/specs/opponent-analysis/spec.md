## MODIFIED Requirements

### Requirement: Opponent analysis frontend page
The system SHALL provide a frontend page at `/opponent-analysis` with a **mode toggle** showing two tabs: **排阵生成** (generate candidates and recommend) and **已保存对比** (compare own saved lineups against opponent).

#### Scenario: Own team and opponent team selectors
- **WHEN** user navigates to `/opponent-analysis`
- **THEN** two dropdowns are shown: one for own team, one for opponent team, populated from `GET /api/teams`

#### Scenario: Mode toggle present
- **WHEN** user navigates to `/opponent-analysis`
- **THEN** a tab toggle showing "排阵生成" and "已保存对比" is displayed

#### Scenario: Opponent lineup selector populated after opponent team selection
- **WHEN** user selects an opponent team
- **THEN** a lineup dropdown is populated with that team's saved lineups (from `GET /api/teams/{id}/lineups`)
- **AND** if the opponent team has no saved lineups, the dropdown shows "对手队伍暂无保存排阵" and action buttons are disabled

#### Scenario: Analyze button triggers API call (排阵生成 mode)
- **WHEN** user clicks "分析" with own team and opponent lineup selected in 排阵生成 mode
- **THEN** `POST /api/lineups/analyze-opponent` is called
- **AND** a loading state is shown during the call

#### Scenario: Compare button triggers API call (已保存对比 mode)
- **WHEN** user clicks "对比" with own team and opponent lineup selected in 已保存对比 mode
- **THEN** `POST /api/lineups/matchup` is called
- **AND** a loading state is shown during the call

#### Scenario: UTR recommendation panel displayed (排阵生成 mode)
- **WHEN** the API returns successfully in 排阵生成 mode
- **THEN** the UTR recommendation lineup is shown in a LineupCard
- **AND** a line analysis table shows: position, own UTR, opponent UTR, delta, and win probability label per line
- **AND** the expected score is displayed (e.g. "预期得分: 6.2 / 10")

#### Scenario: AI recommendation panel displayed (排阵生成 mode)
- **WHEN** the API returns successfully in 排阵生成 mode
- **THEN** the AI recommendation lineup is shown in a separate LineupCard
- **AND** if `aiUsed: true`, the explanation text is shown
- **AND** if `aiUsed: false`, a warning "AI 不可用" is shown

#### Scenario: Saved lineup comparison results displayed (已保存对比 mode)
- **WHEN** the matchup API returns successfully
- **THEN** own saved lineups are shown ranked by expected score with verdict badges

#### Scenario: Error displayed on API failure
- **WHEN** the API call fails (4xx or 5xx)
- **THEN** an error message is shown; the result panels are hidden

#### Scenario: Nav link present in sidebar
- **WHEN** user views any page
- **THEN** a nav link "对手分析" is shown in the sidebar pointing to `/opponent-analysis`

#### Scenario: AI recommendation button shown after best-three results (排阵生成 mode)
- **WHEN** the UTR best-three results are displayed in 排阵生成 mode
- **THEN** an "AI 推荐" button (purple) is shown
- **WHEN** user clicks "AI 推荐"
- **THEN** `POST /api/lineups/matchup` is called with `includeAi: true` and no `ownLineupId`
- **AND** the AI recommendation result is displayed in a separate card with purple border/title
- **AND** the AI card contains: per-line comparison (own | delta badge | opponent), expected score, and AI reasoning text
- **AND** if `aiUsed: false`, a warning "AI 不可用" badge is shown and the highest UTR-score lineup is used as fallback

#### Scenario: Opponent lineup preview shown after selection (已保存对比 mode)
- **WHEN** user selects an opponent lineup in 已保存对比 mode
- **THEN** a preview of that lineup's players is shown below the dropdown (D1-D4, format "D1: 甲 + 乙")
- **WHEN** user selects own lineup in 已保存对比 mode
- **THEN** a preview of the own lineup's players is shown below that dropdown
- Preview style: simple, small gray text for confirmation purposes

#### Scenario: AI line commentary button shown after UTR comparison results (已保存对比 mode)
- **WHEN** UTR line-by-line comparison results are displayed in 已保存对比 mode
- **THEN** an "AI 逐线评析" button (purple) is shown
- **WHEN** user clicks "AI 逐线评析"
- **THEN** `POST /api/lineups/matchup-commentary` is called
- **AND** an AI commentary card is displayed showing per-line (D1-D4) analysis text, positioned below or adjacent to each UTR comparison row
- **AND** if AI is unavailable, each line shows the rule-based fallback text
- **AND** the commentary card does not include a recommended lineup, only text analysis
