## MODIFIED Requirements

### Requirement: Matchup API supports optional own-lineup filter and AI
`POST /api/lineups/matchup` SHALL accept two new optional fields: `ownLineupId` (string) and `includeAi` (boolean, default false).

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
