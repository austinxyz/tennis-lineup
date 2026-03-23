## Context

The UTR matchup algorithm (`winProbability`, `computeLineAnalysis`, `computeUtrRecommendation`) already exists in `OpponentAnalysisService`. This change reuses that logic to evaluate saved lineups rather than generated candidates.

## Goals / Non-Goals

**Goals:**
- Show all own saved lineups ranked by expected score against a specific opponent lineup
- Reuse the existing 5-threshold UTR probability algorithm
- Integrate into the existing opponent analysis page as a second mode

**Non-Goals:**
- Persisting matchup results
- Generating new lineup candidates (covered by existing opponent analysis mode)
- Comparing opponent's multiple lineups at once

## Decisions

### Decision 1: Single endpoint returns all own saved lineups ranked by expected score

`POST /api/lineups/matchup` request body:
```json
{
  "teamId": "team-1",
  "opponentTeamId": "team-2",
  "opponentLineupId": "lineup-opp-1"
}
```

Response:
```json
{
  "results": [
    {
      "lineup": { ...saved lineup... },
      "lineAnalysis": [...],
      "expectedScore": 7.2,
      "opponentExpectedScore": 2.8,
      "verdict": "能赢"     // "能赢" / "势均力敌" / "劣势"
    },
    ...
  ]
}
```

Results are sorted by `expectedScore` descending. All own saved lineups are evaluated — no filtering.

**Alternative**: Separate GET endpoint with query params. Rejected — POST is consistent with the existing `analyze-opponent` pattern and allows richer request bodies.

### Decision 2: verdict badge thresholds

- `expectedScore > 6` out of 10 → "能赢" (green)
- `expectedScore >= 4` → "势均力敌" (yellow)
- `expectedScore < 4` → "劣势" (red)

### Decision 3: Reuse `OpponentAnalysisService` helper methods

`LineupMatchupService` calls `OpponentAnalysisService.computeLineAnalysis()` and `OpponentAnalysisService.winProbability()` directly. Both are package-accessible methods (not private). This avoids duplicating the threshold logic.

**Alternative**: Extract shared logic to a static utility class. Deferred — one extra service call is sufficient for now.

### Decision 4: Page mode toggle

`OpponentAnalysis.vue` gains a tab toggle at the top:
- **排阵生成** — existing generate-and-recommend flow (unchanged)
- **已保存对比** — new saved lineup matchup mode

When mode is "已保存对比", the page shows:
- Own team selector + opponent team selector + opponent lineup selector (same as existing)
- "对比" button triggers `POST /api/lineups/matchup`
- Result: ranked list of own saved lineups, each showing LineupCard + line analysis table + verdict badge

## Risks / Trade-offs

- [No own saved lineups] Team may have no saved lineups → show empty state "己方队伍暂无保存排阵，请先保存排阵"
- [Stale UTRs] Saved lineup UTRs may be outdated → Mitigation: enrich own saved lineup pair UTRs from current roster at analysis time (same pattern as `getLineupsByTeam`)
- [Many saved lineups] Team with many saved lineups produces a long result list → acceptable, user can scroll
