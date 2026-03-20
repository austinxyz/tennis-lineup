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

### Requirement: Team management player list sorting
The team detail page SHALL display players sorted by gender first (males before females), then by UTR descending within each gender group.

#### Scenario: Males appear before females in team player list
- **WHEN** user views the player list on the team detail page
- **THEN** all male players appear above all female players

#### Scenario: Within each gender, higher UTR appears first
- **WHEN** two players of the same gender are displayed
- **THEN** the player with higher UTR appears first

#### Scenario: Sort is display-only
- **WHEN** the sort order is applied
- **THEN** underlying storage order of players in `tennis-data.json` is NOT changed; only the rendered order is affected
