## Requirements

### Requirement: Reorder saved lineups
系统 SHALL 允许用户通过上移/下移按钮调整已保存排阵的显示顺序，顺序持久化存储。排在第一位的排阵视为"首选"，以视觉标记区分（如徽章或边框）。

`Lineup` 模型新增 `sortOrder: int`（默认按创建时间倒序赋值）。后端 `GET /api/teams/{teamId}/lineups` SHALL 按 `sortOrder` 升序返回（sortOrder 小的排前）。

#### Scenario: 上移排阵
- **WHEN** 用户点击某排阵 card 的「↑」按钮
- **THEN** 该排阵与前一条交换 sortOrder，两次 PATCH 顺序执行，列表重新排序显示

#### Scenario: 下移排阵
- **WHEN** 用户点击某排阵 card 的「↓」按钮
- **THEN** 该排阵与后一条交换 sortOrder，列表重新排序显示

#### Scenario: 首位排阵的特殊标记
- **WHEN** 某排阵的 sortOrder 最小（排在第一位）
- **THEN** card 上显示「⭐ 首选」徽章

#### Scenario: 边界按钮禁用
- **WHEN** 排阵已在列表最顶部
- **THEN** 「↑」按钮禁用；排在最底部时「↓」按钮禁用
