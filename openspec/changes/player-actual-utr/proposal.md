## Why

UTR system ratings don't always reflect a player's actual doubles performance — captains observe real skill levels through practice and competition. Without a way to record captain-assessed UTR, lineup generation and opponent analysis are forced to rely solely on potentially inaccurate official ratings, leading to suboptimal lineup decisions.

## What Changes

- **New field `actualUtr`** on the Player model: optional float, defaults to `utr` when not set
- **Player add form** gains an optional `actualUtr` input field alongside `utr`
- **Bulk UTR edit mode** extends to show and edit both `utr` and `actualUtr` per player
- **Lineup generation** expands candidate pool to ~100 valid lineups (still constrained by `utr`-based hard constraints), then re-ranks by `actualUtr` sum descending before returning top 6
- **Lineup card display** shows each player's `actualUtr` (in addition to official UTR) per pair row, and shows `actualUtrSum` in the card header alongside `totalUtr`
- **Opponent analysis win probability** uses `actualUtr` difference instead of `utr` difference for per-line probability calculation
- **AI opponent analysis prompt** explicitly instructs the model to primarily reference `actualUtr`, alongside personal notes and partner notes

## Capabilities

### New Capabilities
- `player-actual-utr`: Captain-assessed actual UTR field — data model, persistence, API contract, and UI editing (add form + bulk edit)

### Modified Capabilities
- `player-bulk-utr-edit`: Bulk edit mode extends to show/edit `actualUtr` alongside `utr` in the same inline row
- `lineup-generation`: Candidate pool expanded to ~100 valid lineups; final 6 ranked by `actualUtr` sum descending (hard constraints remain `utr`-based)
- `opponent-analysis`: Per-line win probability uses `actualUtr` delta; AI prompt instructs model to prioritize `actualUtr` when reasoning

## Impact

- **Backend model**: `Player.java` gains `actualUtr: Double` (nullable, serialized as JSON)
- **PlayerService**: `addPlayer` / `updatePlayer` accept `actualUtr`; getter returns `utr` value when `actualUtr` is null
- **Pair model**: gains `player1ActualUtr` and `player2ActualUtr` (Double, nullable) — populated during generation from `getEffectiveActualUtr()`; serialized in lineup response
- **Lineup model**: gains `actualUtrSum` (Double, READ_ONLY / transient) — sum of all 8 players' effective actualUtr; serialized to client but not persisted
- **LineupGenerationService**: Expand backtracking to produce up to 100 valid candidates; add secondary sort by `actualUtrSum` descending; populate Pair actualUtr fields and Lineup.actualUtrSum before returning
- **MatchupService / UTR win probability**: Switch per-line probability input from `utr` delta to `actualUtr` delta
- **AI prompt builder**: Add instruction "请主要参考实际UTR（actualUtr）进行分析，同时结合个人备注和搭档笔记"
- **Frontend PlayerForm.vue**: Add optional `actualUtr` input (number, step=0.01, placeholder="默认同UTR")
- **Frontend TeamDetail.vue / bulk UTR edit**: Add `actualUtr` column in bulk edit mode
- **Frontend LineupCard.vue**: Each pair row shows actualUtr per player (e.g. "实: 7.0") alongside official UTR; header shows `实际 UTR: <actualUtrSum>` next to `总 UTR`; actualUtr display is omitted when it equals official UTR (i.e. not set)
- **API**: `POST /api/teams/{id}/players` and `PUT /api/teams/{id}/players/{pid}` accept `actualUtr` in request body
- **No breaking changes**: `actualUtr` is optional everywhere; existing data remains valid
