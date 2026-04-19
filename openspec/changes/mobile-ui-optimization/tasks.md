## 1. 全局框架

- [x] 1.1 新增 `src/components/AppHeader.vue` — props: title, backTo（可选），slot: actions；内含 ☰ 按钮 + ← 返回按钮 + 标题
- [~] 1.2 ~~新增 `src/components/SidebarDrawer.vue`~~ — 抽屉逻辑直接在 `MainLayout.vue` 内实现，无需单独组件
- [x] 1.3 修改 `src/layouts/MainLayout.vue` — 引入 sidebarOpen ref + provide；移除内联头部；加 Escape 关闭抽屉
- [x] 1.4 单元测试 `AppHeader.test.js`：props 渲染、汉堡 click、back 按钮显示、aria-label 状态、fallback 安全（16 个测试）
- [~] 1.5 ~~SidebarDrawer.test.js~~ — 抽屉逻辑已通过 MainLayout + AppHeader 测试间接覆盖

## 2. 队伍管理 & 队伍详情

- [x] 2.1 修改 `src/views/TeamManagerView.vue` — Mobile 根据 `route.params.id` 二选一显示 TeamListPanel / TeamDetail；桌面保留两栏
- [x] 2.2 修改 `src/views/TeamDetail.vue` — 新增 Mobile 卡片视图（`lg:hidden`），桌面保留现有表格（`hidden lg:block`）
- [x] 2.3 Mobile 球员卡片行：`[性别] 姓名 UTR[●Verified] 实:UTR ▸`，点击/Enter/Space 展开 Verified Doubles + 性别全称 + Notes + [编辑][删除]；role=button + tabindex 支持键盘
- [x] 2.4 单元测试 `TeamDetail.test.js` 补充：Mobile 视图渲染、行展开/折叠、键盘操作、click.stop 行为、空列表（共 14 个新测试）
- [x] 2.5 单元测试 `TeamManagerView.test.js`：Mobile 下根据路由显示列表或详情（6 个测试）

## 3. LineupCard 重构（影响三页）

- [x] 3.1 修改 `src/components/LineupCard.vue` — 球员行改为每对两行（player1 上 / player2 下），每行 `[性别徽章] 姓名(加粗) UTR 实:UTR`
- [x] 3.2 性别徽章样式：女 `bg-pink-100 text-pink-700`，男 `bg-blue-100 text-blue-700`
- [x] 3.3 实际 UTR 只在非 null 且不等于 utr 时显示，橙色 `text-amber-500 font-semibold`
- [~] 3.4 Verified 绿点暂缓（本批次未实现，后续单独补）
- [x] 3.5 单元测试 `LineupCard.test.js` 更新：2 行结构 + 性别徽章 + 实际 UTR 显示 + 部分空 actualUtr 场景（12 个新测试）

## 4. 已保存排阵

- [x] 4.1 `src/views/LineupHistoryView.vue` 接入 AppHeader（标题含队伍名，返回→`/teams/:id`）；桌面 header `hidden lg:flex` 保留
- [x] 4.2 Mobile actions slot 含 [导出][导入] 紧凑按钮（`data-testid="export-btn-mobile"`/`import-btn-mobile"`）；`importInput`/`importInputDesktop` 分开 ref 避免冲突
- [x] 4.3 LineupCard 在 LineupHistoryView 下验证（LineupCard mock 仍返回 stub，单元测试覆盖；E2E 留到最后统一验）

## 5. 排阵生成

- [ ] 5.1 修改 `src/views/LineupGenerator.vue` — Mobile 表单垂直堆叠
- [ ] 5.2 高级选项（固定位置/包含/排除）用 `<details>` 折叠，summary 内显示已选数量徽章
- [ ] 5.3 「生成排阵」按钮 Mobile 下 `w-full`，桌面端保持现状
- [ ] 5.4 结果区：Mobile `grid-cols-1`，桌面 `lg:grid-cols-2`，方案 1 卡片额外加 `⭐ 最佳` 标签和双倍绿色边框
- [ ] 5.5 单元测试更新：折叠区展开/收起、Mobile 表单布局

## 6. 对手分析（最大改动）

- [ ] 6.1 修改 `src/views/OpponentAnalysis.vue` — 移除对手文本输入区
- [ ] 6.2 新增 4 个下拉：我方队伍、我方排阵、对手队伍、对手排阵
- [ ] 6.3 对手队伍选择后，用 `fetchLineups(opponentTeamId)` 加载其已保存排阵填充第 4 个下拉
- [ ] 6.4 对手队伍无排阵时，第 4 个下拉禁用 + 提示文字；分析按钮禁用
- [ ] 6.5 即时显示我方/对手预览卡（我方白底绿边，对手红底红边），每对两行球员
- [ ] 6.6 结果页整体胜率卡（绿色渐变大字）
- [ ] 6.7 结果页每线对战卡：pos/胜率/进度条/左右对战
- [ ] 6.8 风险线（< 50%）橙色边框 + ⚠️ + 黄色建议框
- [ ] 6.9 AI 点评卡（蓝色）
- [ ] 6.10 单元测试 `OpponentAnalysis.test.js`：4 下拉联动、空排阵禁用、预览渲染、结果分级颜色

## 7. 测试与验收

- [ ] 7.1 运行前端全量单元测试：`npm test`（目标 ≥ 400 tests 全部通过）
- [ ] 7.2 新增 E2E 测试 `mobile-layout.spec.js`（视口 375x667）：
  - 汉堡菜单打开/关闭
  - 队伍列表 → 详情 → 返回
  - 已保存排阵 Mobile 单列
  - 排阵生成结果 Mobile 单列
  - 对手分析 4 下拉选择 + 预览
- [ ] 7.3 视觉回归：在 1280x800 分辨率对比桌面截图，确认现有视觉未被破坏
- [ ] 7.4 手动验收（Chrome DevTools 设备模拟 iPhone SE 375px + iPad 768px）

## 8. 部署

- [ ] 8.1 提交 commit：`feat(mobile): responsive mobile UI optimization`
- [ ] 8.2 部署 fly.io：`flyctl deploy`
- [ ] 8.3 生产验证（Mobile Safari / Chrome Mobile）
