## 1. 后端：扩展 GenerateLineupRequest

- [x] 1.1 在 `GenerateLineupRequest.java` 中添加 `includePlayers: List<String>` 和 `excludePlayers: List<String>` 两个可选字段（默认空列表）

## 2. 后端：LineupGenerationService 支持约束过滤

- [x] 2.1 新增 `generateCandidates(List<Player> players, Set<String> include, Set<String> exclude)` 重载方法：先过滤 exclude 集合中的球员，再从 include 集合中选取必须上场球员，最后在剩余球员中生成候选
- [x] 2.2 验证约束合法性：`exclude` 与 `include` 有交集 → 抛 400；`include.size() > 8` → 抛 400；过滤后可用球员 < 8 → 抛 400（含 include 固定球员）

## 3. 后端：LineupService 返回多个候选

- [x] 3.1 重命名现有 `generateAndSave` 为 `generateMultipleAndSave`，返回类型改为 `List<Lineup>`（最多 6 个）
- [x] 3.2 选取逻辑：先按策略启发式排序所有有效候选，取前 6；AI 可用时将 AI 选中的排在第一位，其余按启发式补足到 6 个
- [x] 3.3 只将第一个（最优）lineup 持久化到 `team.lineups`，其余 5 个仅在响应中返回

## 4. 后端：LineupController 返回数组

- [x] 4.1 更新 `POST /api/lineups/generate` 处理器：调用 `generateMultipleAndSave`，响应体为 `List<Lineup>`（`ResponseEntity<List<Lineup>>`）

## 5. 后端测试更新

- [x] 5.1 更新 `LineupControllerTest`：generate 端点断言响应为 JSON 数组，至少含 1 个 lineup
- [x] 5.2 更新 `LineupServiceTest`：覆盖多候选返回、只保存第一个、include/exclude 过滤逻辑
- [x] 5.3 更新 `LineupGenerationServiceTest`：覆盖 include/exclude 约束过滤的正常和边界场景
- [x] 5.4 更新 `docs/api.md`：记录 generate 响应体由单 Lineup 变为 Lineup 数组，新增 includePlayers/excludePlayers 字段说明

## 6. 前端：useLineup.js 支持数组响应和约束参数

- [x] 6.1 更新 `generateLineup` 方法：请求体加入 `includePlayers`、`excludePlayers` 参数；响应由单个 lineup 改为 `lineups: ref([])` 数组
- [x] 6.2 保持向后兼容：`lineup` ref 改为 `lineups` ref（数组），移除单值 `lineup` ref

## 7. 前端：新增 PlayerConstraintSelector.vue

- [x] 7.1 新增 `PlayerConstraintSelector.vue`：接收 `players` prop，每个球员显示姓名 + UTR + 状态切换按钮（中立 → 必须上场 → 排除 → 中立循环）
- [x] 7.2 使用颜色区分状态：中立（灰色）、必须上场（绿色角标）、排除（红色角标带删除线）
- [x] 7.3 顶部显示约束摘要行："必须上场: N 人 / 排除: M 人"
- [x] 7.4 无团队时显示占位文字："请先选择队伍"
- [x] 7.5 向父组件 emit `update:constraints` 事件，payload: `{ includePlayers: string[], excludePlayers: string[] }`

## 8. 前端：新增 LineupResultTabs.vue

- [x] 8.1 新增 `LineupResultTabs.vue`：接收 `lineups: Array` prop，渲染 3 列 × 最多 2 行的 tab 网格（标签为"方案 1"~"方案 N"）
- [x] 8.2 第一个 tab 默认激活，点击切换激活 tab 并展示对应 `LineupCard`
- [x] 8.3 无结果时显示空状态占位区域

## 9. 前端：重构 LineupGenerator.vue 为两栏布局

- [x] 9.1 将页面改为左右两栏布局：左栏（`w-2/5`）含队伍选择 + 策略选择 + `PlayerConstraintSelector`；右栏（`w-3/5`）含 `LineupResultTabs`
- [x] 9.2 响应式：`< lg` 断点时两栏垂直堆叠
- [x] 9.3 将 `lineup` ref 替换为 `lineups` ref 数组，约束状态 `constraints` ref 传给 `PlayerConstraintSelector` 和 generate 调用
- [x] 9.4 错误信息显示保持在左栏底部

## 10. 前端测试更新

- [x] 10.1 更新 `LineupGenerator.test.js`：断言两栏结构，mock useLineup 返回数组，验证 constraints 参数传递
- [x] 10.2 新增 `PlayerConstraintSelector.test.js`：覆盖状态切换、emit 事件、摘要行、空状态（共 ~8 tests）
- [x] 10.3 新增 `LineupResultTabs.test.js`：覆盖 tab 渲染、切换、空状态（共 ~6 tests）
- [x] 10.4 更新 `useLineup.test.js`：覆盖数组响应、新约束参数

## 11. E2E 测试更新

- [x] 11.1 更新 `LineupGeneratorPage.js`（Page Object）：新增 `pinPlayer(name)`, `excludePlayer(name)`, `selectTab(n)`, `waitForResults()` 方法
- [x] 11.2 更新 `lineup-generation.spec.js`：改为等待多结果、验证 tab 切换、验证约束（排除球员不出现）

## 12. 测试报告

- [x] 12.1 运行全部测试（后端 JUnit + 前端 Vitest + E2E Playwright）并记录测试报告
