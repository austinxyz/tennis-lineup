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

### Requirement: Mode A — Best 3 wins (最佳三阵) opponent lineup preview
The opponent lineup selector SHALL display both official UTR and actual UTR for each pair in the preview, enabling users to assess opponent strength before running analysis.

#### Scenario: Opponent lineup preview shows UTR and actual UTR
- **WHEN** user selects an opponent lineup from the dropdown
- **THEN** the preview below the dropdown shows each line as: `D1: 张三(UTR 8.0/实 7.5) + 李四(UTR 7.5) = 15.5 / 实 15.0`
- **AND** actual UTR is only shown when it differs from the official UTR

---

### Requirement: Mode A — UTR Best 3 line analysis with dual UTR display
Line analysis cards in the UTR best 3 results SHALL display both official UTR and actual UTR for own team and opponent team per line.

#### Scenario: Line card shows own team UTR and actual UTR
- **WHEN** the UTR best 3 results are displayed
- **THEN** each own-team pair shows `name(utr / 实actualUtr)` when actualUtr differs from utr
- **AND** the combined UTR row shows official combined UTR and actual combined UTR

#### Scenario: Line card shows opponent UTR and actual UTR
- **WHEN** the UTR best 3 results are displayed
- **THEN** each opponent pair shows `name(utr / 实actualUtr)` when actualUtr differs from utr

---

### Requirement: Win probability computed using actual UTR for both sides
All win probability calculations (`winProbability`, expected score, verdict) throughout opponent analysis SHALL use actual UTR for both own team and opponent. When a player has no `actualUtr`, the official `utr` is used as fallback.

#### Scenario: Win probability uses actual UTR when available
- **WHEN** `computeLineAnalysis` is called
- **THEN** `delta = ownActualCombinedUtr - opponentActualCombinedUtr`
- **AND** `winProbability` is derived from that actual-UTR-based delta

#### Scenario: Fallback to official UTR when no actual UTR
- **WHEN** a player has no `actualUtr` (null)
- **THEN** their official `utr` is used in the actual UTR calculation
- **AND** results are still computed without error

---

### Requirement: AI recommendation lineup uses actual UTR and partner notes in prompt
The AI recommendation for Mode A (最佳三阵 AI 推荐) SHALL include opponent actual UTR in the prompt and continue to include own/opponent player notes and partner notes.

#### Scenario: Opponent lineup in AI prompt includes actual UTR
- **WHEN** `buildPromptWithOpponent` constructs the opponent lineup section
- **THEN** each opponent player is described as `name(UTR x.x/实y.y, 备注:...)` when actual UTR differs

#### Scenario: Partner notes are included in AI prompt
- **WHEN** `ownPartnerNotes` or `opponentPartnerNotes` are non-empty
- **THEN** they are included in the prompt under 搭档笔记 section

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

### Requirement: Mode B — Head-to-head line comparison with dual UTR display
Line analysis cards in head-to-head (逐线对比) mode SHALL display both official UTR and actual UTR for own team and opponent team per line.

#### Scenario: Head-to-head card shows dual UTR for own team
- **WHEN** head-to-head result is displayed
- **THEN** each own-team pair row shows both official UTR and actual UTR (actual only when different)

#### Scenario: Head-to-head card shows dual UTR for opponent
- **WHEN** head-to-head result is displayed
- **THEN** each opponent pair row shows both official UTR and actual UTR (actual only when different)

---

### Requirement: Mode B — Head-to-head win probability uses actual UTR
Head-to-head (逐线对比) win probability and expected score SHALL use actual UTR for both sides, consistent with Mode A.

#### Scenario: Head-to-head expected score reflects actual UTR
- **WHEN** `POST /api/lineups/matchup` returns head-to-head result
- **THEN** `lineAnalysis[].winProbability` is based on actual UTR delta

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

---

### Requirement: AI line commentary (逐线评析) uses actual UTR and partner notes
The AI line commentary SHALL be generated using enriched lineup data (current player actual UTR) and SHALL include own and opponent partner notes in the prompt.

#### Scenario: Commentary prompt uses actual UTR for delta
- **WHEN** `buildCommentaryPrompt` is called
- **THEN** line delta is computed as `ownActualCombinedUtr - opponentActualCombinedUtr`

#### Scenario: Commentary prompt includes partner notes
- **WHEN** partner notes exist for own or opponent team
- **THEN** they are appended to the commentary prompt under a 搭档笔记 section

#### Scenario: Commentary enriches lineups with current player data
- **WHEN** `MatchupCommentaryService.getCommentary` is called
- **THEN** lineup pairs are enriched with the current player `actualUtr` from the repository before passing to AI

#### Scenario: Frontend passes partner notes for commentary
- **WHEN** user clicks "AI 逐线评析"
- **THEN** `useOpponentMatchup.runCommentary` fetches partner notes for both teams and includes them in `POST /api/lineups/matchup-commentary` body
