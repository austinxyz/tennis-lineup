## Requirements

### Requirement: Replace a player in a saved lineup
系统 SHALL 允许用户在已保存排阵的某条线（D1-D4）的某个位置，从当前队伍球员列表中选择一名球员替换。替换后前端 SHALL 即时显示约束校验结果（总 UTR、搭档 UTR 差、位置顺序），但不阻止保存。用户确认后调用 `PATCH /api/teams/{teamId}/lineups/{lineupId}` 持久化。

#### Scenario: 选择替换球员
- **WHEN** 用户在已保存排阵 card 中点击某位置的球员
- **THEN** 弹出该队伍当前球员的下拉列表供选择（不含已在本排阵中的其他球员）

#### Scenario: 替换球员后显示约束校验
- **WHEN** 用户从下拉列表选择新球员
- **THEN** 即时重新计算 combinedUtr，并在 UI 中高亮显示违反的约束（总 UTR 超限、搭档差值 > 3.5、位置顺序错误）

#### Scenario: 保存替换结果
- **WHEN** 用户点击「保存修改」
- **THEN** 调用 PATCH 端点更新 pairs，列表刷新显示最新 currentViolations

#### Scenario: 取消替换
- **WHEN** 用户点击「取消」或重新选原来的球员
- **THEN** 恢复原始 pairs，不调用 API
