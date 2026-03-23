## Context

The app already generates valid lineup candidates from a team. This change adds a dedicated analysis page where a user picks their team and an opponent team's saved lineup, then gets a recommended lineup with per-line win probability.

Two data paths exist:
1. **UTR comparison** — pure math, no external dependency, always available
2. **AI mode** — calls Zhipu AI (`ZhipuAiService`), may time out or be unconfigured

The existing `ZhipuAiService.selectBestLineup(candidates, strategy)` only accepts a strategy string; it needs to accept opponent context as well.

## Goals / Non-Goals

**Goals:**
- Recommend the best own-team lineup against a specific opponent lineup
- Show per-line UTR delta and win probability in both modes
- UTR mode uses deterministic expected-score algorithm (user-specified win probability thresholds)
- AI mode asks Zhipu to select with opponent context; falls back to UTR result if AI fails

**Non-Goals:**
- Persisting analysis results
- Accepting an opponent lineup entered manually (must be a saved lineup from an existing team)
- Supporting opponent lineups that are not stored in `tennis-data.json`

## Decisions

### Decision 1: UTR comparison algorithm produces an expected score, not win/lose
Each position carries points: D1=1, D2=2, D3=3, D4=4 (total 10 points).
Expected score = Σ over positions of (position_points × win_probability).
The lineup with the highest expected score is recommended.

Win probability thresholds (own pair combinedUtr − opponent pair combinedUtr = delta):
- delta > 1.0 → 80%
- delta > 0.5 → 60%
- delta ≥ −0.5 → 50%
- delta ≥ −1.0 → 40%
- delta < −1.0 → 20%

**Alternative considered**: return only win/lose per line. Rejected because it loses resolution when most lines are 50%.

### Decision 2: AI mode extends ZhipuAiService with opponent context parameter
`ZhipuAiService` gets a new overload / parameter for optional opponent lineup. The prompt is extended to include opponent pair UTRs. If `apiKey` is blank or AI call fails, service automatically returns the UTR-mode result.

**Alternative**: Separate `AiOpponentService`. Rejected to keep AI logic in one place.

### Decision 3: Backend returns both UTR recommendation and AI recommendation in one response
`POST /api/lineups/analyze-opponent` returns:
```json
{
  "utrRecommendation": {
    "lineup": { ... },
    "lineAnalysis": [
      { "position": "D1", "ownCombinedUtr": 9.5, "opponentCombinedUtr": 8.8, "delta": 0.7, "winProbability": 0.6, "label": "60% 赢" }
    ],
    "expectedScore": 6.2,
    "opponentExpectedScore": 3.8
  },
  "aiRecommendation": {
    "lineup": { ... },
    "explanation": "AI 选择理由...",
    "aiUsed": true
  }
}
```
Frontend shows both panels side-by-side (or tabbed on mobile), user sees both results.

**Alternative**: Two separate endpoints. Rejected because both require the same candidate generation step; batching is more efficient.

### Decision 4: Reuse existing lineup generation pipeline
`LineupGenerationService.generateCandidates(teamId, ...)` already generates all valid lineups. `OpponentAnalysisService` calls it directly. Constraints (exclude/include/pin) are also supported in the request.

## Risks / Trade-offs

- [AI timeout] Zhipu AI may not respond within 3 seconds → Mitigation: AI recommendation field in response will have `aiUsed: false` and `explanation: "AI 不可用，已用UTR分析代替"`, lineup defaults to UTR recommendation
- [Opponent lineup stale] Opponent lineup's stored UTRs may be outdated → Mitigation: when loading opponent lineup for analysis, enrich pair UTRs from current opponent team player data (same pattern as `LineupService.getLineupsByTeam`)
- [No opponent lineups] Opponent team may have no saved lineups → Mitigation: frontend disables analysis button, shows "对手队伍暂无保存排阵"

## Migration Plan

No data migration needed. New endpoint, new frontend page. Backend restart required after Java changes.
