## Why

本地测试环境产生的已保存排阵无法直接同步到生产环境（fly.io），每次需要手动重新生成，费时费力。需要一个 Export/Import 机制，将本地保存的排阵一键导出为 JSON 文件，再导入到生产环境。

## What Changes

- 新增后端 API：`GET /api/teams/{id}/lineups/export` — 将指定队伍的已保存排阵导出为 JSON 文件下载
- 新增后端 API：`POST /api/teams/{id}/lineups/import` — 接收 JSON 文件，将排阵批量导入到目标队伍
- 新增前端功能：在已保存排阵列表页（LineupHistoryView）添加「导出」按钮，触发文件下载
- 新增前端功能：在已保存排阵列表页添加「导入」按钮，支持选择本地 JSON 文件上传

## Capabilities

### New Capabilities

- `lineup-export`: 将队伍的已保存排阵导出为 JSON 文件（包含完整排阵数据）
- `lineup-import`: 从 JSON 文件批量导入排阵到指定队伍，支持重复检测（按球员组合去重）

### Modified Capabilities

（无现有 spec 需修改）

## Impact

- **后端**：`LineupController`（新增两个端点）、`LineupService`（新增 export/import 逻辑）
- **前端**：`LineupHistoryView.vue`（新增导出/导入 UI）、新增 `useLineupHistory.js` 中的 export/import 方法
- **数据格式**：导出的 JSON 包含 `exportedAt`、`teamId`、`teamName`、`lineups[]` 字段
- **风险**：导入时球员 ID 与目标环境不一致（已保存排阵中存储球员信息的快照，不依赖 ID 查询，风险低）
- **工作量**：小（约 4-6 小时），后端 2 个端点 + 前端 2 个按钮
