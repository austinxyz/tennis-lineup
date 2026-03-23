## 1. Backend — DTO and Response Model

- [x] 1.1 Create `LineupMatchupRequest.java` DTO with fields: `teamId`, `opponentTeamId`, `opponentLineupId`
- [x] 1.2 Create `MatchupResult.java` model with fields: `lineup` (Lineup), `lineAnalysis` (List<LineAnalysis>), `expectedScore` (double), `opponentExpectedScore` (double), `verdict` (String)
- [x] 1.3 Create `LineupMatchupResponse.java` with field: `results` (List<MatchupResult>)

## 2. Backend — LineupMatchupService

- [x] 2.1 Create `LineupMatchupService.java` with `matchup(LineupMatchupRequest)` method that: loads own team, loads opponent team, finds and enriches opponent lineup UTRs from current roster
- [x] 2.2 Implement enrichment of own saved lineup pair UTRs from current own team roster (same pattern as `LineupService.getLineupsByTeam`)
- [x] 2.3 For each own saved lineup, call `OpponentAnalysisService.computeLineAnalysis()` and compute expected score; assign verdict based on expected score thresholds (>6=能赢, >=4=势均力敌, <4=劣势)
- [x] 2.4 Sort results by `expectedScore` descending and return `LineupMatchupResponse`

## 3. Backend — Controller and Tests

- [x] 3.1 Add `POST /api/lineups/matchup` endpoint to `LineupController` calling `LineupMatchupService.matchup()`
- [x] 3.2 Write `LineupMatchupServiceTest.java` — test: verdict thresholds (all 3 cases), results sorted by score descending, own team not found (404), opponent team not found (404), opponent lineup not found (404), own team with no saved lineups returns empty results
- [x] 3.3 Restart backend and verify `POST /api/lineups/matchup` returns correct structure

## 4. Frontend — Composable

- [x] 4.1 Create `useSavedLineupMatchup.js` composable with `runMatchup(ownTeamId, opponentTeamId, opponentLineupId)` calling `POST /api/lineups/matchup`; expose `loading`, `error`, `matchupResults`
- [x] 4.2 Write `useSavedLineupMatchup.test.js` — test: successful call sets matchupResults, error handling, loading state

## 5. Frontend — OpponentAnalysis Page Update

- [x] 5.1 Add mode toggle tabs ("排阵生成" / "已保存对比") to `OpponentAnalysis.vue`; default to "排阵生成" tab
- [x] 5.2 Import `useSavedLineupMatchup` and add "对比" button in "已保存对比" mode; disable when own team not selected or opponent lineup not selected
- [x] 5.3 When own team is selected in "已保存对比" mode, fetch own team's saved lineups; show "己方队伍暂无保存排阵，请先保存排阵" when empty
- [x] 5.4 Add matchup results panel: for each result show verdict badge (green/yellow/red), expected score, per-line analysis table, and LineupCard
- [x] 5.5 Wire `onOpponentTeamChange` to also fetch opponent lineups when in "已保存对比" mode (already shared with 排阵生成 mode)

## 6. Frontend — Tests

- [x] 6.1 Update `OpponentAnalysis.test.js` — add tests for: mode toggle switches view, "对比" button triggers matchup composable, results ranked with verdicts displayed, empty own lineups state shown

## 7. Test Report

- [x] 7.1 Run backend tests (`mvn test`) and confirm all pass
- [x] 7.2 Run frontend tests (`npm test`) and confirm all pass
