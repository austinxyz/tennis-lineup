## Context

The lineup generator currently produces up to 6 candidates sorted by a basic heuristic, presented in a tabbed UI. The generation algorithm optimizes only by strategy type (balanced = min variance, aggressive = max D1+D2+D3), without considering the global optimum of total UTR approaching 40.5. Users can include/exclude players but cannot pin them to a specific position or manually adjust a finalized lineup.

## Goals / Non-Goals

**Goals:**
- Re-rank candidates by proximity to 40.5 total UTR cap (regardless of strategy)
- Improve balanced strategy to rank by per-line evenness (target 10.125 per pair)
- Add position-pin constraint to generation request
- Add manual pair swap after lineup selection with real-time constraint validation
- Show all 6 lineup cards simultaneously (remove tab navigation)
- Add per-player UTR display inside each lineup card

**Non-Goals:**
- Changing the backtracking generation algorithm itself
- Persisting manual swaps to lineup history
- Mobile-optimized layout (desktop-first, same as current)
- Custom AI strategy changes

## Decisions

### 1. Re-ranking by total UTR proximity

**Decision**: Sort all valid candidates descending by `40.5 - totalUtr` (ascending distance from cap), then apply strategy-specific secondary sort. Candidates with `totalUtr > 40.5` are already filtered out by the hard constraint.

**Rationale**: The cap is the dominant constraint. Maximizing within it uses roster strength most effectively. Strategy sort becomes a tiebreaker.

**Alternative considered**: Make UTR proximity the strategy itself (a new "maximize" preset). Rejected — too many strategy options; it's better as a universal primary sort.

### 2. Balanced strategy scoring (per-line evenness)

**Decision**: Score each candidate by sum of `|pair.combinedUtr - 10.125|` across D1–D4. Lower = more balanced. Use this as secondary sort for `balanced` strategy after UTR proximity sort.

**Rationale**: 40.5 / 4 = 10.125 is the ideal even split. Minimizing total deviation from this target directly implements "each line close to 10.1" requirement.

**Alternative considered**: Variance of pair UTRs (current implementation). Rejected — current variance approach doesn't account for the absolute target of ~10.1.

### 3. Position pin constraint

**Decision**: Add `pinPlayers: Map<String, String>` (playerId → "D1"|"D2"|"D3"|"D4") to `GenerateLineupRequest`. In `LineupGenerationService.generateCandidates`, post-filter candidates to keep only lineups where each pinned player appears in the specified position.

**Rationale**: Post-filtering is simpler than modifying backtracking; pin constraints are rare, so the filtering overhead is acceptable. Validation: a player in both `pinPlayers` and `excludePlayers` → 400; more than 2 players pinned to same position → 400.

**Alternative considered**: Pre-assign pinned players to positions before backtracking. More efficient but significantly more complex to implement correctly with the existing backtracking design.

### 4. Manual pair swap

**Decision**: New frontend-only operation. The swap UI operates on the selected lineup object in memory (the `lineups.value[activeIndex]` ref). After user picks two players from different positions, compute the swapped lineup locally, validate all hard constraints client-side, and update the reactive lineup ref. No backend API call needed — the swap is ephemeral.

**Rationale**: Swaps don't change which lineup is persisted (only the first was saved at generation time). A client-side operation avoids an extra API round-trip and simplifies the implementation. If user wants to save a swapped lineup, that's a future feature.

**Alternative considered**: `POST /api/lineups/{id}/swap` backend endpoint. Rejected — adds API surface and persistence complexity for what is essentially a display/preview operation.

### 5. All-at-once lineup display

**Decision**: Replace `LineupResultTabs.vue` with `LineupResultGrid.vue` — a CSS grid showing all lineups simultaneously. On large screens: 2 columns × up to 3 rows. On medium screens: 1 column.

**Rationale**: With at most 6 cards, all fitting on one scrollable page is simpler UX than navigating tabs. Each card is compact (4 rows for D1–D4 pairs).

### 6. Per-player UTR in LineupCard

**Decision**: Modify `LineupCard.vue` to show `player1Name (UTR) / player2Name (UTR)` format for each pair. Add a `showPlayerUtr` prop (default `true`) to allow callers to hide it if needed.

**Rationale**: Minimal change to existing component; prop keeps it backward-compatible.

### 7. PlayerConstraintSelector position-pin UX

**Decision**: Extend existing 3-state toggle (中立 → 必须上场 → 排除) to 6 states: 中立 → D1 → D2 → D3 → D4 → 排除 → 中立. The "必须上场" state is removed (superseded by positional pins). Emit payload gains `pinPlayers: Record<playerId, position>` field.

**Alternative considered**: Separate "pin position" dropdown per player instead of extending the toggle. More explicit but uses more screen space.

### 8. PlayerConstraintSelector sort order and verified badge

**Decision**: Sort player list: females first (gender "female" before "male"), then within each gender sort by UTR descending. Display a small "认证" badge (green, compact) next to the player name when `player.verified === true`. Sorting is computed in the component via `computed` from the `players` prop.

**Rationale**: Coaches typically field mixed doubles and need to assess female players first. UTR descending within gender groups lets them quickly see strongest players at top. Verified badge helps quickly identify players eligible for D4 (which requires verified doubles UTR).

**Alternative considered**: Sort by UTR only (ignoring gender). Rejected — tennis lineups are gender-aware and the female slots are specifically constrained.

## Risks / Trade-offs

- **Post-filtering position pins may yield 0 results**: If the pinned player's UTR is incompatible with the requested position (e.g., lowest-rated player pinned to D1), no valid lineups remain → 400 error with clear message. Mitigation: clear error message "无法生成满足位置约束的排阵".
- **Client-side swap validation complexity**: Replicating all 6 hard constraints in JavaScript means two places to maintain the same logic. Mitigation: keep swap validation minimal — only check UTR ordering (D1 ≥ D2 ≥ D3 ≥ D4 combined UTRs) since other constraints are preserved by definition of swapping within the same lineup.
- **6-state toggle UX learning curve**: More states than before. Mitigation: tooltip or label shows current state clearly.

## Open Questions

- Should swapped lineups be optionally saveable to history? (Deferred to future feature)
- Should there be a "reset to original" button in the swap panel? (Yes — simple to add, include in implementation)
