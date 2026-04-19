## MODIFIED Requirements

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
