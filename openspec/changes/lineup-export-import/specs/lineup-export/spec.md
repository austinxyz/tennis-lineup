## ADDED Requirements

### Requirement: Export lineups as JSON file
系统 SHALL 提供 `GET /api/teams/{teamId}/lineups/export` 端点，将指定队伍的所有已保存排阵打包为 JSON 文件供下载。响应须设置 `Content-Disposition: attachment; filename=lineups-{teamName}-{date}.json` 和 `Content-Type: application/json`。

导出格式：
```json
{
  "exportedAt": "<ISO 8601>",
  "teamId": "<id>",
  "teamName": "<name>",
  "lineups": [ ...Lineup[] ]
}
```

#### Scenario: 正常导出
- **WHEN** 用户对有排阵记录的队伍调用导出端点
- **THEN** 返回 200，响应体为合法 JSON，包含 `exportedAt`、`teamId`、`teamName`、`lineups` 字段，`lineups` 数组长度与该队伍已保存排阵数量一致

#### Scenario: 队伍不存在
- **WHEN** 用户调用导出端点时 teamId 不存在
- **THEN** 返回 404，错误码 `NOT_FOUND`

#### Scenario: 队伍无排阵
- **WHEN** 用户对尚无已保存排阵的队伍调用导出端点
- **THEN** 返回 200，`lineups` 为空数组 `[]`

### Requirement: Frontend export button
前端 LineupHistoryView SHALL 在排阵历史页面提供「导出排阵」按钮，点击后触发浏览器下载导出 JSON 文件。

#### Scenario: 点击导出
- **WHEN** 用户在排阵历史页面点击「导出排阵」按钮
- **THEN** 浏览器下载名为 `lineups-{teamName}-{date}.json` 的文件，文件内容符合导出格式
