## Requirements

### Requirement: Team detail response includes lineups
`GET /api/teams/{id}` SHALL return the team object including a `lineups` array. When no lineups have been saved, the array SHALL be empty (`[]`), never `null`.

#### Scenario: Team with no lineups returns empty lineups array
- **WHEN** `GET /api/teams/{id}` is called for a team that has no saved lineups
- **THEN** response includes `"lineups": []`

#### Scenario: Team with saved lineups returns full lineup data
- **WHEN** `GET /api/teams/{id}` is called for a team that has saved lineups
- **THEN** response includes `"lineups"` array with all saved lineup objects, each containing `id`, `createdAt`, `strategy`, `pairs`, `totalUtr`, `valid`, `violationMessages`

#### Scenario: Existing JSON without lineups field is backward compatible
- **WHEN** `tennis-data.json` contains a team object without a `lineups` field (legacy data)
- **THEN** system treats `lineups` as empty array and returns `"lineups": []` without error

---

### Requirement: Team detail view includes notes access
The team detail view SHALL include a "队员笔记" section that provides access to both personal notes and partner notes editors for that team.

#### Scenario: Notes section visible in team detail
- **WHEN** the user navigates to a team's detail page
- **THEN** a "队员笔记" section or button is visible that opens the notes editor

#### Scenario: Notes section shows two panels
- **WHEN** the user opens the 队员笔记 section
- **THEN** two panels are displayed: "个人笔记" (personal notes) and "搭档笔记" (partner notes)
