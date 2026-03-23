## 1. TeamDetail.vue — 新增两列

- [x] 1.1 在球员表格 header 中，在「已验证」和「操作」之间添加两列：「个人备注」和「搭档笔记」
- [x] 1.2 在每个球员行中，「个人备注」列显示 `player.notes`（无则显示 `—`）
- [x] 1.3 在 `<script setup>` 中添加 `partnerNotesMap`（`ref({})`）、`partnerNotesLoading`、`partnerNotesError` refs
- [x] 1.4 在 `onMounted` 中调用 `GET /api/teams/{id}/partner-notes`，将每条 note 同时写入 `partnerNotesMap[note.player1Id]` 和 `partnerNotesMap[note.player2Id]`
- [x] 1.5 「搭档笔记」列：遍历 `partnerNotesMap[player.id]`，渲染 chips（紫色，`搭档名: 笔记内容`）；无数据时显示可点击的灰色「+ 添加搭档笔记」chip；loading 时显示 skeleton；error 时显示「加载失败 重试」
- [x] 1.6 每行行首添加 ▶/▼ 展开图标，绑定 `expandedPlayerId`；点击 chip「+ 添加搭档笔记」也触发展开；同一时刻只允许一行展开

## 2. TeamDetail.vue — 批量编辑 Notes 模式

- [x] 2.1 顶部按钮栏添加「批量编辑 Notes」按钮（靛蓝色），与「批量编辑 UTR」和「添加球员」并列
- [x] 2.2 进入 Notes 批量编辑模式时，顶部切换为「正在批量编辑个人备注 / 保存全部 / 取消」；「个人备注」列变为 `<input>`
- [x] 2.3 Notes 批量编辑与 UTR 批量编辑互斥（进入其中一个时，另一个按钮不可见）
- [x] 2.4 「保存全部」调用 `PATCH /api/teams/{id}/players/notes`；成功退出模式；失败显示顶部错误 banner（「保存失败，请重试」），留在编辑模式
- [x] 2.5 「取消」丢弃修改，退出模式

## 3. 新建 PlayerPartnerNotesRow.vue

- [x] 3.1 创建组件，接收 props：`teamId`、`playerId`、`playerName`、`players`（全队名单）、`notes`（PartnerNote[]）
- [x] 3.2 用 `watch(() => props.notes, reinitLocalState, { immediate: true })` 初始化本地可编辑副本
- [x] 3.3 渲染已有搭档笔记行：`[搭档 dropdown] [笔记 input] [✕]`；dropdown 排除当前球员及已在其他行中选用的搭档（基于当前本地状态计算）
- [x] 3.4 末尾渲染一行空白添加行（虚线边框）：`[选搭档…] [添加笔记…]`；排除逻辑同上
- [x] 3.5 ✕ 点击后在本地标记删除（隐藏该行），不立即调用 API
- [x] 3.6 「保存」按钮：依次发送 DELETE（被标记删除的行）、PUT（笔记内容有变更的行）、POST（空白行已填写）；保存中显示 spinner 并禁用输入；全部成功后 emit `saved`；任意失败显示内联错误「保存部分失败，请检查」并保持面板打开
- [x] 3.7 「取消」丢弃本地修改，父组件将 `expandedPlayerId` 置为 null

## 4. TeamDetail.vue — 集成 PlayerPartnerNotesRow

- [x] 4.1 在展开行下方渲染 `<PlayerPartnerNotesRow>`，传入对应 player 的 props
- [x] 4.2 监听 `saved` 事件：重新请求 `GET /api/teams/{id}/partner-notes` 并重建 `partnerNotesMap`

## 5. 清理旧组件

- [x] 5.1 从 `TeamDetail.vue` 移除「队员笔记」折叠区块及 `showNotes` 状态
- [x] 5.2 从 `TeamDetail.vue` 移除 `PlayerNotesEditor` 和 `PartnerNotesEditor` 的 import
- [x] 5.3 删除 `PlayerNotesEditor.vue` 文件

## 6. 单元测试

- [x] 6.1 删除 `PlayerNotesEditor.test.js`
- [x] 6.2 新建 `PlayerPartnerNotesRow.test.js`：
  - 渲染已有笔记行
  - dropdown 排除当前球员及已选搭档
  - ✕ 标记删除后不显示该行
  - 空白行填写后包含在保存请求中
  - 全部成功后 emit `saved`
  - 失败时显示内联错误
  - `notes` prop 更新后重新初始化本地状态

## 7. E2E 测试

- [x] 7.1 更新 `player-partner-notes.spec.js`：
  - 移除所有点击「队员笔记」折叠按钮的测试
  - 添加：「个人备注」列在表格中可见
  - 添加：「搭档笔记」chips 列在表格中可见
  - 添加：批量编辑 Notes → 修改并保存 → 刷新后仍显示
  - 添加：展开行 → 添加搭档笔记 → chips 刷新显示新 chip

## 8. 运行全量测试

- [x] 8.1 运行前端单元测试（`npm test`），确认全部通过
- [x] 8.2 运行 E2E 测试（`npm run test:e2e`），确认无回归
- [x] 8.3 生成测试报告
