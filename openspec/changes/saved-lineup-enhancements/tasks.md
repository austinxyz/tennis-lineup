## 1. 后端 — 模型扩展

- [x] 1.1 在 `Lineup.java` 新增 `label`（String, nullable, @JsonProperty）、`comment`（String, nullable）、`sortOrder`（int, 默认 0）字段
- [x] 1.2 在 `LineupService.getLineupsByTeam()` 中，将 sortOrder 初始化（按 createdAt 倒序赋值），并按 sortOrder 升序返回列表
- [x] 1.3 验证旧数据（无 sortOrder 字段）能正常反序列化（Jackson 忽略缺失字段，默认 0）

## 2. 后端 — PATCH 端点

- [x] 2.1 新增 `LineupUpdateRequest`（DTO）：包含 `pairs`、`label`、`comment`、`sortOrder` 全部可选（null = 不更新）
- [x] 2.2 在 `LineupService` 新增 `updateLineup(teamId, lineupId, request)` 方法：读取团队排阵，找到目标排阵，仅更新非 null 字段，写回 repository
- [x] 2.3 在 `LineupController` 新增 `PATCH /api/teams/{teamId}/lineups/{lineupId}` 端点，调用 updateLineup
- [x] 2.4 `updateLineup` 更新 pairs 时，重新生成 dedup key（按球员名集合），若与队内其他排阵重复则抛 400
- [x] 2.5 在 `LineupControllerTest` 补充测试：正常更新 label/comment、更新 pairs、sortOrder 更新、目标不存在 404

## 3. 前端 — composable 扩展

- [x] 3.1 在 `useLineupHistory.js` 新增 `updateLineup(teamId, lineupId, data)` 方法：调用 PATCH 端点，返回更新后的排阵

## 4. 前端 — 排阵命名 & 备注

- [x] 4.1 在 `LineupHistoryView.vue` 每张 card 标题区加可点击的名称字段：点击变 input，失焦调用 updateLineup({ label })，为空则显示 strategy
- [x] 4.2 在 card 底部加备注入口：有 comment 时显示文字可点击编辑；无 comment 时显示「+ 添加备注」；失焦调用 updateLineup({ comment })

## 5. 前端 — 排序（上移/下移）

- [x] 5.1 在 `LineupHistoryView.vue` 每张 card 右上角加「↑」「↓」按钮，首位禁用↑，末位禁用↓
- [x] 5.2 点击↑：交换当前排阵与前一排阵的 sortOrder，依次调用两次 updateLineup({ sortOrder })，完成后刷新列表
- [x] 5.3 点击↓：同上，交换当前与后一排阵
- [x] 5.4 sortOrder 最小的 card 显示「⭐ 首选」徽章

## 6. 前端 — Swap 配对（复用 LineupSwapPanel）

- [x] 6.1 在 `LineupHistoryView.vue` 每张 card 底部添加可展开的 swap 区域（`<details>`），内嵌 `LineupSwapPanel`
- [x] 6.2 `LineupSwapPanel` swap 完成后 emit `update:lineup`，父组件收到后调用 updateLineup({ pairs }) 持久化，再刷新列表

## 7. 前端 — 替换球员

- [x] 7.1 在 `LineupHistoryView.vue` 每张 card 的配对行，每个球员名旁加铅笔图标，点击后该位置变为下拉选框（`<select>`），列出当前队伍所有球员（排除已在本排阵其他位置的球员）
- [x] 7.2 选择新球员后，即时重算受影响 pair 的 combinedUtr，并对整个排阵做前端约束校验（UTR 上限、搭档差值、位置顺序），违规时在 card 内显示红色警告列表
- [x] 7.3 显示「保存修改」和「取消」按钮；点击「保存修改」调用 updateLineup({ pairs })；点击「取消」恢复原始数据

## 8. 测试与验收

- [ ] 8.1 运行后端测试：`mvn test`
- [x] 8.2 运行前端单元测试：`npm test`
- [ ] 8.3 手动验收：命名/备注持久化；swap 后保存正确；替换球员约束提示；上下移顺序保存
- [ ] 8.4 部署到 fly.io：`flyctl deploy`
