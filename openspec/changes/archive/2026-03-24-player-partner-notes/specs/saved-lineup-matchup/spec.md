## MODIFIED Requirements

### Requirement: Matchup API supports partner notes context for AI
`POST /api/lineups/matchup` SHALL accept two new optional fields: `ownPartnerNotes` (array) and `opponentPartnerNotes` (array). Each entry is `{player1Name, player2Name, note}`.

#### Scenario: Partner notes forwarded to AI prompt
- **WHEN** `includeAi: true` and `ownPartnerNotes` or `opponentPartnerNotes` are non-empty
- **THEN** the AI prompt includes a "搭档笔记" section with own-team and opponent notes formatted as `[player1 + player2]: note`

#### Scenario: Request without partner notes still works
- **WHEN** `ownPartnerNotes` and `opponentPartnerNotes` are absent or empty arrays
- **THEN** the API behaves exactly as before — no partner notes section in the prompt
