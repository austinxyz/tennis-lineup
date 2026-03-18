## 1. 后端：LineupGenerationService 算法重构

- [x] 1.1 修改 `enumerateSubsets` 排序逻辑：去除"恰好2女优先"，改为仅按 cap-valid 优先 + totalUtr 降序（最接近40.5排最前）
- [x] 1.2 提取私有方法 `filterCandidates(List<Lineup>, Set<String> effectiveInclude, Map<String,String> pinPlayers, Map<String,List<String>> playersByPin)`：包含 include 过滤 + pin 约束过滤，返回 `List<Lineup>`
- [x] 1.3 修改 `generateCandidates` 主逻辑（roster > 8 时）：先处理 top-20 子集，调用 `filterCandidates` 检查结果数；若 < 6 则继续处理子集 21-40；最终调用 `filterCandidates` 返回结果
- [x] 1.4 保留原有 roster == 8 时的直接 backtrack 路径（无需分批）

## 2. 后端：LineupService 去除自动持久化

- [x] 2.1 在 `generateMultipleAndSave` 中删除 `team.getLineups().add(top6.get(0))` 和 `jsonRepository.writeData(teamData)` 调用（不再自动保存）
- [x] 2.2 保留 ID 和 createdAt 赋值逻辑（方便前端展示和后续保存）
- [x] 2.3 方法重命名为 `generateMultiple`（去掉"AndSave"）；更新 `LineupController` 的调用

## 3. 后端：手动保存接口

- [x] 3.1 在 `LineupService` 新增 `saveLineup(String teamId, Lineup lineup)` 方法：从 JSON 读取 team，将 lineup 加入 `team.lineups`，写回文件；若 lineup 无 id 则赋 nanoTime id，若无 createdAt 则赋当前时间
- [x] 3.2 在 `LineupController` 新增 `POST /api/teams/{teamId}/lineups` 端点：接收 `@RequestBody Lineup lineup`，调用 `saveLineup`，返回 200 + 保存后的 lineup

## 4. 后端：历史排阵 enrichment

- [x] 4.1 在 `LineupService.getLineupsByTeam` 中：构建 `Map<String, Player> playerMap`；遍历每个 lineup 的每个 pair，若 `player1Utr == null` 则从 playerMap 填充，若 `player1Gender == null` 则填充（player2 同理）
- [x] 4.2 enrichment 不回写文件，仅在内存中补全后返回

## 5. 后端测试更新

- [x] 5.1 更新 `LineupGenerationServiceTest`：修改 `testSubsetEnumerationSelectsHighUtrCombination` — 验证最高 UTR 子集排最前（无2女偏好）
- [x] 5.2 更新 `LineupGenerationServiceTest`：新增 top-20/top-40 批次测试（roster 10人时，top-20 足够则不继续）
- [x] 5.3 更新 `LineupServiceTest`：`generateMultiple` 不再调用 `jsonRepository.writeData`（验证 writeData 未被调用）
- [x] 5.4 新增 `LineupServiceTest`：`saveLineup` 调用 `jsonRepository.writeData` 并将 lineup 加入 team
- [x] 5.5 新增 `LineupControllerTest`：`POST /api/teams/{id}/lineups` 返回 200 + saved lineup
- [x] 5.6 新增 `LineupServiceTest`：`getLineupsByTeam` enrichment — 历史 pair 缺 UTR/gender 时从球员数据填充

## 6. 前端：LineupCard 性别显示修复

- [x] 6.1 在 `LineupCard.vue` 中，将性别 span 改为 `v-if="pair.player1Gender"`（gender 为 null 时不渲染），消除"null → 男"的默认行为；player2 同理

## 7. 前端：手动保存按钮

- [x] 7.1 在 `useLineup.js` 新增 `saveLineup(teamId, lineup)` 函数：`POST /api/teams/{teamId}/lineups`，请求体为 lineup JSON；返回 saved lineup 或抛错
- [x] 7.2 在 `LineupResultGrid.vue` 每个排阵卡片下方加"保留此排阵"按钮（Tailwind 样式），绑定 per-lineup 的 `saved` 和 `saveError` 状态（用 `ref([])` 数组按 index 管理）
- [x] 7.3 点击后：调用 `saveLineup`，成功则该 index 的 saved 设为 true（按钮变"已保留 ✓"并 disabled）；失败则 saveError[index] 显示错误文字

## 8. 前端：去除自动保存逻辑（如有）

- [x] 8.1 检查 `useLineup.js` 的 `generateLineup` — 确认不调用保存接口（当前应已无自动保存，确认并删除任何残留）

## 9. 前端测试更新

- [x] 9.1 更新 `LineupCard.test.js`：验证 `player1Gender: null` 时性别文字不显示（无"男"/"女"）
- [x] 9.2 新增 `useLineup.test.js` 测试：`saveLineup` 调用 `POST /api/teams/{id}/lineups` 且 body 含 lineup 数据
- [x] 9.3 更新 `LineupResultGrid.test.js`（如有）或新增：验证每个 lineup 卡片有"保留此排阵"按钮；点击后变"已保留 ✓"并禁用

## 10. 数据清理

- [x] 10.1 清空 `tennis-data.json` 中所有 team 的 `lineups` 数组（将其设为 `[]`），去除旧的自动保存排阵历史

## 11. 测试运行验证

- [x] 11.1 运行后端测试 `mvn test`，全部通过（无失败）
- [x] 11.2 运行前端测试 `npm test`，全部通过（无失败）
