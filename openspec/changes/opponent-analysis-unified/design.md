## Context

The opponent analysis page currently has two tabs:
- **排阵生成**: generates lineup candidates on-the-fly and runs UTR + optional AI recommendation
- **已保存对比**: compares all own saved lineups against a selected opponent saved lineup

Both tabs share the same team/opponent selectors but operate differently. The generate mode is more complex (generates thousands of candidates) but produces results that aren't anchored to real saved lineups. The saved-matchup mode is simpler and more relevant to actual match prep.

The redesign unifies the page around saved lineups only, introduces a "Best 3 wins" quick mode, and makes AI analysis available on demand from saved lineup data.

## Goals / Non-Goals

**Goals:**
- Single page, no tab toggle — one consistent interaction model
- Mode A (Best 3): select opponent's saved lineup → auto-rank own saved lineups by win probability → show top 3
- Mode B (Head-to-head): select one own saved lineup + one opponent saved lineup → show detailed UTR line comparison + optional AI analysis
- AI analysis is opt-in and operates on saved lineups (not generated candidates)
- AI provides a specific recommendation from own saved lineups with reasoning text

**Non-Goals:**
- On-the-fly lineup generation (removed entirely)
- Showing more than top 3 in Mode A (keep it focused)
- Changing the UTR win probability algorithm

## Decisions

### Decision 1: Two modes surfaced as a radio/toggle, not tabs

The two modes (Best 3 vs Head-to-head) are differentiated by whether an own-lineup selector is shown. Default is Mode A (Best 3) since that's the most common use case — arriving to prep for a match against a known opponent lineup.

A horizontal radio toggle ("最佳三阵" / "逐线对比") replaces the current tab bar. Controls below adapt based on selection.

### Decision 2: Backend — reuse `/api/lineups/matchup` for Mode A, add own-lineup filter for Mode B

- **Mode A**: Call `POST /api/lineups/matchup` (existing), take top 3 results from response
- **Mode B**: Call `POST /api/lineups/matchup` with an additional `ownLineupId` filter, returns a single result

Rather than a new endpoint, add optional `ownLineupId` to the matchup request. When present, the service filters own saved lineups to just that one. This keeps the backend simple.

### Decision 3: AI operates on saved lineups, not generated candidates

`POST /api/lineups/analyze-opponent` currently generates candidates. For the new flow, AI receives the sorted saved lineups (up to 5 by expected score) and picks the best one. The `analyze-opponent` endpoint is updated to accept `savedOnly: true` + the team IDs, fetching saved lineups internally instead of accepting generated candidates.

Alternatively, we extend the matchup endpoint with `includeAi: true`. This is cleaner — one endpoint for all analysis.

**Final decision**: extend `/api/lineups/matchup` with `includeAi: boolean` and `ownLineupId: string?`. When `includeAi: true` and no `ownLineupId`, AI picks from top-5 saved lineups by expected score. Response gains `aiRecommendation` field alongside `results`.

### Decision 4: AI reasoning shown as text block

The Zhipu AI currently returns only a lineup index. The model (`glm-4-air`) can also explain its reasoning in the prompt. We'll add a second AI call (or extend the prompt) to return a brief explanation in Chinese alongside the index. A simple approach: append "并用一句话解释理由" to the system prompt, parse the response as `<index>|<reason>`.

Actually simpler: use a two-message approach where after getting the index, we do not make a second call. Instead, change the system prompt to allow format `数字<TAB>理由` and parse accordingly. The reasoning string is extracted and stored in `AiRecommendation.explanation`.

### Decision 5: Remove `useOpponentAnalysis` composable; unify under `useSavedLineupMatchup`

The frontend will have one composable `useOpponentMatchup` that handles all modes. Old composables are retired.

## Risks / Trade-offs

- [Risk] Removing the generate mode breaks existing frontend tests → Mitigation: rewrite tests to cover new modes only; old tests deleted
- [Risk] AI response format change (adding reasoning) may cause parse failures → Mitigation: parse defensively; fall back to empty explanation string if format doesn't match
- [Risk] Users who relied on generated-candidate analysis lose that feature → Accepted trade-off; saved-lineup analysis is more relevant for match prep
