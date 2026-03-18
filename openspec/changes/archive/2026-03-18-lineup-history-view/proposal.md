## Why

After lineup generation and manual saving are working, users need a way to review saved lineups and avoid cluttering storage with duplicate saves. The current state has no history view and no protection against saving the same lineup multiple times.

## What Changes

- **防重复保存**: When saving a lineup, the system checks if an identical lineup already exists (same 8 players in same pair assignments). If a duplicate is detected, the save is rejected with a clear error message.
- **排阵历史页面**: A new frontend page (or panel within TeamDetail) that lists all saved lineups for the selected team in reverse chronological order. Each saved lineup can be deleted individually.
- **性别和UTR显示**: The history view renders each lineup card with full player detail — gender badge (M/F) and individual UTR — by consuming the already-enriched `player1Utr`, `player1Gender`, `player2Utr`, `player2Gender` fields that `GET /api/teams/{id}/lineups` already returns.

## Capabilities

### New Capabilities
- `lineup-save-dedup`: Duplicate lineup detection and rejection on save. Compares the set of player IDs in each pair (order-insensitive) against existing saved lineups for the same team.

### Modified Capabilities
- `lineup-history`: Add frontend lineup history view. The existing spec covers the backend API (GET/DELETE); this change adds the frontend page requirement and the display requirement for gender/UTR enrichment in the history view.

## Impact

- **Backend**: `LineupService.saveLineup` — add dedup check before persisting
- **Frontend**: New view component `LineupHistoryView.vue` or tab within team detail; uses existing `GET /api/teams/{id}/lineups` and `DELETE /api/lineups/{id}` endpoints
- **Frontend**: Reuse `LineupCard` component for history display with `showPlayerUtr=true`
- **No API changes**: All required backend endpoints already exist
