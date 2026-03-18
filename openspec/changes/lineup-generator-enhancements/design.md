## Context

The current `LineupGenerator.vue` is a single-column page: strategy selector → generate button → one `LineupCard` result. The backend `POST /api/lineups/generate` returns exactly one `Lineup` object, chosen by heuristic or AI from all valid candidates.

Two user needs drive this change:
1. Coaches want to compare several lineup options side-by-side before deciding.
2. Coaches need to handle player availability (injuries, rest days) or tactical commitments (star player must play) before running the algorithm.

## Goals / Non-Goals

**Goals:**
- Two-column layout: controls on the left, up to 6 result tabs on the right
- Backend returns up to 6 ranked lineup candidates in one request
- Per-player constraint UI: toggle each player as "必须上场" / "排除" / (neutral)
- Backend enforces include/exclude lists before candidate generation

**Non-Goals:**
- Saving/persisting player constraints between sessions
- Allowing constraints that are mathematically impossible (e.g., pinning 10 players) — return 400 with clear error
- Changing how individual lineup cards are rendered

## Decisions

### Decision 1: Return array of Lineups instead of wrapping in a new type
Return `List<Lineup>` (max 6) directly from `POST /api/lineups/generate`. Each lineup in the list already has all needed fields.

*Alternative considered*: Wrap in `{ lineups: [...], meta: {} }`. Rejected — over-engineering for a list that is already self-describing.

### Decision 2: Save only the first (best) lineup, not all 6
The generate-and-save flow saves `candidates.get(0)` (the strategy-selected best) to `team.lineups`. The other 5 are ephemeral "preview" options returned to the client only.

*Rationale*: Storing all 6 every time would bloat the team's lineup history with redundant entries. The user picks from the 6 on screen; if they want to keep one other than the first, a future "save this tab" feature can add that.

### Decision 3: Player constraints are request-time parameters, not persisted
`includePlayers` and `excludePlayers` are fields on `GenerateLineupRequest`. They are not stored on the team or player models.

*Rationale*: Constraints are session-specific tactical decisions. Persisting them would require a new data model, migrations, and UI to manage saved constraint sets — out of scope.

### Decision 4: Frontend constraint selector uses player list from the loaded team
`PlayerConstraintSelector.vue` receives the team's `players` array as a prop and emits `{ includePlayers: string[], excludePlayers: string[] }`. The constraint state lives in `LineupGenerator.vue`'s reactive data.

### Decision 5: Tab layout — 3 columns × 2 rows of tabs, not a scrollable carousel
Tabs are rendered as a 3×2 grid of buttons above the active `LineupCard`. This makes all options scannable at once without horizontal scrolling.

### Decision 6: Left column width ratio ~40/60
Left column (controls): `w-2/5`; right column (results): `w-3/5`. On narrow screens (<lg breakpoint) the columns stack vertically.

## Risks / Trade-offs

- **Constraint conflicts** → The algorithm will throw `IllegalArgumentException("约束冲突：必须上场球员不足8人")` if `includePlayers.size() > 8` or the pinned set violates hard constraints. Frontend shows this as an error banner.
- **Performance: more candidates needed** → Currently the algorithm returns all valid candidates and picks 1. With constraints, the valid set may shrink. We take the top-6 by strategy score from whatever valid candidates exist; if fewer than 6 are found, return all. No extra computation cost.
- **Breaking API change** → `POST /api/lineups/generate` changes response from `Lineup` to `List<Lineup>`. The existing `useLineup.js` and `LineupGenerator.vue` must be updated together. The `LineupControllerTest` must also be updated. E2E tests need updating for multi-result assertions.

## Migration Plan

1. Update backend: `GenerateLineupRequest` + `LineupService.generateMultiple` + `LineupController`
2. Update frontend: `useLineup.js` (handle array), `LineupGenerator.vue` (two-column layout + constraint state), new `PlayerConstraintSelector.vue`, new `LineupResultTabs.vue`
3. Update all affected tests (controller test, useLineup test, LineupGenerator test, E2E)
4. No data migration needed — JSON storage schema unchanged
