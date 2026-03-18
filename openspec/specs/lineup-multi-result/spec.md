## Requirements

### Requirement: Generate and return multiple lineup candidates
The system SHALL return up to 6 ranked lineup candidates from `POST /api/lineups/generate`, ordered by the selected strategy heuristic (best first). Candidates with identical 8-player sets are deduplicated server-side before the limit is applied — only the highest-ranked candidate per unique player set is returned.

#### Scenario: Returns up to 6 candidates with preset balanced strategy
- **WHEN** client posts a valid generate request with `strategyType: "preset"` and `preset: "balanced"`
- **THEN** system returns HTTP 200 with a JSON array of 1–6 Lineup objects, ordered by ascending variance of pair UTRs (best balanced first)

#### Scenario: Returns up to 6 candidates with preset aggressive strategy
- **WHEN** client posts a valid generate request with `strategyType: "preset"` and `preset: "aggressive"`
- **THEN** system returns HTTP 200 with a JSON array of 1–6 Lineup objects, ordered by descending D1+D2+D3 combined UTR sum (strongest top-three first)

#### Scenario: Fewer than 6 valid candidates exist
- **WHEN** the roster produces fewer than 6 valid combinations
- **THEN** system returns all valid candidates (1 to 5) in a JSON array

#### Scenario: Duplicate player sets deduplicated before limit
- **WHEN** multiple raw candidates share the same 8 player IDs (differing only in pair assignments)
- **THEN** only the highest-ranked among them is included in the returned array; the others are discarded before the 6-candidate limit is applied

#### Scenario: Generated candidates are not auto-saved
- **WHEN** system returns multiple candidates
- **THEN** none are persisted automatically; candidates are ephemeral until the user explicitly saves one

---

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
- **THEN** "方案 1" card has a distinct visual treatment (e.g., green border or badge) indicating it is the best candidate

#### Scenario: Single lineup returned
- **WHEN** generation returns exactly 1 lineup
- **THEN** that lineup card is displayed normally without empty placeholders for missing slots

#### Scenario: No lineups yet
- **WHEN** no generation has been triggered
- **THEN** the results area shows an empty-state placeholder message

---

### Requirement: Gender badge in lineup result cards
Each player name displayed in a lineup result card (LineupCard) SHALL be accompanied by a gender badge using the `player1Gender`/`player2Gender` fields from the Pair model.

#### Scenario: Male player shows M badge in card
- **WHEN** `pair.player1Gender === 'male'`
- **THEN** the player1 entry SHALL display an "M" badge in a blue color scheme

#### Scenario: Female player shows F badge in card
- **WHEN** `pair.player2Gender === 'female'`
- **THEN** the player2 entry SHALL display an "F" badge in a pink/red color scheme

#### Scenario: Gender badge visible alongside UTR
- **WHEN** `showPlayerUtr` is true
- **THEN** the display format SHALL be: `[M/F badge] Name (UTR)`

---

### Requirement: Two-column scrollable layout for the lineup generator page
The lineup generator page SHALL use a responsive two-column layout. Both columns SHALL be independently scrollable with equal maximum height. The generate button SHALL remain visible at all times without requiring the user to scroll.

#### Scenario: Layout renders two columns on large screens
- **WHEN** the page is viewed at ≥ lg breakpoint (≥ 1024px)
- **THEN** controls are in a left column and results are in a right column side by side

#### Scenario: Layout stacks vertically on small screens
- **WHEN** the page is viewed below lg breakpoint
- **THEN** controls appear above results (stacked layout)

#### Scenario: Results panel is independently scrollable
- **WHEN** more than 2 lineup cards overflow the visible area
- **THEN** the right column shows a vertical scrollbar; the left column does not scroll with it

#### Scenario: Generate button always visible
- **WHEN** the player constraint list is long enough to require scrolling
- **THEN** the "生成排阵" button remains pinned at the bottom of the left column and does not scroll out of view
