## Requirements

### Requirement: Team detail response includes lineups
`GET /api/teams/{id}` SHALL return the team object including a `lineups` array. When no lineups have been saved, the array SHALL be empty (`[]`), never `null`.

#### Scenario: Team with no lineups returns empty lineups array
- **WHEN** `GET /api/teams/{id}` is called for a team that has no saved lineups
- **THEN** response includes `"lineups": []`

#### Scenario: Team with saved lineups returns full lineup data
- **WHEN** `GET /api/teams/{id}` is called for a team that has saved lineups
- **THEN** response includes `"lineups"` array with all saved lineup objects, each containing `id`, `createdAt`, `strategy`, `pairs`, `totalUtr`, `valid`, `violationMessages`

#### Scenario: Existing JSON without lineups field is backward compatible
- **WHEN** `tennis-data.json` contains a team object without a `lineups` field (legacy data)
- **THEN** system treats `lineups` as empty array and returns `"lineups": []` without error

---

### Requirement: Team detail view includes notes access
The team detail view SHALL include a "队员笔记" section that provides access to both personal notes and partner notes editors for that team.

#### Scenario: Notes section visible in team detail
- **WHEN** the user navigates to a team's detail page
- **THEN** a "队员笔记" section or button is visible that opens the notes editor

#### Scenario: Notes section shows two panels
- **WHEN** the user opens the 队员笔记 section
- **THEN** two panels are displayed: "个人笔记" (personal notes) and "搭档笔记" (partner notes)

---

### Requirement: Team manager responsive layout
系统 SHALL 在 Mobile (`< 1024px`) 下采用列表/详情二选一显示模式，由 URL 驱动：`/` 显示队伍列表，`/teams/:id` 显示队伍详情（列表隐藏）。桌面端（`>= 1024px`）保持现有两栏并排布局。

#### Scenario: Mobile 未选队伍显示列表
- **WHEN** 用户在 Mobile 下访问 `/`
- **THEN** 全屏显示 `TeamListPanel`（队伍列表），包含创建/导入按钮

#### Scenario: Mobile 点击队伍进入详情
- **WHEN** 用户点击某个队伍
- **THEN** 导航到 `/teams/:id`，队伍列表隐藏，`TeamDetail` 全屏显示

#### Scenario: Mobile 返回列表
- **WHEN** 用户在 `/teams/:id` 点击顶部 ← 返回按钮
- **THEN** 导航回 `/`，显示队伍列表

#### Scenario: 桌面端两栏并排
- **WHEN** 视口 `>= 1024px`
- **THEN** 队伍列表 + 详情两栏始终并排显示（现有行为不变）
