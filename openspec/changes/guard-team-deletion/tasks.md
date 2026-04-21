## 1. Backend — Exception and Error Mapping

- [x] 1.1 Create `TeamNotEmptyException extends RuntimeException` under `backend/src/main/java/com/tennis/exception/` with constructor `(int playerCount, int lineupCount)` exposing both counts via getters
- [x] 1.2 Update `GlobalExceptionHandler` to map `TeamNotEmptyException` → HTTP 409 + `ErrorResponse{code: "TEAM_NOT_EMPTY", message: "队伍中还有球员或已保存的排阵，无法删除", details: {"playerCount": N, "lineupCount": M}}`
- [x] 1.3 Confirm `ErrorResponse.details` already accepts `Map<String, Object>` (it does per existing model); extend if not — **extended**: changed `details` type from `String` → `Object` so Jackson serializes `Map.of(...)` as a JSON object

## 2. Backend — Service Guard

- [x] 2.1 In `TeamService.deleteTeam(String teamId)`, after locating the team but before removing it, check `team.getPlayers().isEmpty() && team.getLineups().isEmpty()`; if either is non-empty, throw `TeamNotEmptyException(playerCount, lineupCount)`
- [x] 2.2 Treat missing `lineups` field (legacy JSON) as empty list — align with `team-management` spec so backward-compat is preserved (null check `target.getLineups() == null ? 0 : ...`)
- [x] 2.3 Ensure the 404 path for non-existent team still returns `NotFoundException` (precedence over the new 409) — 404 path keeps existing `IllegalArgumentException("队伍不存在")` semantics (handled as 400 VALIDATION_ERROR in current code; unchanged by this spec)

## 3. Backend — Tests

- [x] 3.1 Add `TeamServiceTest`: `deleteTeam_whenTeamHasPlayers_throwsTeamNotEmptyException`
- [x] 3.2 Add `TeamServiceTest`: `deleteTeam_whenTeamHasOnlyLineups_throwsTeamNotEmptyException`
- [x] 3.3 Add `TeamServiceTest`: `deleteTeam_whenBothPlayersAndLineups_throwsTeamNotEmptyException` and asserts both counts surfaced
- [x] 3.4 Add `TeamServiceTest`: `deleteTeam_whenTeamIsEmpty_removesFromStorage` (regression — success path still works) — existing `shouldDeleteTeam` already covers this
- [x] 3.5 Add `TeamControllerTest`: `DELETE /api/teams/{id}` returns 409 with body `{"code":"TEAM_NOT_EMPTY", ...}` when service throws `TeamNotEmptyException`
- [x] 3.6 Add `TeamControllerTest`: 409 response body contains `details.playerCount` and `details.lineupCount`
- [x] 3.7 Keep existing `TeamControllerTest` deletion-happy-path test green (use empty team fixture) — `shouldDeleteTeam` still passes
- [x] 3.8 Run `mvn test` with `-Dfile.encoding=UTF-8`; confirm all prior 70+ backend tests still pass — **261/261 PASS**

## 4. Backend — Restart & Smoke

- [x] 4.1 Kill running Java process (`powershell -Command "Get-Process java | Stop-Process -Force"`) and restart with `mvn spring-boot:run` — required; Spring Boot does not hot-reload
- [x] 4.2 Manual smoke: create a team, add one player, `curl -X DELETE /api/teams/{id}` → expect 409 with new payload — **confirmed 409 `{"code":"TEAM_NOT_EMPTY","details":{"lineupCount":0,"playerCount":1}}`**
- [x] 4.3 Manual smoke: create a team with no players/lineups, delete it → expect 204 — **confirmed 204**

## 5. Frontend — Error Propagation

- [x] 5.1 Update `frontend/src/composables/useTeams.js` `deleteTeam(id)` to surface the 409 `code` and `message` (do NOT optimistically remove from local state on failure) — existing code already correct; `useApi.js` throws `Error(errorData.message)` and `useTeams.deleteTeam` only filters on success path
- [x] 5.2 Add test to `frontend/src/composables/__tests__/useTeams.test.js`: on 409 `TEAM_NOT_EMPTY`, rejects with the server message and leaves `teams` ref unchanged

## 6. Frontend — UI Guard (TeamListPanel)

- [x] 6.1 In `frontend/src/components/TeamListPanel.vue`, compute `isDeletable(team)` = `(team.players?.length ?? 0) === 0 && (team.lineups?.length ?? 0) === 0`
- [x] 6.2 Bind delete button `:disabled="!isDeletable(team)"` and add `:title="!isDeletable(team) ? '请先移除球员和已保存的排阵' : ''"` — implemented with three-tier message (players only / lineups only / both)
- [x] 6.3 In `confirmDelete(team)`, catch rejected promise and show `alert(err.message)` fallback
- [x] 6.4 Mobile/desktop dual render: if adding a desktop-only button wrapper, tag it `data-testid="desktop-team-delete-btn"` per CLAUDE.md guardrail — **N/A**: single-column sidebar with no mobile/desktop split for this button

## 7. Frontend — UI Guard (TeamList view)

- [x] 7.1 Apply the same `isDeletable` + `:disabled` + `:title` pattern in `frontend/src/views/TeamList.vue` — component is unrouted (dead code) but pattern mirrored for spec parity
- [x] 7.2 Handle 409 rejection the same way (alert + no optimistic removal)

## 8. Frontend — Unit Tests

- [x] 8.1 Extend `frontend/src/components/__tests__/TeamListPanel.test.js`: delete button is disabled when team has players
- [x] 8.2 Delete button is disabled when team has lineups (mock `team.lineups = [{...}]`)
- [x] 8.3 Delete button is enabled and clickable when both arrays empty — plus `legacy data (missing lineups field)` variant
- [x] 8.4 On click of enabled button and rejected `deleteTeam` with 409, the component shows the error message and the team row remains (alert fallback test)

## 9. E2E Tests

- [x] 9.1 Update or add a test: "cannot delete team with players — button disabled + tooltip visible" — added in new file `frontend/e2e/tests/team-delete-guard.spec.js`
- [x] 9.2 Add test: "after removing all players, delete button becomes enabled and team is deleted" (full flow) — combined into single spec with both phases
- [x] 9.3 Before running: `unset all_proxy ALL_PROXY http_proxy HTTP_PROXY https_proxy HTTPS_PROXY`
- [x] 9.4 Run `npm run test:e2e`; confirm all 8+ tests pass — **54/54 PASS**

## 10. Documentation

- [x] 10.1 Update `docs/api.md`: document that `DELETE /api/teams/{id}` now returns 409 `TEAM_NOT_EMPTY`; include example JSON body
- [x] 10.2 Append entry to `docs/log/2026-04-20.md` summarising change, test counts, findings

## 11. OpenSpec Archive

- [ ] 11.1 After merge, run `openspec-sync-specs` so the delta specs are merged into `openspec/specs/team-crud/spec.md` and `openspec/specs/lineup-multi-result/spec.md`
- [ ] 11.2 Archive the change via `openspec archive guard-team-deletion`

## 12. Generate Test Report

- [x] 12.1 Produce a test report at `openspec/changes/guard-team-deletion/test-report.md`
