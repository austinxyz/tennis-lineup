## Why

Users need to compare their lineup candidates against a known opponent lineup to decide which of their valid lineup options gives the best chance of winning. Currently the system generates lineups with no regard for the opponent, leaving the user to do manual UTR comparison.

## What Changes

- New page `OpponentAnalysis.vue` at route `/opponent-analysis`
- Two recommendation modes, user can switch between them:
  - **UTR 比较模式**: Deterministic scoring — each pair's UTR delta vs the matched opponent pair determines win probability (80% / 60% / 50% / 40% / 20%), expected score = Σ(points × win_probability) across D1–D4; the lineup with the highest expected score is recommended
  - **AI 建议模式**: Pass all valid candidate lineups + opponent lineup context to Zhipu AI and ask it to select the best lineup with explanation
- New API endpoint: `POST /api/lineups/analyze-opponent`
- Per-line win probability display (e.g. "D3: 60% 赢" / "D1: 50% 对等")
- Nav sidebar link added

## Capabilities

### New Capabilities
- `opponent-analysis`: Opponent-aware lineup recommendation with UTR comparison scoring and AI suggestion modes

### Modified Capabilities
- `lineup-generation`: `ZhipuAiService.buildPrompt()` extended to accept optional opponent lineup context for AI mode

## Impact

- **Backend**: New `OpponentAnalysisRequest` DTO, `OpponentAnalysisService`, endpoint in `LineupController`
- **Frontend**: New view `OpponentAnalysis.vue`, new composable `useOpponentAnalysis.js`, nav link in sidebar
- **No model changes**: Reuses existing `Lineup`, `Pair`, `Team` models
- **No storage changes**: Analysis results are never persisted
- **Risk**: AI mode depends on Zhipu API availability; fallback is UTR mode result
