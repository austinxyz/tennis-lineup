## 1. Backend — DTO and Model

- [x] 1.1 Create `OpponentAnalysisRequest.java` DTO with fields: `teamId`, `opponentTeamId`, `opponentLineupId`, `strategyType`, `naturalLanguage`, `includePlayers`, `excludePlayers`, `pinPlayers`
- [x] 1.2 Create `LineAnalysis.java` model with fields: `position`, `ownCombinedUtr`, `opponentCombinedUtr`, `delta`, `winProbability`, `label`
- [x] 1.3 Create `UtrRecommendation.java` response model with fields: `lineup` (Lineup), `lineAnalysis` (List<LineAnalysis>), `expectedScore`, `opponentExpectedScore`
- [x] 1.4 Create `AiRecommendation.java` response model with fields: `lineup` (Lineup), `explanation`, `aiUsed`
- [x] 1.5 Create `OpponentAnalysisResponse.java` with fields: `utrRecommendation`, `aiRecommendation`

## 2. Backend — OpponentAnalysisService

- [x] 2.1 Create `OpponentAnalysisService.java` with method `analyze(OpponentAnalysisRequest)` that: loads own team, loads opponent team, loads and enriches opponent lineup UTRs from current opponent roster
- [x] 2.2 Implement `generateCandidates()` call — reuse `LineupGenerationService` with constraints from request (include/exclude/pin)
- [x] 2.3 Implement `computeUtrRecommendation()` — for each candidate, compute per-line win probability using the 5-threshold rules; calculate expected score; select highest; build `LineAnalysis` list and `UtrRecommendation`
- [x] 2.4 Implement `computeAiRecommendation()` — call extended `ZhipuAiService` with opponent context; on failure/timeout return UTR recommendation with `aiUsed=false`
- [x] 2.5 Extend `ZhipuAiService.buildPrompt()` to accept optional opponent lineup (add overload or new method `buildPromptWithOpponent(candidates, strategy, opponentLineup)`) including opponent pair UTRs in the prompt

## 3. Backend — Controller and Tests

- [x] 3.1 Add `POST /api/lineups/analyze-opponent` endpoint to `LineupController` that calls `OpponentAnalysisService.analyze()` and returns `OpponentAnalysisResponse`
- [x] 3.2 Write `OpponentAnalysisServiceTest.java` — test UTR win probability thresholds (all 5 cases), expected score calculation, opponent-not-found, team-not-found, and AI fallback behavior
- [x] 3.3 Restart backend and verify `POST /api/lineups/analyze-opponent` returns correct structure

## 4. Frontend — Composable and API

- [x] 4.1 Create `useOpponentAnalysis.js` composable with `analyzeOpponent(ownTeamId, opponentTeamId, opponentLineupId, constraints)` calling `POST /api/lineups/analyze-opponent`; expose `loading`, `error`, `result`
- [x] 4.2 Write `useOpponentAnalysis.test.js` — test successful call, error handling, loading state

## 5. Frontend — OpponentAnalysis Page

- [x] 5.1 Create `OpponentAnalysis.vue` view at `frontend/src/views/OpponentAnalysis.vue` with two team dropdowns (loaded from `useTeams`) and opponent lineup dropdown (loaded from `useLineupHistory` or direct API call when opponent team changes)
- [x] 5.2 Add "分析" button — disabled unless own team + opponent lineup both selected; clicking calls `analyzeOpponent()`
- [x] 5.3 Add UTR recommendation panel: `LineupCard` for recommended lineup + line analysis table (position / 己方UTR / 对手UTR / 差值 / 胜率) + expected score row
- [x] 5.4 Add AI recommendation panel: separate `LineupCard` + explanation text; show "AI 不可用" warning when `aiUsed: false`
- [x] 5.5 Add win probability label color coding: "80% 赢" / "60% 赢" in green, "对等" in gray, "60% 输" / "80% 输" in red
- [x] 5.6 Handle empty opponent lineups state: show "对手队伍暂无保存排阵", disable analyze button

## 6. Frontend — Nav and Routing

- [x] 6.1 Add `/opponent-analysis` route to `frontend/src/router/index.js`
- [x] 6.2 Add "对手分析" nav link to sidebar component

## 7. Frontend — Tests

- [x] 7.1 Write `OpponentAnalysis.test.js` — test: initial disabled state, opponent lineup dropdown populated after team selection, analyze button triggers composable call, UTR panel rendered, AI panel rendered, `aiUsed: false` warning shown, error state shown

## 8. Test Report

- [ ] 8.1 Run backend tests (`mvn test`) and confirm all pass
- [ ] 8.2 Run frontend tests (`npm test`) and confirm all pass
- [ ] 8.3 Run E2E tests (`npm run test:e2e`) and confirm all pass
