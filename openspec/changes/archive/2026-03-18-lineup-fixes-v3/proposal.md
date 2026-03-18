## Why

After the lineup-ux-v2 release, several bugs and UX gaps were found in production use: the backtracking algorithm doesn't maximize total UTR toward the 40.5 cap, the position-pin constraint allows only one player per position (not a full pair), the female-player constraint is being over-satisfied (choosing more than 2 when not required), and the player constraint UI needs clearer gender labels and a dropdown instead of a cycle-toggle. Additionally, after a manual swap the lineup ordering should auto-sort rather than show a violation error.

## What Changes

- **Bug: total UTR too low** — backtracking currently prunes too aggressively or doesn't explore high-UTR combinations; algorithm must maximize toward 40.5 cap
- **Bug: two-player D4 pin not working** — pinning two players to the same position (e.g., both Ye Hanshan and Wei Bill to D4) should produce a pair at that position, not just one; current post-filter doesn't handle intra-position pairing
- **Bug: too many females selected** — constraint "at least 2 females" is being misapplied; selection sometimes picks 3+ females when 2 suffice; must prefer the minimum needed
- **Feature: gender indicator in constraint list** — each player row in `PlayerConstraintSelector` must show a visible gender badge (M/F or icon)
- **Feature: dropdown constraint selector** — replace 6-state cycle toggle with a `<select>` dropdown offering: 中立 / 不上 / 一定上 / D1 / D2 / D3 / D4; "一定上" means the player must play but position is algorithm's choice
- **Feature: gender and UTR in lineup result cards** — each player name in `LineupCard` must also show gender badge alongside existing UTR display
- **Feature: auto-sort after swap** — after a manual swap that violates D1≥D2≥D3≥D4 ordering, instead of rejecting the swap, re-sort the four pairs by combinedUtr descending and reassign positions D1→D4

## Capabilities

### New Capabilities
- `lineup-constraint-dropdown`: Player constraint UI as dropdown (中立/不上/一定上/D1-D4) replacing cycle-toggle; "一定上" maps to include-without-position

### Modified Capabilities
- `lineup-generation`: Fix UTR maximization algorithm; fix two-player same-position pin; fix female over-selection (prefer exactly 2 unless more are pinned)
- `lineup-player-constraints`: Add gender badge per player row; switch from 6-state toggle to dropdown
- `lineup-multi-result`: Show gender badge in lineup result cards alongside UTR
- `lineup-manual-swap`: After swap, auto-sort pairs by combinedUtr descending and reassign D1–D4 instead of rejecting invalid orderings

## Impact

- **Backend**: `LineupGenerationService` — algorithm changes for UTR maximization, pair-level pin (two players same position), female selection
- **Frontend**: `PlayerConstraintSelector.vue` — replace button toggle with `<select>` dropdown; add gender badge
- **Frontend**: `LineupCard.vue` — add gender badge next to each player name
- **Frontend**: `LineupSwapPanel.vue` — change swap validation: instead of rejecting, auto-sort pairs and reassign positions
- **Tests**: Backend service tests, frontend component tests updated to match new behavior
