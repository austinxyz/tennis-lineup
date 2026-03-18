## Why

The current lineup generator produces results hidden behind tabs and lacks fine-grained control: players cannot be pinned to a specific position, the scoring algorithm doesn't optimize toward the 40.5 total UTR cap, and once a lineup is selected the user cannot make manual adjustments. These limitations reduce the practical utility of the tool for coaches preparing match-day lineups.

## What Changes

- **Display all 6 lineups simultaneously** — replace tab-based navigation with a vertical or grid all-at-once layout; each lineup card always visible without clicking
- **Show per-player UTR in lineup cards** — each pair in a LineupCard displays both players' individual UTRs alongside the combined UTR
- **Maximize total UTR toward 40.5 cap** — generation algorithm re-ranks candidates to prefer lineups whose total UTR is closest to 40.5 (without exceeding it)
- **Improved balanced strategy scoring** — balanced strategy ranks candidates by how close each pair's combined UTR is to 10.125 (40.5 ÷ 4); best fit = 方案 1, next best = 方案 2, etc.
- **Position pin constraint** — user can pin a player to a specific position (D1/D2/D3/D4); all returned lineups will place that player in the specified position
- **Manual pair swap** — after lineup selection, user can swap one player from one position with one player from another position; the swap UI validates the new assignment still satisfies all hard constraints
- **Sorted player constraint list** — `PlayerConstraintSelector` renders players sorted by gender first (female before male), then UTR descending within each gender; each player row shows a verified badge

## Capabilities

### New Capabilities

- `lineup-position-pin`: Pin a specific player to a specific position (D1–D4) as a generation constraint; all returned lineup candidates must honor the pin
- `lineup-manual-swap`: After a lineup is generated and selected, allow user to pick two players from different positions and swap them; validate constraints after swap

### Modified Capabilities

- `lineup-generation`: Scoring/ranking algorithm changes — (1) candidates are ranked by proximity to 40.5 total UTR cap; (2) balanced strategy ranks by how evenly each pair's UTR is distributed toward 10.125 per line; (3) position pin constraint added to generation request
- `lineup-multi-result`: Display format changes — show all results simultaneously (no tabs); each lineup card shows per-player UTR values
- `lineup-player-constraints`: Player constraint selector sorted females-first then UTR descending; verified badge per player

## Impact

- **Backend**: `GenerateLineupRequest` adds `pinPlayers: Map<String, String>` (playerId → position); `LineupGenerationService.generateCandidates` updated for position pin filtering; `LineupService.generateMultipleAndSave` re-ranking logic updated; new `SwapRequest` DTO + `POST /api/lineups/{id}/swap` endpoint
- **Frontend**: `LineupCard.vue` updated to show per-player UTRs; `LineupResultTabs.vue` replaced with `LineupResultGrid.vue` (all-at-once display); `PlayerConstraintSelector.vue` extended with position-pin option per player, sorted females-first then UTR desc, verified badge; new `LineupSwapPanel.vue` component; `useLineup.js` adds `swapPlayers` method
- **API**: New endpoint `POST /api/lineups/{id}/swap`; `GenerateLineupRequest` gains `pinPlayers` field
- **Tests**: Backend unit tests for new scoring, position pin, swap; frontend component tests; E2E for full flow
