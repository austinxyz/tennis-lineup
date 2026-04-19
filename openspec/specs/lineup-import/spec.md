## ADDED Requirements

### Requirement: Import lineups from JSON file
系统 SHALL 提供 `POST /api/teams/{teamId}/lineups/import` 端点，接收 multipart/form-data（字段名 `file`），解析导出格式的 JSON 文件，将其中的排阵批量写入目标队伍。

返回格式：
```json
{ "imported": 3, "skipped": 1 }
```

去重规则：若待导入排阵的 pairs 中 8 名球员的**姓名集合（有序）**与目标队伍现有某条排阵完全一致，则跳过该条。使用姓名而非 ID，确保跨环境导入（ID 不同但球员相同）时能正确去重。

每条导入的排阵重新生成 ID（`lineup-{nanoTime}`）和 `createdAt`（当前时间），其余字段原样保留。

#### Scenario: 正常导入
- **WHEN** 用户上传合法的导出 JSON 文件到目标队伍
- **THEN** 返回 200，`imported` 等于成功写入数量，`skipped` 等于重复跳过数量，目标队伍排阵列表新增对应条目

#### Scenario: 重复导入同一文件
- **WHEN** 用户对同一队伍两次上传相同导出文件
- **THEN** 第二次返回 200，`imported` 为 0，`skipped` 等于文件中排阵数量

#### Scenario: 目标队伍不存在
- **WHEN** 用户上传文件时 teamId 不存在
- **THEN** 返回 404，错误码 `NOT_FOUND`

#### Scenario: 文件格式非法
- **WHEN** 上传的文件不是合法 JSON 或缺少 `lineups` 字段
- **THEN** 返回 400，错误码 `VALIDATION_ERROR`，提示格式错误

#### Scenario: 单条排阵数据异常
- **WHEN** 文件中某条排阵数据缺少必要字段（如 pairs 为 null）
- **THEN** 跳过该条，继续处理其余排阵，最终结果中该条计入 `skipped`

### Requirement: Frontend import button
前端 LineupHistoryView SHALL 提供「导入排阵」按钮，点击后弹出文件选择框，选择 JSON 文件后自动上传，并在完成后显示导入结果（成功 N 条，跳过 N 条）。

#### Scenario: 成功导入
- **WHEN** 用户选择合法的导出 JSON 文件并确认
- **THEN** 文件上传完成后显示「导入成功：N 条，跳过：N 条」，排阵列表自动刷新

#### Scenario: 导入失败
- **WHEN** 用户上传非法文件或服务器报错
- **THEN** 显示错误提示，排阵列表不变
