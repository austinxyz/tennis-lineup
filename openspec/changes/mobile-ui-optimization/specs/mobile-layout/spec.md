## ADDED Requirements

### Requirement: Mobile breakpoint and sidebar drawer
系统 SHALL 在视口宽度 `< 1024px`（Tailwind `lg` 断点以下）时切换到 Mobile 布局：左侧导航栏折叠为抽屉，需点击汉堡菜单 ☰ 打开。桌面端（`>= 1024px`）保持现有常驻侧栏布局不变。

#### Scenario: Mobile 下侧栏默认隐藏
- **WHEN** 用户在 `< 1024px` 视口下打开任意页面
- **THEN** 左侧导航栏不可见，顶部栏左侧显示汉堡菜单 ☰

#### Scenario: 点击汉堡菜单打开抽屉
- **WHEN** 用户在 Mobile 下点击 ☰
- **THEN** 左侧导航抽屉滑入，背景显示半透明遮罩 `rgba(0,0,0,0.4)`

#### Scenario: 点击遮罩或导航项关闭抽屉
- **WHEN** 用户点击遮罩 / 点击任意导航链接 / 按 Esc
- **THEN** 抽屉滑出关闭

#### Scenario: 桌面端侧栏常驻
- **WHEN** 视口 `>= 1024px`
- **THEN** 左侧导航栏常驻可见，无汉堡菜单，无遮罩

### Requirement: AppHeader with back button on sub-pages
系统 SHALL 在 Mobile 下为所有页面提供统一顶部栏 `AppHeader`，包含汉堡菜单 + 可选返回按钮 + 当前页面标题 + 右侧操作区（slot）。返回按钮仅在子页面显示（队伍详情、已保存排阵、排阵生成、对手分析），点击后返回上一级。桌面端不显示此顶部栏。

#### Scenario: 未选队伍时不显示返回按钮
- **WHEN** 用户在 Mobile 下打开 `/` 队伍管理首页
- **THEN** 顶部栏只显示 ☰ + "队伍列表" 标题 + 操作区

#### Scenario: 进入子页面显示返回按钮
- **WHEN** 用户进入 `/teams/:id` 或 `/lineup` 或 `/opponent-analysis` 或 `/teams/:id/lineups`
- **THEN** 顶部栏显示 ☰ + ← 返回 + 页面标题

#### Scenario: 点击返回按钮
- **WHEN** 用户点击 ← 返回
- **THEN** 导航回到上一级路由（通常是 `/`）
