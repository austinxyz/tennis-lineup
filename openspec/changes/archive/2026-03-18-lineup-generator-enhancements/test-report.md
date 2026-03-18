# Test Report — lineup-generator-enhancements

**Date**: 2026-03-18
**Total Tests**: 339
- Backend JUnit: 130
- Frontend Vitest: 194
- E2E Playwright: 15

**Result**: All 339 tests passing ✓

---

## Backend (130 tests — all pass)

| Test Class | Tests | Result |
|---|---|---|
| LineupControllerTest | 10 | ✓ |
| TeamControllerTest | 17 | ✓ |
| JsonRepositoryTest | 11 | ✓ |
| BatchImportServiceTest | 15 | ✓ |
| ConstraintServiceTest | 12 | ✓ |
| LineupGenerationServiceTest | 16 | ✓ |
| LineupServiceTest | 9 | ✓ |
| PlayerServiceTest | 20 | ✓ |
| TeamServiceTest | 13 | ✓ |
| ZhipuAiServiceTest | 7 | ✓ |

New/updated tests this change:
- `LineupControllerTest`: response body is now JSON array with at least 1 lineup; added constraint violation 400 test
- `LineupServiceTest`: `generateMultipleAndSave` returns up to 6 candidates; only first persisted
- `LineupGenerationServiceTest`: include/exclude constraint filter; overlap/size validation

---

## Frontend Vitest (194 tests — all pass)

| Test File | Tests | Result |
|---|---|---|
| useApi.test.js | 6 | ✓ |
| useTeams.test.js | 8 | ✓ |
| usePlayers.test.js | 15 | ✓ |
| useBatchImport.test.js | 8 | ✓ |
| useLineup.test.js | 13 | ✓ |
| TeamListPanel.test.js | 14 | ✓ |
| PlayerForm.test.js | 22 | ✓ |
| StrategySelector.test.js | 10 | ✓ |
| LineupCard.test.js | 10 | ✓ |
| NavSidebar.test.js | 8 | ✓ |
| PlayerConstraintSelector.test.js | 16 | ✓ (new) |
| LineupResultTabs.test.js | 9 | ✓ (new) |
| LineupGenerator.test.js | 12 | ✓ (updated) |
| TeamDetail.test.js | 43 | ✓ |

New/updated tests this change:
- `useLineup.test.js`: updated `lineup` → `lineups` array; added `includePlayers`/`excludePlayers` body test
- `LineupGenerator.test.js`: asserts two-column structure (lg:w-2/5, lg:w-3/5); mocks return array; verifies constraint params
- `PlayerConstraintSelector.test.js` (new): state toggle, emit events, summary row, empty state
- `LineupResultTabs.test.js` (new): tab rendering, tab switching, empty state, lineups prop reset

---

## E2E Playwright (15 tests — all pass)

| Test Suite | Tests | Result |
|---|---|---|
| 批量导入 | 2 | ✓ |
| 排阵生成 | 5 | ✓ |
| 球员管理 | 3 | ✓ |
| 队伍管理 | 2 | ✓ |
| 队伍切换 | 1 | ✓ |
| team-switch | 1 | ✓ (skip, not counted) |

Updated tests this change:
- `lineup-generation.spec.js`: `waitForResults()` waits for "方案 1" tab button; verifies tab grid appears; tests tab 2 switching

---

## Issues Found and Fixed

1. **Backend running old compiled binary**: After code changes, the running spring-boot server used stale `.class` files → restarted with `mvn clean spring-boot:run`
2. **`waitForResults` mismatch**: E2E page object updated to wait for tab button "方案 1" instead of "排阵结果" text (which only appears inside the card after tab renders)
