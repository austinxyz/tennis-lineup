## Why

当前前端按桌面优先设计：固定侧栏 + 两栏/多列布局，在手机屏幕（< 1024px）下体验糟糕——侧栏占据过半屏幕、队伍列表与详情横向挤压、排阵卡片被压扁。用户越来越多在手机上查看已保存排阵和做对手分析，需要一套专门的 Mobile 布局。

## What Changes

- **全局框架**：新增汉堡菜单 ☰ 折叠左侧导航（桌面常驻，移动端抽屉）、返回按钮 ← 回到上一级
- **队伍管理**：移动端列表和详情二选一显示（URL 驱动），桌面保持两栏
- **队伍详情**：移动端每行球员显示姓名/性别/UTR/实际UTR，点击展开 Verified Doubles、Notes、编辑删除
- **已保存排阵**：移动端单列堆叠，卡片内每名球员单独一行（姓名/性别徽章/UTR/实际UTR）
- **排阵生成**：移动端表单高级选项折叠为 `<details>`，结果单列堆叠
- **对手分析**：移除文本输入，改为 4 个下拉（我方队伍/排阵 + 对手队伍/排阵），选定后即时显示两边预览；结果页逐线胜率卡片 + 风险线警示 + AI 点评
- **断点**：`lg` (1024px)。`< lg` 走 Mobile 布局，桌面保持现状

## Capabilities

### New Capabilities

- `mobile-layout`: 全局 Mobile 框架（汉堡菜单、抽屉导航、返回按钮、响应式顶部栏）

### Modified Capabilities

- `team-management`: 移动端列表/详情二选一切换，URL 驱动
- `player-crud`: 移动端球员行折叠/展开交互
- `lineup-history`: 移动端单列布局 + 卡片内球员行重构
- `lineup-generation`: 移动端表单折叠 + 结果单列
- `opponent-analysis`: 改为纯下拉选择（对手必须是系统内已有队伍且有已保存排阵），移除文本输入

## Impact

- **前端组件**：
  - 新增 `AppHeader.vue`、`SidebarDrawer.vue`
  - 修改 `MainLayout.vue`（引入 AppHeader 和全局 sidebarOpen 状态）
  - 修改 `TeamManagerView.vue`、`TeamDetail.vue`、`LineupCard.vue`、`LineupHistoryView.vue`、`LineupGenerator.vue`、`OpponentAnalysis.vue`
- **后端**：无改动（已有 API 足够）
- **数据模型**：无改动
- **路由**：无改动（现有嵌套路由正好支持列表/详情切换）
- **风险**：桌面体验必须保持 pixel-perfect 不变（所有 Mobile 样式只在 `< lg` 生效）；对手分析 UX 改变，需要用户先建对手队伍（备注已在 spec 说明）
- **工作量**：中等（1-2 天），涉及 8 个组件文件 + 测试

详细设计参考 `docs/superpowers/specs/2026-04-19-mobile-ui-optimization-design.md`
