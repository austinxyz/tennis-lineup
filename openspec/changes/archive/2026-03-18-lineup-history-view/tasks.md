## 1. Backend: Duplicate Save Prevention

- [x] 1.1 In `LineupService.saveLineup`, collect the set of 8 player IDs from the incoming lineup's pairs
- [x] 1.2 Compare against all existing saved lineups for the team; if any has the same player ID set, throw `IllegalArgumentException("该排阵已保存，请勿重复保存")`
- [x] 1.3 Add unit tests in `LineupServiceTest` for: duplicate rejected, non-duplicate accepted, first save accepted

## 2. Frontend: Lineup History Composable

- [x] 2.1 Create `frontend/src/composables/useLineupHistory.js` with `fetchLineups(teamId)` and `deleteLineup(lineupId)` functions, following the same pattern as `usePlayers.js` (loading/error reactive state)
- [x] 2.2 Add API calls: `GET /api/teams/{id}/lineups` and `DELETE /api/lineups/{id}`
- [x] 2.3 Write unit tests in `frontend/src/composables/__tests__/useLineupHistory.test.js`

## 3. Frontend: Lineup History View

- [x] 3.1 Create `frontend/src/views/LineupHistoryView.vue` — fetches lineups on mount using `useLineupHistory`, renders `LineupCard` for each with `showPlayerUtr=true`
- [x] 3.2 Add empty state message "暂无保存的排阵" when lineup list is empty
- [x] 3.3 Add delete button per lineup card; on click call `deleteLineup(id)` and remove from list
- [x] 3.4 Show error message if delete fails (404 or network error)
- [x] 3.5 Add route `/teams/:id/lineups` to `frontend/src/router/index.js`
- [x] 3.6 Add navigation link to history page from TeamDetail or NavSidebar

## 4. Frontend: Tests for History View

- [x] 4.1 Write unit tests in `frontend/src/views/__tests__/LineupHistoryView.test.js` covering: list render, empty state, delete success, delete error
