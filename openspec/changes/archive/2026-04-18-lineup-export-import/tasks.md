## 1. 后端 - 导出 API

- [x] 1.1 在 `LineupService` 中新增 `exportLineups(teamId)` 方法，返回包含 `exportedAt`、`teamId`、`teamName`、`lineups` 的 envelope 对象
- [x] 1.2 在 `LineupController` 中新增 `GET /api/teams/{teamId}/lineups/export` 端点，设置 `Content-Disposition: attachment` 响应头，返回 JSON 文件
- [x] 1.3 在 `LineupControllerTest` 中补充导出端点测试（正常导出、队伍不存在 404、无排阵时返回空数组）

## 2. 后端 - 导入 API

- [x] 2.1 在 `LineupService` 中新增 `importLineups(teamId, lineups)` 方法，实现去重逻辑（按 pairs 中 8 名球员**姓名**有序集合匹配，忽略 ID 差异）、重新分配 ID 和 createdAt、写入 repository，返回 `{ imported, skipped }`
- [x] 2.2 在 `LineupController` 中新增 `POST /api/teams/{teamId}/lineups/import` 端点，接收 multipart `file`，解析 JSON，调用 service
- [x] 2.3 在 `LineupControllerTest` 中补充导入端点测试（正常导入、按姓名去重重复导入 skipped、跨环境 ID 不同但姓名相同时正确去重、队伍不存在 404、文件格式非法 400）

## 3. 前端 - composable

- [x] 3.1 在 `useLineupHistory.js` 中新增 `exportLineups(teamId, teamName)` 函数：调用导出 API，用 `<a>` 标签触发浏览器下载
- [x] 3.2 在 `useLineupHistory.js` 中新增 `importLineups(teamId, file)` 函数：用 FormData 上传文件，返回 `{ imported, skipped }`

## 4. 前端 - UI

- [x] 4.1 在 `LineupHistoryView.vue` 顶部操作栏添加「导出排阵」按钮，点击调用 `exportLineups`
- [x] 4.2 在 `LineupHistoryView.vue` 顶部操作栏添加「导入排阵」按钮，点击触发隐藏的 `<input type="file">` 文件选择框
- [x] 4.3 文件选择后自动调用 `importLineups`，完成后显示结果提示（「导入成功：N 条，跳过：N 条」），并刷新排阵列表
- [x] 4.4 处理导入错误：显示错误提示，不影响现有列表

## 5. 测试与验收

- [x] 5.1 运行后端测试确认全部通过：`mvn test`
- [x] 5.2 运行前端单元测试确认全部通过：`npm test`
- [ ] 5.3 手动验收：本地导出排阵 JSON，导入到生产环境（https://tennis-lineup.fly.dev/），验证排阵内容正确
- [ ] 5.4 验证重复导入：同一文件导入两次，第二次 `imported=0`、`skipped=N`

## 6. 发布

- [x] 6.1 提交代码：`feat(lineup): add export/import for saved lineups`
- [ ] 6.2 部署到 fly.io：`flyctl deploy`
