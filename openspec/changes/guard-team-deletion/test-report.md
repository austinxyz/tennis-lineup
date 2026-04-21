# Test Report — guard-team-deletion

**Date:** 2026-04-20
**Implementation mode:** Strict TDD (RED → GREEN per cycle)

---

## Backend

### Unit — `TeamServiceTest` (Mockito)

| # | Test | Status |
| - | --- | --- |
| added | `deleteTeam_whenTeamHasPlayers_throwsTeamNotEmptyException` | ✅ PASS |
| added | `deleteTeam_whenTeamHasOnlyLineups_throwsTeamNotEmptyException` | ✅ PASS |
| added | `deleteTeam_whenBothPlayersAndLineups_throwsTeamNotEmptyException` | ✅ PASS |
| existing | `shouldDeleteTeam` (empty-team happy path — regression kept green) | ✅ PASS |
| existing | all other 12 tests | ✅ PASS |

**Subtotal:** 16/16 PASS (13 prior + 3 new)

### Web — `TeamControllerTest` (@WebMvcTest + MockMvc)

| # | Test | Status |
| - | --- | --- |
| added | `deleteTeam_whenTeamNotEmpty_returns409WithPayload` (asserts code, message, details.playerCount, details.lineupCount) | ✅ PASS |
| existing | `shouldDeleteTeam` (204 happy path) | ✅ PASS |
| existing | 18 others | ✅ PASS |

**Subtotal:** 20/20 PASS (19 prior + 1 new)

### Backend full regression

```
mvn test (261 tests total): 261 PASS, 0 FAIL
```

Includes repository, services, controllers across the whole codebase.

---

## Frontend (Vitest + @vue/test-utils)

### `useTeams.test.js`

| # | Test | Status |
| - | --- | --- |
| added | `rejects with server message on 409 TEAM_NOT_EMPTY and leaves teams unchanged` | ✅ PASS (regression guard; no new prod code — existing error path already satisfied spec) |

**Subtotal:** 17/17 PASS (16 prior + 1 new)

### `TeamListPanel.test.js` — new `describe('delete button guard')` block

| # | Test | Status |
| - | --- | --- |
| added | `disables delete button when team has players` | ✅ PASS |
| added | `disables delete button when team has saved lineups` | ✅ PASS |
| added | `enables delete button when team is empty` | ✅ PASS |
| added | `treats missing lineups field (legacy data) as empty` | ✅ PASS |
| added | `shows alert when server rejects with 409 on enabled button click` | ✅ PASS |

**Subtotal:** 16/16 PASS (11 prior + 5 new)

### Frontend full regression

```
npx vitest run  →  26 files / 448 tests, all PASS
```

---

## E2E (Playwright + Chromium)

### New spec — `frontend/e2e/tests/team-delete-guard.spec.js`

| # | Test | Status | Duration |
| - | --- | --- | --- |
| added | `删除队伍保护：非空队伍不可删除 › 有球员时删除按钮 disabled，清空后可删除` | ✅ PASS | 1.1 s |

Flow:
1. Create team → add player
2. Assert sidebar delete button `toBeDisabled()` + `title` contains 「请先移除球员」
3. Delete player
4. Assert button `toBeEnabled()`
5. Click delete → team row detaches from DOM, not in `getTeamNames()`

### E2E full regression

```
npx playwright test (all specs): 54/54 PASS, 47.1 s
```

No regressions in 队伍管理 / 球员管理 / 队伍切换 / 批量导入 / 排阵 / 对手分析 / 搭档笔记 etc.

---

## Manual backend smoke (curl)

```
POST /api/teams                          → 200  (new team team-209751910177200)
POST /api/teams/{id}/players             → 200  (player-209757767979100 SmokeAlice)
DELETE /api/teams/{id}                   → 409  {"code":"TEAM_NOT_EMPTY","message":"队伍中还有球员或已保存的排阵，无法删除","details":{"lineupCount":0,"playerCount":1}}
DELETE /api/teams/{id}/players/{pid}     → 204
DELETE /api/teams/{id}                   → 204
```

---

## Totals

| Layer | Added | Total | Pass |
| --- | ---: | ---: | ---: |
| Backend unit & web | 4 | 261 | 261 |
| Frontend unit | 6 | 448 | 448 |
| E2E | 1 | 54 | 54 |
| **Combined** | **11** | **763** | **763** |

0 failures, 0 skips.

---

## TDD discipline notes

- **Cycles executed with observed RED → GREEN:**
  - Backend service guard (3 new service tests failed with `Expected TeamNotEmptyException to be thrown, but nothing was thrown` before implementation)
  - Backend controller 409 mapping (controller test failed with `Status expected:<409> but was:<500>` before the handler was added)
  - Frontend TeamListPanel disabled button (3/5 new tests failed before `:disabled`/`:title`/try-catch were added)
- **No-new-code cases (regression-only tests added, PASS on first run):**
  - `useTeams.test.js` 409 test — existing `useApi.js` already propagates server message; test locks in behavior
  - `TeamList.vue` pattern mirror — unrouted dead code, no tests added (covered by `TeamListPanel.test.js` for the live component)
- **Coverage delta:** not computed here (no JaCoCo/V8 coverage runs executed); can be added with `mvn verify` + `npx vitest run --coverage` if required.

---

## Outstanding

- git commit & push
- `openspec archive guard-team-deletion` — will merge the two delta spec files into `openspec/specs/team-crud/spec.md` and `openspec/specs/lineup-multi-result/spec.md`
- Post-deploy smoke test against `https://tennis-lineup.fly.dev` for the 409 path
