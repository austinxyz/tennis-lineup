## Context

The lineup-ux-v2 implementation introduced the lineup generation UI but several bugs remain in production. The core backtracking algorithm in `LineupGenerationService` has a structural flaw: it always anchors on the first unused player, so for rosters larger than 8 players it never explores lineups that omit the first player. This explains why total UTR appears lower than expected — the selection isn't maximizing. Additionally, pin constraints are checked per-player independently, so pinning two players to D4 doesn't guarantee they're paired together. The female constraint requires "at least 2" but doesn't prevent over-selection. Frontend UX needs a dropdown instead of cycle-toggle, gender labels, and auto-sort on swap.

## Goals / Non-Goals

**Goals:**
- Fix UTR maximization: when roster > 8 players, explore all valid 8-player subsets and prefer those closest to the 40.5 cap
- Fix two-player same-position pin: players pinned to the same position must form a pair there
- Fix female selection: prefer exactly 2 females in selection unless constraints force more; never pick an extra female when enough males are available
- Add gender badge (M/F) to PlayerConstraintSelector rows and LineupCard player entries
- Replace cycle-toggle with `<select>` dropdown offering: 中立 / 不上 / 一定上 / D1 / D2 / D3 / D4
- Restore `includePlayers` (一定上) distinction from `pinPlayers` (D1-D4)
- Add `player1Gender` / `player2Gender` fields to `Pair` model for frontend rendering
- After manual swap in LineupSwapPanel, auto-sort pairs by combinedUtr and reassign D1–D4 instead of rejecting

**Non-Goals:**
- Changing the hard constraint rules (≤40.5 cap, D4 verified, partner gap ≤3.5)
- Adding new API endpoints
- AI strategy changes

## Decisions

### Decision 1: Fix algorithm — pre-select 8 players before backtracking

**Problem**: Current backtrack always anchors on the first unused player, so for a 12-player roster it always uses player[0]. This means it never explores lineups that exclude the top players (even when those top players push totalUtr over cap).

**Fix**: Add a pre-selection phase. Enumerate all C(n, 8) subsets of 8 players from the eligible roster. For each subset:
- Check basic feasibility: sum of UTRs ≤ 40.5, subset has ≥ 2 females
- If feasible: run backtracking on that 8-player subset to find valid pairings

**Ordering**: Process subsets in descending totalUtr order (closest to 40.5 first). Stop after collecting enough candidates (e.g., 6). This limits computation: for a 12-player roster, C(12,8) = 495 subsets — fast.

**Alternative considered**: Modify backtrack to optionally skip anchor players. Rejected — more complex and harder to reason about completeness.

### Decision 2: Fix same-position pin — pair-level grouping

**Problem**: Current post-filter checks each pinned player independently. Pinning player A to D4 AND player B to D4 matches any lineup where A is in D4 (even paired with C) and B is in D4 (even paired with D, which is impossible since D4 only has one pair).

**Fix**: Group pinned players by target position. If exactly 2 players are pinned to the same position, require they form that specific pair. If 1 player is pinned, require only that player appears at that position. If >2 players pin to same position → validation error "不能将超过2名球员固定到同一位置".

### Decision 3: Female selection preference

**Problem**: When more than 2 females are available and no female-specific constraints are set, the algorithm may generate lineups with 3 or 4 females (all are valid per the ≥2 rule).

**Fix**: In the pre-selection phase, when choosing 8-player subsets, prefer subsets with exactly 2 females. Score subsets: `(femaleCount == 2 ? 0 : femaleCount) * penalty`. Only include 3+ female subsets if no 2-female subset exists that satisfies other constraints (e.g., all females are pinned to play).

**Why**: The user said "只需要2个女生，多的也可以，但是不能少" — minimize females unless forced.

### Decision 4: Restore `includePlayers` alongside `pinPlayers`

The lineup-ux-v2 merged "must play" into D1-D4 pins, removing the general "must play" option. The user needs "一定上" (must play, any position) as distinct from D1-D4 pins.

**Fix**: Restore `includePlayers: string[]` in the generate request and frontend. The dropdown maps to:
- 不上 → excludePlayers
- 一定上 → includePlayers
- D1/D2/D3/D4 → pinPlayers (also implicitly in includePlayers)

Backend already supports the `include` parameter in `generateCandidates`. Just re-wire the frontend.

### Decision 5: Gender in Pair model

Add `player1Gender` and `player2Gender` fields to `Pair.java` (populated in `buildLineup`). These let `LineupCard` show gender badges without additional API calls.

**Why not derive from player data on frontend**: The frontend receives only the lineup JSON; it doesn't have player gender data at render time without a separate lookup.

### Decision 6: Auto-sort after swap

When user swaps two players in LineupSwapPanel, instead of checking D1≥D2≥D3≥D4 and rejecting, recompute `combinedUtr` for all 4 pairs and sort descending, then reassign positions D1→D4.

**Tradeoff**: The user loses their explicit D-position expectations — the swap changes not just the two swapped players but potentially all position labels. This is acceptable because the user can see the new ordering immediately.

**Why not validate and warn**: The user explicitly requested this behavior (requirement 7).

## Risks / Trade-offs

- [Risk] C(n,8) pre-selection with large rosters (n=20+): C(20,8) = 125,970 subsets. **Mitigation**: Sort players by UTR descending before enumeration; prune subsets early if partial sum already exceeds 40.5 or remaining players can't bring total high enough. Also cap at n=20 players in warning.
- [Risk] Restoring `includePlayers` alongside `pinPlayers` re-complicates the API contract. **Mitigation**: `pinPlayers` keys are implicitly also "must include"; backend adds them to the include set automatically before backtracking.
- [Risk] Auto-sort on swap may surprise users who expected their specific swap to stick. **Mitigation**: Show the new position assignments clearly after sort; behavior is documented.

## Migration Plan

1. Backend: update `Pair` model, fix pre-selection in `LineupGenerationService`, fix pin grouping
2. Backend: update tests
3. Frontend: add gender fields to `PlayerConstraintSelector` and `LineupCard`
4. Frontend: replace toggle with `<select>` in `PlayerConstraintSelector`
5. Frontend: update `LineupSwapPanel` for auto-sort
6. Frontend: restore `includePlayers` in `useLineup.js` and `LineupGenerator.vue`
7. Frontend: update all affected tests

No data migration needed (lineups are not persisted between sessions in current impl).
