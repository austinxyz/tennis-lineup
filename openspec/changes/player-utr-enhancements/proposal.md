## Why

球员的 UTR 数值精确到两位小数（如 5.23），当前前端展示和后端校验未明确保证精度。同时，用户需要频繁查看球员在 UTR 官网的最新数据（UTR 每天更新），以及在赛前快速批量更新多名球员的 UTR，当前只能逐个编辑，效率低。

## What Changes

- **UTR 精度保证**：后端存储和校验、前端输入控件均精确到两位小数（step=0.01）；显示时固定两位小数
- **新增球员 URL 字段**：在球员数据模型中添加可选 `profileUrl` 字段，用于存储 UTR 官网个人页链接；在球员列表和表单中显示可点击链接
- **批量编辑 UTR**：在队员列表页面新增「批量编辑 UTR」模式，可内联编辑所有球员 UTR，一次提交保存所有变更

## Capabilities

### New Capabilities

- `player-bulk-utr-edit`: 队员列表批量编辑 UTR——进入编辑模式后，所有球员的 UTR 字段变为可编辑输入框，提交时并发调用后端更新所有已修改项

### Modified Capabilities

- `player-crud`: 球员数据模型新增 `profileUrl` 字段（可选）；UTR 精度要求明确为两位小数；`GET /api/teams/{id}/players` 响应包含 `profileUrl`；添加/编辑球员表单增加 URL 输入项

## Impact

- **后端**：`Player.java` 新增 `profileUrl` 字段；`PlayerRequest` 新增可选 `profileUrl`；UTR 存储格式保持 double（精度由前端控制，后端校验范围不变）
- **前端**：`PlayerForm.vue` 新增 profileUrl 输入；`TeamDetail.vue` 球员列表显示可点击 URL；新增批量编辑模式（输入框内联 + 保存/取消按钮）；`usePlayers.js` 新增批量更新逻辑
- **数据**：现有 JSON 数据中无 `profileUrl` 字段时视为 `null`，完全向后兼容
