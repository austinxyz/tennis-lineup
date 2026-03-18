# Test Report: basic-lineup-generation

**Date**: 2026-03-18

## Summary

| Suite | Tests | Pass | Fail |
|-------|-------|------|------|
| Backend unit (JUnit) | 123 | 123 | 0 |
| Frontend unit (Vitest) | 172 | 172 | 0 |
| E2E (Playwright/Chromium) | 14 | 14 | 0 |
| **Total** | **309** | **309** | **0** |

## Backend Unit Tests (JUnit 5)

```
ConstraintServiceTest        12 tests  PASS
LineupGenerationServiceTest  11 tests  PASS
LineupServiceTest             8 tests  PASS
LineupControllerTest          9 tests  PASS
ZhipuAiServiceTest            7 tests  PASS
TeamControllerTest           17 tests  PASS
BatchImportServiceTest       15 tests  PASS
PlayerServiceTest            20 tests  PASS
TeamServiceTest              13 tests  PASS
JsonRepositoryTest           11 tests  PASS
─────────────────────────────────────────────
Total                       123 tests  BUILD SUCCESS
```

## Frontend Unit Tests (Vitest)

```
ConstraintService.test.js       → N/A (backend only)
useLineup.test.js              12 tests  PASS
StrategySelector.test.js        6 tests  PASS
LineupCard.test.js              6 tests  PASS
LineupGenerator.test.js        10 tests  PASS
(+ existing suites)           138 tests  PASS
─────────────────────────────────────────────
Total                         172 tests  PASS
```

## E2E Tests (Playwright — Chromium)

Both backend (Spring Boot) and frontend (Vite dev server) were running during E2E execution.

```
排阵生成 › 导航到 /lineup 显示排阵生成页面          PASS
排阵生成 › 侧边栏显示排阵生成导航入口              PASS
排阵生成 › 选择队伍后生成按钮启用                  PASS
排阵生成 › 使用均衡策略生成排阵成功                PASS
排阵生成 › 使用集中火力策略生成排阵成功            PASS
排阵生成 › 生成的排阵卡片显示总 UTR               PASS
(+ 8 existing E2E tests)                         PASS
─────────────────────────────────────────────
Total  14 tests  PASS  (7.9s)
```

## Issues Found & Fixed During Testing

1. **E2E strict mode violation** — `locator('text=排阵生成')` matched both nav link and page `<h2>`. Fixed with `getByRole('link', { name: '排阵生成' })`.
2. **E2E test data exceeded UTR cap** — test players totalled 50.0 UTR (cap: 40.5), causing backend to generate 0 valid candidates. Fixed by lowering player UTRs to total 37.0.
3. **Backend not restarted after code changes** — `mvn spring-boot:run` reused old `.class` files. Fixed by running `mvn clean spring-boot:run`.
4. **Vitest picking up Playwright files** — `e2e/**` not excluded from Vitest. Fixed by adding `exclude: ['e2e/**']` to `vite.config.js`.
