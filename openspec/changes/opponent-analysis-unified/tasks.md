## 1. Backend — Matchup API extensions

- [x] 1.1 Add `ownLineupId` (String, optional) and `includeAi` (boolean, default false) fields to `LineupMatchupRequest.java`
- [x] 1.2 Update `LineupMatchupService.matchup()` to filter own saved lineups to just `ownLineupId` when provided
- [x] 1.3 Add `aiRecommendation` field (type `AiRecommendation`) to `LineupMatchupResponse.java`
- [x] 1.4 When `includeAi: true` and no `ownLineupId`: call `ZhipuAiService` with top-5 own saved lineups (by expected score); populate `aiRecommendation` with the chosen lineup, its line analysis, expected scores, and explanation
- [x] 1.5 Update `ZhipuAiService` prompt to allow format `数字\t理由` (tab-separated index + one-sentence reason in Chinese); update `parseIndexFromContent` to extract both the index and the reason string
- [x] 1.6 Add `explanation` extraction to `ZhipuAiService` and expose it so callers can retrieve the AI reasoning text
- [x] 1.7 When `includeAi: true` AND `ownLineupId` is set, ignore AI and return `aiRecommendation: null`

## 2. Backend — Remove unused analyze-opponent generate flow

- [x] 2.1 Remove or deprecate the `LineupGenerationService` dependency from `OpponentAnalysisService` (generated-candidate flow no longer needed by the page)
- [x] 2.2 Keep `POST /api/lineups/analyze-opponent` endpoint intact for now but mark as internal (no frontend will call it after this change)

## 3. Backend — Tests

- [x] 3.1 Update `LineupMatchupServiceTest` to cover `ownLineupId` filter (returns single result)
- [x] 3.2 Add test: `includeAi: true` calls AI service and populates `aiRecommendation` on response
- [x] 3.3 Add test: `includeAi: true` + `ownLineupId` → AI is not called, `aiRecommendation` is null
- [x] 3.4 Add test for AI explanation extraction in `ZhipuAiServiceTest` (tab-separated format)
- [x] 3.5 Restart backend and verify all tests pass

## 4. Frontend — New composable

- [x] 4.1 Create `useOpponentMatchup.js` composable with:
  - `runBestThree(ownTeamId, opponentTeamId, opponentLineupId)` → calls matchup, returns top 3 results
  - `runHeadToHead(ownTeamId, ownLineupId, opponentTeamId, opponentLineupId)` → calls matchup with `ownLineupId`, returns single result
  - `runAiAnalysis(ownTeamId, opponentTeamId, opponentLineupId)` → calls matchup with `includeAi: true`, returns `aiRecommendation`
  - Shared `loading`, `error` refs
- [x] 4.2 Write unit tests for `useOpponentMatchup.js` covering all three functions

## 5. Frontend — Rewrite OpponentAnalysis.vue

- [x] 5.1 Remove two-tab structure; replace with mode radio toggle: "最佳三阵" (default) / "逐线对比"
- [x] 5.2 Show shared controls: own team selector + opponent team selector + opponent lineup selector
- [x] 5.3 In "最佳三阵" mode: show "查找最佳三阵" button; on result display top-3 cards using per-line comparison layout (己方|delta+badge|对手)
- [x] 5.4 In "逐线对比" mode: add own lineup selector; show "对比分析" button; display single result card with per-line comparison
- [x] 5.5 In "逐线对比" mode result: add "AI 排阵分析" button (purple); on click call `runAiAnalysis`; display AI card with per-line comparison + explanation text below
- [x] 5.6 Handle empty states: no own saved lineups warning, no opponent lineups warning
- [x] 5.7 Remove `useOpponentAnalysis.js` and `useSavedLineupMatchup.js` imports; use `useOpponentMatchup.js` exclusively
- [x] 5.8 Update or rewrite `OpponentAnalysis.test.js` to cover new modes

## 6. Cleanup and test report

- [x] 6.1 Delete or retire `useOpponentAnalysis.js` and `useSavedLineupMatchup.js` if no longer used elsewhere
- [x] 6.2 Run `npm test` and confirm all frontend tests pass; fix any failures
- [x] 6.3 Run backend tests (`mvn test`) and confirm all pass
- [x] 6.4 E2E tests (Playwright): 12 tests covering Mode A and Mode B flows
- [x] 6.5 Generate test report summarizing pass/fail counts
