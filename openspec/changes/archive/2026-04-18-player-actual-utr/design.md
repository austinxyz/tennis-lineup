## Context

The Player model currently has a single `utr` field representing the official UTR system rating. Captains frequently observe that a player's actual doubles ability diverges from this number — players may have played few rated matches, or their skill has improved significantly since their last rating update. This change introduces `actualUtr` as a captain-owned, editable overlay that propagates into lineup ranking and opponent analysis while leaving all hard constraint logic unchanged.

## Goals / Non-Goals

**Goals:**
- Add `actualUtr` (Double, nullable) to the Player model with transparent fallback to `utr` when unset
- Allow editing `actualUtr` in the add-player form and the bulk UTR edit mode
- Use `actualUtr` for secondary ranking of lineup candidates (sort top 6 from a larger pool by actualUtr sum)
- Use `actualUtr` delta in per-line win probability calculations for opponent analysis
- Pass `actualUtr` context to AI prompts so the model reasons from captain-assessed skill levels

**Non-Goals:**
- `actualUtr` does NOT affect hard constraint validation (total UTR cap, ordering, gap — all still use `utr`)
- No batch import support for `actualUtr` in this change (future work)
- No display of `actualUtr` in the lineup card pair details beyond what the generation ranking implies

## Decisions

### Decision 1: Nullable field with fallback helper
`actualUtr` is stored as `Double` (nullable) on `Player`. A helper method `getEffectiveActualUtr()` returns `actualUtr != null ? actualUtr : utr`. All downstream consumers (lineup ranking, matchup probability, AI prompt) use this helper — they never access `actualUtr` directly. This avoids null-checks scattered across services.

**Alternative considered**: Store `actualUtr` as required (same as `utr`). Rejected — would require migrating all existing player records and adds friction to the common case where a player has no captain-assessed override.

### Decision 2: Expand lineup candidate pool to 100, re-rank by actualUtrSum
The current algorithm caps at 6 results. With `actualUtr`, a lineup with lower `totalUtr` (official) might be stronger in practice. The fix: expand the valid candidate pool to ~100 (or all valid lineups, whichever is smaller), then apply a secondary sort by `actualUtrSum` (sum of 8 players' effective actualUtr) descending, and return the top 6.

Primary sort (proximity to 40.5 cap by official `utr`) is preserved as the constraint-satisfaction pass. The 100-candidate expansion is applied after constraint validation.

**Alternative considered**: Replace the primary sort entirely with `actualUtrSum`. Rejected — hard constraints are based on official UTR and the tournament enforces them; we must surface constraint-valid lineups first.

### Decision 3: Inline `actualUtr` column in bulk edit mode
Bulk UTR edit already shows `utr` inline. We extend each row with a second editable input for `actualUtr` (optional, placeholder "默认同UTR"). Both are saved on the same `PUT /api/teams/{id}/players/{pid}` call. The save logic only sends a PUT for players where at least one of `utr` or `actualUtr` changed.

**Alternative considered**: Separate bulk edit mode for `actualUtr`. Rejected — two modes for closely related fields adds friction; showing both side-by-side is clearer.

### Decision 4: Win probability uses `getEffectiveActualUtr()` delta
The existing per-line probability formula is based on UTR delta between the two pairs. We switch the input from `player.utr` to `player.getEffectiveActualUtr()`. No change to the formula itself; only the input values change.

### Decision 5: Pair model exposes actualUtr fields; Lineup exposes actualUtrSum
`Pair.java` gains `player1ActualUtr` and `player2ActualUtr` (Double, nullable), populated from `player.getEffectiveActualUtr()` during generation. `Lineup.java` gains `actualUtrSum` (Double) as a READ_ONLY transient field (same pattern as `currentValid`) so it is serialized to the client but not written to `tennis-data.json`.

`LineupCard.vue` displays actualUtr per player in the pair row when it differs from official UTR (i.e. `actualUtr != null`), formatted as "实: X.XX" after the official UTR. The card header shows a second UTR figure: "实际 UTR: X.XX" next to "总 UTR: X.XX". When no player has an override, both values are identical and only "总 UTR" is shown to avoid clutter.

**Alternative considered**: Compute actualUtrSum on the frontend from pair fields. Rejected — requires the frontend to know which players are in each lineup and their actualUtr, adding coupling. Backend populates it once and the frontend renders directly.

### Decision 6: AI prompt injection
In the matchup AI prompt builder, after the existing player roster section, inject: "请主要参考实际UTR（actualUtr）进行分析。如果实际UTR与官方UTR不同，以实际UTR为准。" This is appended regardless of whether any player has a non-null `actualUtr` — even when all effective values equal `utr`, the instruction is harmless.

## Risks / Trade-offs

- **Data drift risk**: `actualUtr` is manually maintained; it can become stale after a player's official UTR updates significantly. → No mitigation in this change — captains own the value. Future change could add a "sync to official UTR" button.
- **Lineup ranking opacity**: Users may be confused why a lineup with lower official totalUtr ranks above one with higher totalUtr. → UI should display `actualUtr` values in player rows so captains can correlate.
- **100-candidate expansion performance**: Current algorithm is fast for small rosters (≤ 20 players). Expanding to 100 adds ~16x candidate tracking overhead but no algorithmic complexity increase. Should remain within 5-second budget. → Monitor with existing team sizes; no optimization needed now.

## Migration Plan

- `actualUtr` is added as a nullable JSON field; existing `tennis-data.json` records without the field deserialize with `actualUtr = null` automatically (Jackson ignores missing fields).
- No data migration script needed.
- Frontend: PlayerForm and bulk edit gracefully handle `null` actualUtr (show empty input).

## Open Questions

- Should `actualUtr` be shown in the player list column (alongside `utr`) outside of bulk edit mode? Not specified — leaving as display-only in bulk edit for now; can be surfaced in a follow-up.
