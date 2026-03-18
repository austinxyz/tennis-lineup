## 1. 后端数据模型

- [x] 1.1 新增 `Pair.java` 模型类（position, points, player1Id, player1Name, player2Id, player2Name, combinedUtr）
- [x] 1.2 新增 `Lineup.java` 模型类（id, createdAt, strategy, aiUsed, pairs, totalUtr, valid, violationMessages）
- [x] 1.3 扩展 `Team.java`：添加 `lineups: List<Lineup>` 字段，Jackson 注解设默认空列表
- [x] 1.4 新增 `GenerateLineupRequest.java` DTO（teamId, strategyType, preset, naturalLanguage）
- [x] 1.5 添加智谱 AI SDK Maven 依赖（`cn.bigmodel.openapi:oapi-java-sdk:4.0.0`）

## 2. 后端约束服务

- [x] 2.1 新增 `ConstraintService.java`：实现 `validateLineup(lineup, team)` 返回 `ValidationResult`
- [x] 2.2 实现 6 项硬约束检查：UTR 排序、总 UTR ≤ 40.5、女性 ≥ 2、搭档差 ≤ 3.5、D4 verified、球员唯一性
- [x] 2.3 新增 `ConstraintServiceTest.java`：覆盖每项约束的通过/失败场景（共 ~12 tests）

## 3. 后端排阵生成服务

- [x] 3.1 新增 `LineupGenerationService.java`：回溯算法生成所有合法配对组合（搭档差剪枝）
- [x] 3.2 实现位置分配逻辑：按组合 UTR 降序分配 D1–D4
- [x] 3.3 实现 Fallback 启发式选优：`balanced`（最小方差）、`aggressive`（最大 D1+D2+D3）
- [x] 3.4 新增 `LineupGenerationServiceTest.java`：覆盖生成算法、位置分配、约束过滤（共 ~10 tests）

## 4. 后端 AI 服务

- [x] 4.1 新增 `ZhipuAiService.java`：配置 `ZhipuAiClient`，实现 `selectBestLineup(candidates, strategy)`
- [x] 4.2 实现 Prompt 构建逻辑（候选排阵列表 + 策略描述 → 返回编号）
- [x] 4.3 实现超时（3s）和异常处理，触发 Fallback 并返回 `aiUsed: false`
- [x] 4.4 配置 `application.yml`：添加 `zhipu.api.key: ${ZHIPU_API_KEY}`
- [x] 4.5 新增 `ZhipuAiServiceTest.java`：Mock AI 调用，覆盖正常/超时/解析异常场景（共 ~6 tests）

## 5. 后端排阵历史服务与 Controller

- [x] 5.1 新增 `LineupService.java`：`saveLineup`、`getLineupsByTeam`（倒序）、`deleteLineup`
- [x] 5.2 新增 `LineupController.java`：`POST /api/lineups/generate`、`GET /api/teams/{id}/lineups`、`DELETE /api/lineups/{id}`
- [x] 5.3 新增 `LineupServiceTest.java`：覆盖保存、查询（倒序）、删除、不存在异常（共 ~8 tests）
- [x] 5.4 新增 `LineupControllerTest.java`：覆盖 3 个端点的正常/错误场景（共 ~10 tests）
- [x] 5.5 更新 `docs/api.md`：添加排阵相关 API 文档

## 6. 前端 Composable

- [x] 6.1 新增 `useLineup.js`：封装 `generateLineup`、`fetchLineupHistory`、`deleteLineup`，管理 loading/error 状态
- [x] 6.2 新增 `useLineup.test.js`：覆盖三个 API 操作的正常/错误场景（共 ~12 tests）

## 7. 前端组件

- [x] 7.1 新增 `StrategySelector.vue`：预设策略（均衡/集中火力）与自定义自然语言两种模式切换
- [x] 7.2 新增 `LineupCard.vue`：卡片展示 D1–D4 四线，显示球员名、组合 UTR、总 UTR，`aiUsed` 标志
- [x] 7.3 新增 `StrategySelector.test.js`：覆盖模式切换、emit 事件（共 ~6 tests）
- [x] 7.4 新增 `LineupCard.test.js`：覆盖渲染、数据展示（共 ~6 tests）

## 8. 前端排阵生成页面

- [x] 8.1 新增 `LineupGenerator.vue`：队伍下拉选择 + `StrategySelector` + 生成按钮 + `LineupCard` 结果展示 + 错误提示
- [x] 8.2 新增 `LineupGenerator.test.js`：覆盖完整生成流程、加载状态、错误处理（共 ~10 tests）

## 9. 前端路由与导航

- [x] 9.1 在 `router/index.js` 中添加 `/lineup` 路由，指向 `LineupGenerator.vue`
- [x] 9.2 在 `NavSidebar.vue` 中添加「排阵生成」导航入口
- [x] 9.3 更新 `NavSidebar.test.js`：验证新导航项渲染和跳转

## 10. E2E 测试

- [x] 10.1 新增 `frontend/e2e/pages/LineupGeneratorPage.js`（Page Object）
- [x] 10.2 新增 `frontend/e2e/tests/lineup-generation.spec.js`：覆盖完整生成流程（选队伍 → 选策略 → 生成 → 验证卡片展示）

## 11. 测试报告

- [x] 11.1 运行全部测试并记录测试报告
