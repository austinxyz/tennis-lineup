## 1. 后端数据模型扩展

- [x] 1.1 在 `Player.java` 中新增 `profileUrl: String`（可为 null），添加 Jackson 注解确保缺失时反序列化为 null
- [x] 1.2 在 `PlayerRequest.java`（或对应 DTO）中新增可选 `profileUrl` 字段
- [x] 1.3 更新 `PlayerService.addPlayer` 和 `updatePlayer`：从请求中读取并保存 `profileUrl`（空字符串转存为 null）

## 2. 后端测试更新

- [x] 2.1 更新 `PlayerServiceTest.java`：添加带 `profileUrl` 的添加/更新场景；验证空字符串转 null；验证 null 时正常处理（~4 个新 tests）
- [x] 2.2 更新 `TeamControllerTest.java`：验证 `GET /api/teams/{id}/players` 响应中包含 `profileUrl` 字段（~2 个新 tests）

## 3. 前端 PlayerForm 更新

- [x] 3.1 在 `PlayerForm.vue` 中新增 `profileUrl` 文本输入字段（label: "UTR 主页链接"，非必填，placeholder 示例 URL）
- [x] 3.2 将 UTR 输入控件的 `step` 属性设为 `0.01`，确保两位小数精度
- [x] 3.3 更新 `PlayerForm.test.js`：覆盖 profileUrl 输入渲染、submit 事件含 profileUrl、step=0.01 验证（~4 个新 tests）

## 4. 前端球员列表 UTR 显示与链接

- [x] 4.1 在 `TeamDetail.vue` 球员列表中将 UTR 显示改为 `toFixed(2)` 格式
- [x] 4.2 在每个球员行新增条件渲染：当 `profileUrl` 非空时显示可点击链接（`<a :href="player.profileUrl" target="_blank" rel="noopener noreferrer">`，图标或文字"UTR主页"）
- [x] 4.3 更新 `TeamDetail.test.js`：验证 UTR 显示两位小数；验证有 profileUrl 时显示链接；验证无 profileUrl 时不显示链接（~4 个新 tests）

## 5. 前端批量编辑 UTR — Composable

- [x] 5.1 在 `usePlayers.js` 中新增 `bulkUpdateUtrs(teamId, changes)` 方法：接收 `[{playerId, utr}]` 数组，并发调用 PUT，返回 `{ succeeded, failed }` 结果
- [x] 5.2 新增 `usePlayers.test.js` 中批量更新测试：全部成功、部分失败、无变更时不发请求（~4 个新 tests）

## 6. 前端批量编辑 UTR — UI

- [x] 6.1 在 `TeamDetail.vue` 球员列表区域新增「批量编辑 UTR」按钮
- [x] 6.2 实现编辑模式切换：进入时将每行 UTR 替换为 `<input type="number" step="0.01">` 预填当前值；记录原始值用于取消还原
- [x] 6.3 实现「保存」逻辑：收集所有变更（仅修改过的），调用 `bulkUpdateUtrs`，处理部分失败（高亮失败行 + 错误消息）
- [x] 6.4 实现「取消」逻辑：还原所有输入值，退出编辑模式
- [x] 6.5 实现路由离开守卫：编辑模式有未保存变更时弹出确认对话框
- [x] 6.6 更新 `TeamDetail.test.js`：覆盖进入/退出编辑模式、保存全成功、保存部分失败高亮、取消还原、未改动时保存无请求（~10 个新 tests）

## 7. API 文档更新

- [x] 7.1 更新 `docs/api.md`：在球员相关端点的请求/响应示例中添加 `profileUrl` 字段说明
