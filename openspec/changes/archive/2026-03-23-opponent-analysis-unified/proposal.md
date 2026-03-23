## Why

The opponent analysis feature currently splits into two separate tabs ("排阵生成" and "已保存对比"), creating a fragmented UX where both modes do fundamentally the same thing — compare lineups against an opponent. Merging them into one cohesive flow, anchored on saved lineups, makes the feature simpler and more powerful.

## What Changes

- Remove the two-tab structure; replace with a single unified opponent analysis page
- Analysis is always based on **saved lineups** (own team and opponent team), not on generated candidates
- **Mode A — Best 3 wins**: Select an opponent's saved lineup → automatically find the top 3 own saved lineups with highest win probability and display them ranked
- **Mode B — Head-to-head**: Select one own saved lineup AND one opponent saved lineup → show detailed per-line UTR comparison with win probabilities
- Default analysis method is **UTR probability comparison**; user can optionally trigger **AI lineup recommendation** which picks the best own saved lineup from the full set and shows AI reasoning

## Capabilities

### New Capabilities
- (none — this is a redesign of existing pages, no brand-new capability)

### Modified Capabilities
- `opponent-analysis`: Page is restructured; removes generated-candidate flow entirely; both analysis modes now use only saved lineups; AI is opt-in from saved lineup set
- `saved-lineup-matchup`: Absorbed into the unified opponent analysis page; no longer a separate tab or composable

## Impact

- `frontend/src/views/OpponentAnalysis.vue` — full rewrite
- `frontend/src/composables/useOpponentAnalysis.js` — update to support saved-lineup-based analysis
- `frontend/src/composables/useSavedLineupMatchup.js` — logic merged into unified flow; file may be retired
- Backend `/api/lineups/analyze-opponent` — currently accepts generated candidates; needs to be replaced or supplemented with a saved-lineup-aware endpoint
- Backend `/api/lineups/matchup` — currently returns all saved lineup comparisons; adapt to return top-3 wins for Mode A, and single comparison for Mode B
- `ZhipuAiService` / `OpponentAnalysisService` — AI now operates on saved lineups instead of generated candidates
