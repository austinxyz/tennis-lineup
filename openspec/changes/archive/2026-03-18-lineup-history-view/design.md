## Context

Backend lineup save/delete/list APIs are fully implemented (`LineupService`, `TeamController`). The `getLineupsByTeam` method already enriches pair objects with `player1Utr`, `player1Gender`, `player2Utr`, `player2Gender` from current player data. The frontend has a `LineupCard` component that renders pair detail with gender badges and UTR when the fields are present. There is no frontend page to browse or delete saved lineups, and no duplicate detection on save.

## Goals / Non-Goals

**Goals:**
- Dedup check in `LineupService.saveLineup`: compare pair player sets against existing saved lineups; reject with 400 if identical
- Frontend lineup history tab/panel on the lineup generator page: lists saved lineups for the current team, shows full player detail (gender + UTR), allows deletion
- History view reuses `LineupCard` component with `showPlayerUtr=true`

**Non-Goals:**
- Editing saved lineups
- Pagination (teams rarely have >20 saved lineups)
- Bulk delete
- Comparing two saved lineups side by side

## Decisions

### Dedup Logic (Backend)
Two lineups are considered duplicates if their 8 participating player IDs are identical (order-insensitive). Implementation: collect the set of all player IDs across all 4 pairs, then compare with existing saved lineups. This is the simplest meaningful dedup — avoids saving the exact same player selection twice, regardless of position assignment order.

Why not compare positions too? Users might want to save different position assignments of the same players (e.g., swap D1/D2 partners). However, since positions are assigned deterministically by UTR order, the same player set always produces the same arrangement — so player-set equality is sufficient.

Error response: `400 VALIDATION_ERROR "该排阵已保存，请勿重复保存"`

### Frontend History View
Add a "已保存排阵" section to the existing lineup generator page (`LineupGeneratorView.vue`), below the results panel, OR as a separate route `/teams/:id/lineups`. Separate route is cleaner — keeps the generator page focused and avoids cluttering the layout.

Route: `/teams/:id/lineups` → `LineupHistoryView.vue`
- Fetches `GET /api/teams/{id}/lineups` on mount
- Renders each lineup as a `LineupCard` with `showPlayerUtr=true`
- Each card has a delete button; calls `DELETE /api/lineups/{id}` then removes from list
- Empty state: "暂无保存的排阵"
- Link from team detail or lineup generator

### Composable
New `useLineupHistory.js` composable handles fetch + delete, following existing composable patterns (loading/error state, same API call style as `usePlayers.js`).

## Risks / Trade-offs

- **Player data drift**: If a player's UTR is updated after a lineup is saved, the history view shows the current UTR (from enrichment), not the UTR at save time. This is intentional — always show current rating — but worth noting.
- **Dedup on player set only**: Won't catch lineups with different players that happen to have the same structure. This is acceptable since lineup identity is defined by which players are playing.
