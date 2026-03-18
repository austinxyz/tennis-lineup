## 1. 后端：GenerateLineupRequest 扩展

- [x] 1.1 在 `GenerateLineupRequest.java` 中添加 `pinPlayers: Map<String, String>` 字段（playerId → "D1"|"D2"|"D3"|"D4"），默认空 Map

## 2. 后端：LineupGenerationService 支持位置固定约束

- [x] 2.1 在 `generateCandidates(players, include, exclude)` 重载中接受 `pinPlayers` 参数；校验：pinPlayer 与 exclude 有交集 → 抛 400；position 值不在 D1-D4 → 抛 400
- [x] 2.2 后处理过滤：保留所有满足 "指定球员出现在指定位置" 的候选排阵；若过滤后为空 → 抛 400（"无法生成满足位置约束的排阵"）

## 3. 后端：LineupService 重新排序算法

- [x] 3.1 所有候选先按 `40.5 - totalUtr` 升序排列（最接近上限的排在前面）作为主排序
- [x] 3.2 均衡策略（balanced）：次排序按 `Σ|pair.combinedUtr - 10.125|` 升序（偏差最小的排方案 1）
- [x] 3.3 更新 `generateMultipleAndSave` 调用，传入 `pinPlayers` 参数

## 4. 后端：LineupController 传递 pinPlayers

- [x] 4.1 更新 `POST /api/lineups/generate` 处理器：从请求读取 `pinPlayers` 并传入 `generateMultipleAndSave`

## 5. 后端测试更新

- [x] 5.1 更新 `LineupGenerationServiceTest`：覆盖 pinPlayers 合法场景（单人固定、多人固定）和异常（冲突、无效位置、无满足方案）
- [x] 5.2 更新 `LineupServiceTest`：覆盖新的主排序（总 UTR 接近 40.5 优先）和均衡策略次排序逻辑
- [x] 5.3 更新 `LineupControllerTest`：覆盖 pinPlayers 请求字段传递

## 6. 前端：PlayerConstraintSelector 排序和认证标志

- [x] 6.1 添加 `computed sortedPlayers`：先女后男，同性别内按 UTR 降序，UTR 相同按名字升序
- [x] 6.2 每个球员行显示 "认证" 绿色小徽章（`verified === true`）
- [x] 6.3 将渲染列表从 `players` 替换为 `sortedPlayers`

## 7. 前端：PlayerConstraintSelector 位置固定状态

- [x] 7.1 将球员状态从 3 态（中立/必须上场/排除）改为 6 态循环：中立 → D1 → D2 → D3 → D4 → 排除 → 中立（移除独立"必须上场"态）
- [x] 7.2 更新按钮标签和颜色：D1-D4 使用蓝色系；排除保持红色；中立保持灰色
- [x] 7.3 更新约束摘要行：新增"固定位置: N 人"计数
- [x] 7.4 更新 `update:constraints` emit payload：加入 `pinPlayers: Record<playerId, position>`；`includePlayers` 字段移除（合并到位置固定）

## 8. 前端：LineupCard 显示每个球员的 UTR

- [x] 8.1 更新 `LineupCard.vue` 的每行配对显示：`player1Name (utr) / player2Name (utr)` 格式
- [x] 8.2 添加 `showPlayerUtr` prop（默认 `true`）；为 false 时退化为现有显示格式

## 9. 前端：LineupResultGrid 替换 LineupResultTabs

- [x] 9.1 新增 `LineupResultGrid.vue`：接收 `lineups: Array` prop，2 列 × 最多 3 行 CSS grid（desktop），1 列（移动端）
- [x] 9.2 每个 lineup card 头部显示"方案 N"标签；方案 1 加绿色边框/徽章区分为最佳方案
- [x] 9.3 无结果时显示空状态占位区域
- [x] 9.4 在 `LineupGenerator.vue` 中用 `LineupResultGrid` 替换 `LineupResultTabs`

## 10. 前端：LineupSwapPanel 手动互换

- [x] 10.1 新增 `LineupSwapPanel.vue`：接收 `lineup` prop，显示 4 条线各 2 名球员；用户可点选两个球员（需来自不同位置）
- [x] 10.2 点击"互换"按钮：交换两球员所在位置，重新计算 combinedUtr；校验 D1 ≥ D2 ≥ D3 ≥ D4，违反则显示行内错误
- [x] 10.3 "重置"按钮：恢复为原始排阵
- [x] 10.4 将 `LineupSwapPanel` 集成到 `LineupResultGrid`：每个 lineup card 下方或侧边显示（可折叠）

## 11. 前端：useLineup.js 更新

- [x] 11.1 更新 `generateLineup` 方法：请求体加入 `pinPlayers` 参数

## 12. 前端测试更新

- [x] 12.1 更新 `PlayerConstraintSelector.test.js`：覆盖 6 态切换、排序（女优先、UTR 降序）、认证徽章、更新后的 emit payload（含 pinPlayers）
- [x] 12.2 更新 `LineupCard.test.js`：覆盖 showPlayerUtr prop、per-player UTR 显示格式
- [x] 12.3 新增 `LineupResultGrid.test.js`：覆盖多方案同时显示、方案 1 高亮、空状态（~6 tests）
- [x] 12.4 新增 `LineupSwapPanel.test.js`：覆盖球员选择、合法互换、违反约束拒绝、重置（~8 tests）
- [x] 12.5 更新 `LineupGenerator.test.js`：验证 LineupResultGrid 替代 LineupResultTabs、pinPlayers 传递
- [x] 12.6 更新 `useLineup.test.js`：覆盖 pinPlayers 参数

## 13. E2E 测试更新

- [x] 13.1 更新 `LineupGeneratorPage.js`：新增 `pinPlayerToPosition(name, position)`、`verifyAllCardsVisible()`、`swapPlayers(card, name1, name2)` 方法
- [x] 13.2 更新 `lineup-generation.spec.js`：验证所有方案卡片直接显示（无 tab 导航）、per-player UTR 可见、位置固定约束、手动互换

## 14. 测试报告

- [x] 14.1 运行全部测试（后端 JUnit + 前端 Vitest + E2E Playwright）并记录测试报告
