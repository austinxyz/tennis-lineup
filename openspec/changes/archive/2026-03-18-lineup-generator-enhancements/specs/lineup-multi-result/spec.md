## ADDED Requirements

### Requirement: Generate and return multiple lineup candidates
The system SHALL return up to 6 ranked lineup candidates from `POST /api/lineups/generate`, ordered by the selected strategy heuristic (best first). If fewer than 6 valid candidates exist, all are returned.

#### Scenario: Returns up to 6 candidates with preset balanced strategy
- **WHEN** client posts a valid generate request with `strategyType: "preset"` and `preset: "balanced"`
- **THEN** system returns HTTP 200 with a JSON array of 1–6 Lineup objects, ordered by ascending variance of pair UTRs (best balanced first)

#### Scenario: Returns up to 6 candidates with preset aggressive strategy
- **WHEN** client posts a valid generate request with `strategyType: "preset"` and `preset: "aggressive"`
- **THEN** system returns HTTP 200 with a JSON array of 1–6 Lineup objects, ordered by descending D1+D2+D3 combined UTR sum (strongest top-three first)

#### Scenario: Fewer than 6 valid candidates exist
- **WHEN** the roster produces fewer than 6 valid combinations
- **THEN** system returns all valid candidates (1 to 5) in a JSON array

#### Scenario: Only the first (best) lineup is saved to history
- **WHEN** system returns multiple candidates
- **THEN** only the first element of the returned array is persisted to `team.lineups`; the others are ephemeral

---

### Requirement: Frontend displays multiple lineup results in a tabbed 3×2 grid
The lineup generator page SHALL display result candidates in a grid of tabs (3 columns × 2 rows), with each tab showing one `LineupCard`.

#### Scenario: Tab grid renders one tab per result
- **WHEN** the generate response contains N lineups (1 ≤ N ≤ 6)
- **THEN** N tab buttons are shown in the result panel, arranged as up to 3 per row

#### Scenario: First tab is active by default
- **WHEN** results are loaded
- **THEN** tab 1 is selected and its `LineupCard` is displayed

#### Scenario: Clicking a tab switches the displayed lineup
- **WHEN** user clicks tab 3
- **THEN** the third lineup's `LineupCard` is displayed

#### Scenario: Tabs are labelled "方案 1" through "方案 N"
- **WHEN** N lineups are returned
- **THEN** tab labels are "方案 1", "方案 2", … "方案 N"

---

### Requirement: Two-column layout for the lineup generator page
The lineup generator page SHALL use a responsive two-column layout: left column for strategy selection and player constraints, right column for lineup results.

#### Scenario: Layout renders two columns on large screens
- **WHEN** the page is viewed at ≥ lg breakpoint (≥ 1024px)
- **THEN** controls are in a left column and results are in a right column side by side

#### Scenario: Layout stacks vertically on small screens
- **WHEN** the page is viewed below lg breakpoint
- **THEN** controls appear above results (stacked layout)
