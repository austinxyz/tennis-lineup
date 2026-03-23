## MODIFIED Requirements

### Requirement: Unified opponent analysis page
The opponent analysis page at `/opponent-analysis` SHALL be redesigned as a single unified page with no tab toggle. All analysis modes operate on **saved lineups only** (own team and opponent team).

#### Scenario: Team selectors shown on load
- **WHEN** user navigates to `/opponent-analysis`
- **THEN** two dropdowns are shown: own team and opponent team, populated from `GET /api/teams`

#### Scenario: Opponent lineup selector populated after opponent team selection
- **WHEN** user selects an opponent team
- **THEN** a lineup dropdown is populated with that team's saved lineups (from `GET /api/teams/{id}/lineups`)
- **AND** if no saved lineups exist, dropdown shows "对手队伍暂无保存排阵"

---

### Requirement: Mode A — Best 3 wins (最佳三阵)
The page SHALL support a mode where user selects an opponent saved lineup and the system automatically finds the top 3 own saved lineups with highest win probability.

#### Scenario: Best 3 triggered with opponent lineup selected
- **WHEN** user selects own team + opponent lineup and clicks "查找最佳三阵"
- **THEN** `POST /api/lineups/matchup` is called without `ownLineupId`
- **AND** the top 3 results by expected score are displayed

#### Scenario: Top 3 results displayed
- **WHEN** results return
- **THEN** up to 3 own saved lineup cards are shown, ranked by expected score (highest first)
- **AND** each card shows: verdict badge, per-line comparison (己方 | delta+badge | 对手), expected score footer
- **AND** if fewer than 3 own saved lineups exist, all available are shown

#### Scenario: Own team has no saved lineups
- **WHEN** own team has no saved lineups
- **THEN** a warning "己方队伍暂无保存排阵，请先保存排阵" is shown and the button is disabled

---

### Requirement: Mode B — Head-to-head comparison (逐线对比)
The page SHALL support a mode where user selects one own saved lineup and one opponent saved lineup for detailed comparison.

#### Scenario: Own lineup selector shown in head-to-head mode
- **WHEN** user selects "逐线对比" mode
- **THEN** an own lineup dropdown appears, populated from `GET /api/teams/{ownTeamId}/lineups`

#### Scenario: Head-to-head comparison triggered
- **WHEN** user selects own team + own lineup + opponent lineup and clicks "对比分析"
- **THEN** `POST /api/lineups/matchup` is called with `ownLineupId` set to the selected own lineup
- **AND** a single result card is displayed with per-line comparison

#### Scenario: UTR comparison result displayed
- **WHEN** result returns
- **THEN** a card shows per-line comparison: position | 己方组合(UTR) | delta+badge | 对手组合(UTR)
- **AND** footer shows expected score and opponent expected score

---

### Requirement: AI analysis in head-to-head mode
In head-to-head mode, the user MAY request AI analysis. The AI evaluates own saved lineups and recommends the best one with reasoning.

#### Scenario: AI analysis button shown after UTR result
- **WHEN** head-to-head UTR result is displayed
- **THEN** an "AI 排阵分析" button is shown below the result

#### Scenario: AI analysis triggered
- **WHEN** user clicks "AI 排阵分析"
- **THEN** `POST /api/lineups/matchup` is called with `includeAi: true` and no `ownLineupId`
- **AND** loading state shown during AI call

#### Scenario: AI result displayed
- **WHEN** AI response returns with `aiRecommendation`
- **THEN** an AI result card is shown with: per-line comparison, expected score, and AI reasoning text
- **AND** if `aiUsed: false`, a warning "AI 不可用" is shown with UTR fallback

#### Scenario: Nav link present
- **WHEN** user views any page
- **THEN** a nav link "对手分析" is shown in the sidebar pointing to `/opponent-analysis`
