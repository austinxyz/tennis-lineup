### Requirement: Playwright E2E 测试框架配置
项目 SHALL 在 `frontend/` 下包含 Playwright 配置，支持通过 `npm run test:e2e` 运行所有 E2E 测试。配置 SHALL 仅启用 Chromium，并通过 `webServer` 自动管理 Vite dev server 生命周期。

#### Scenario: 本地运行 E2E 测试
- **WHEN** 后端已在 localhost:8080 运行，用户执行 `npm run test:e2e`
- **THEN** Playwright 自动启动 Vite dev server（若未运行），在 Chromium 中执行所有 E2E 测试，并输出通过/失败结果

#### Scenario: Dev server 已运行时复用
- **WHEN** localhost:5173 已有正在运行的 Vite dev server
- **THEN** Playwright 复用该 server 而非重新启动，测试正常执行

### Requirement: 队伍管理 E2E 测试
系统 SHALL 提供覆盖队伍创建和删除完整流程的 E2E 测试。

#### Scenario: 创建新队伍
- **WHEN** 用户在队伍管理页面点击创建队伍，输入唯一队伍名称并提交
- **THEN** 新队伍出现在左侧队伍列表中

#### Scenario: 删除队伍
- **WHEN** 用户选中一个队伍并点击删除确认
- **THEN** 该队伍从左侧队伍列表中消失

### Requirement: 球员管理 E2E 测试
系统 SHALL 提供覆盖球员添加、编辑、删除的 E2E 测试，并验证左侧列表计数同步更新。

#### Scenario: 添加球员后左侧计数同步
- **WHEN** 用户在已选中队伍的详情页添加一名球员并提交
- **THEN** 球员出现在右侧球员列表中，且左侧队伍列表中该队伍的球员计数加 1

#### Scenario: 编辑球员信息
- **WHEN** 用户点击某球员的编辑按钮，修改姓名或 UTR 并保存
- **THEN** 球员列表中该球员显示更新后的信息

#### Scenario: 删除球员
- **WHEN** 用户点击某球员的删除按钮并确认
- **THEN** 该球员从球员列表中消失

### Requirement: 队伍切换后球员列表刷新 E2E 测试
系统 SHALL 提供验证切换队伍后球员列表正确刷新的 E2E 测试。

#### Scenario: 切换队伍后显示正确球员
- **WHEN** 用户在左侧列表依次点击队伍 A 和队伍 B
- **THEN** 右侧球员列表在每次切换后均更新为对应队伍的球员，不显示前一个队伍的数据

### Requirement: 批量导入 E2E 测试
系统 SHALL 提供验证 CSV 文件批量导入流程的 E2E 测试。

#### Scenario: 成功导入 CSV 文件
- **WHEN** 用户在批量导入 modal 中上传一个有效的 CSV 文件并提交
- **THEN** 导入结果展示成功数量，队伍球员列表更新显示新增球员

#### Scenario: 导入含无效行的 CSV
- **WHEN** 用户上传包含部分无效行（如 UTR 超范围）的 CSV 文件并提交
- **THEN** 导入结果同时展示成功数量和失败数量及错误描述

### Requirement: 页面对象模型（POM）封装
E2E 测试 SHALL 使用页面对象模型，将 DOM 选择器封装在 `frontend/e2e/pages/` 目录下的 class 中，测试文件 SHALL 只调用 POM 方法，不直接使用选择器字符串。

#### Scenario: POM 封装队伍管理操作
- **WHEN** 测试需要创建队伍时
- **THEN** 测试代码调用 `teamManagerPage.createTeam(name)` 方法，不直接操作 `page.locator(...)` 选择器
