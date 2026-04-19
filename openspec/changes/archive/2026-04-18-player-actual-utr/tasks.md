## 1. Backend — Player Model & API

- [x] 1.1 Add `actualUtr` (Double, nullable) field to `Player.java` model
- [x] 1.2 Add `getEffectiveActualUtr()` helper to `Player.java` that returns `actualUtr != null ? actualUtr : utr`
- [x] 1.3 Update `PlayerRequest` (in `TeamController.java`) to accept `actualUtr` field
- [x] 1.4 Update `PlayerService.addPlayer()` to accept and store `actualUtr`
- [x] 1.5 Update `PlayerService.updatePlayer()` to accept and store `actualUtr`
- [x] 1.6 Add validation in `PlayerService`: if `actualUtr` is non-null, must be in range [0.0, 16.0], error message "实际UTR必须在0.0到16.0之间"
- [x] 1.7 Verify existing `tennis-data.json` players without `actualUtr` deserialize correctly (Jackson ignores missing fields — no migration needed)

## 2. Backend — Lineup Generation Re-ranking & Pair Model

- [x] 2.1 Add `player1ActualUtr` and `player2ActualUtr` (Double, nullable) fields to `Pair.java`
- [x] 2.2 Add `actualUtrSum` (Double, READ_ONLY transient) field to `Lineup.java` — same pattern as `currentValid`; serialized to client, not persisted
- [x] 2.3 Populate `pair.player1ActualUtr` / `pair.player2ActualUtr` from `player.getEffectiveActualUtr()` during lineup generation
- [x] 2.4 Compute and set `lineup.actualUtrSum` = sum of all 8 players' effective actualUtr after each candidate is assembled
- [x] 2.5 Expand valid candidate collection in `LineupGenerationService` from stop-at-6 to collect up to 100 candidates
- [x] 2.6 After collecting candidates, sort by `actualUtrSum` descending
- [x] 2.7 Return top 6 from the re-ranked list
- [x] 2.8 Confirm all hard constraint checks (`totalUtr` cap, ordering, gap) still use official `utr` — no changes needed there

## 3. Backend — Opponent Analysis Win Probability

- [x] 3.1 Update per-line win probability calculation in `MatchupService` (or wherever UTR delta is computed) to use `player.getEffectiveActualUtr()` instead of `player.getUtr()`
- [x] 3.2 Update AI prompt builder: append instruction "请主要参考实际UTR（actualUtr）进行分析。如果实际UTR与官方UTR不同，以实际UTR为准。同时结合个人备注和搭档笔记进行综合判断。"
- [x] 3.3 Include `actualUtr` values in player roster data sent to AI prompt

## 4. Backend — Tests

- [x] 4.1 Update `PlayerServiceTest`: add tests for `addPlayer` with `actualUtr` set and null; test range validation (below 0, above 16, valid boundary)
- [x] 4.2 Update `TeamControllerTest`: add test for POST/PUT player with `actualUtr` field in request/response
- [x] 4.3 Add unit test for `getEffectiveActualUtr()`: null case returns `utr`, non-null case returns `actualUtr`
- [x] 4.4 Add `LineupGenerationService` test: verify 100-candidate collection + actualUtrSum re-ranking (lineup with lower totalUtr but higher actualUtrSum ranks first)
- [x] 4.5 Add `MatchupService` test: verify per-line probability uses effective actualUtr; null actualUtr falls back to utr

## 5. Frontend — PlayerForm Component

- [x] 5.1 Add optional `actualUtr` number input to `PlayerForm.vue` below the `utr` field, placeholder "默认同UTR（选填）", step=0.01, min=0, max=16
- [x] 5.2 Pre-fill `actualUtr` input from `initialData.actualUtr` when editing an existing player
- [x] 5.3 Include `actualUtr` (null when empty) in the `submit` event payload
- [x] 5.4 Update `usePlayers.js` composable: pass `actualUtr` in `addPlayer` and `updatePlayer` API calls

## 6. Frontend — Bulk UTR Edit Mode

- [x] 6.1 Extend bulk edit rows in `TeamDetail.vue` to show a second editable input for `actualUtr` alongside `utr`
- [x] 6.2 Add column header "实际UTR" next to "UTR" in bulk edit mode
- [x] 6.3 Blank `actualUtr` input sends `null` (not 0) in the PUT request
- [x] 6.4 Change detection: trigger PUT only when `utr` OR `actualUtr` changed compared to original values

## 7. Frontend — LineupCard Display

- [x] 7.1 In `LineupCard.vue` pair rows: after the official UTR of each player, show "实: X.XX" for `player1ActualUtr` / `player2ActualUtr` when the value is non-null (i.e. when actualUtr differs from official utr)
- [x] 7.2 In `LineupCard.vue` header: show "实际 UTR: X.XX" next to "总 UTR: X.XX" when `lineup.actualUtrSum` is non-null and differs from `lineup.totalUtr`
- [x] 7.3 When all players have `actualUtr: null` (actualUtrSum equals totalUtr), hide the "实际 UTR" header element entirely

## 8. Frontend — Tests

- [x] 8.1 Update `PlayerForm.test.js`: test actualUtr input renders, pre-fills from initialData, submits null when empty, submits value when filled
- [x] 8.2 Update `usePlayers.test.js`: verify `addPlayer` and `updatePlayer` include `actualUtr` in request payload
- [x] 8.3 Update `TeamDetail.test.js`: test bulk edit mode shows actualUtr column; blank input sends null
- [x] 8.4 Update `LineupCard.test.js`: test actualUtr shown per player when non-null; actualUtrSum shown in header when differs from totalUtr; both hidden when all null

## 9. E2E Tests

- [x] 9.1 Add E2E scenario: add player with actualUtr set → verify displayed and saved
- [x] 9.2 Add E2E scenario: bulk edit actualUtr for existing player → verify saved and reflected in player list
- [x] 9.3 Add E2E scenario: clear actualUtr in bulk edit → verify sent as null
- [x] 9.4 Add E2E scenario: generate lineup with players having actualUtr set → verify card shows "实: X.XX" per player and "实际 UTR" sum in header

## 10. Backend Restart & Verification

- [x] 10.1 Restart backend (`JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 mvn spring-boot:run`)
- [x] 10.2 Run full backend test suite: `JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 mvn test` — all tests pass
- [x] 10.3 Run frontend unit tests: `npm test` — all tests pass
- [x] 10.4 Run E2E tests: `npm run test:e2e` — all tests pass
- [ ] 10.5 Manual smoke test: add player with actualUtr, generate lineup, verify card shows actualUtr per player and actualUtrSum in header
