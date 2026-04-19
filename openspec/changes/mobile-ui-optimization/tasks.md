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

- [x] 5.1 接入 AppHeader（标题=排阵生成）；`pt-14 lg:pt-0` 让内容避开 fixed header
- [~] 5.2 高级选项折叠暂缓 — 现有布局本就纵向堆叠，Mobile 体验可接受；如需进一步压缩可后续加 `<details>`
- [x] 5.3 「生成排阵」按钮已有 `w-full`（无需改动）
- [x] 5.4 `LineupResultGrid` 已用 `grid-cols-1 lg:grid-cols-2`（无需改动）；⭐最佳标签已在 LineupCard（preferred prop）
- [x] 5.5 单元测试：AppHeader 渲染 + pt-14 padding + 按钮 w-full（3 个新测试）

## 6. 对手分析（最大改动）

- [x] 6.1 保留原有两种模式：**最佳三阵 (bestThree)** + **逐线对比 (headToHead)** + 模式切换 Tab
- [x] 6.2 保留两种分析方式：**UTR match**（算法推荐） + **AI 推荐/点评**（on-demand 按钮）
- [x] 6.3 最佳三阵：两列布局（UTR 最佳三阵 vs AI 推荐）保留
- [x] 6.4 接入 AppHeader（标题=对手策略分析）；`pt-14 lg:pt-0` 让内容避开 fixed header
- [x] 6.5 桌面 h2 `hidden lg:block` 保留；移动端 AppHeader 承担标题作用
- [x] 6.6 原有下拉选择 + 即时预览 + partner notes + 错误处理保持不变
- [x] 6.7 原有 24 个单元测试全部保留

## 7. 测试与验收

- [x] 7.1 运行前端全量单元测试：`npm test`（442 tests 全部通过，超过 400 目标）
- [x] 7.2 新增 E2E 测试 `mobile-layout.spec.js`（视口 375x667）：
  - 汉堡菜单打开/关闭 ✓
  - 队伍列表 → 详情 → 返回 ✓
  - 已保存排阵 Mobile 单列 ✓
  - 排阵生成结果 Mobile 单列 ✓
  - 对手分析 4 下拉选择 + 预览 ✓
- [x] 7.3 视觉回归：Playwright Desktop Chrome (1280x720) 跑完整 E2E 53/53 通过，桌面视觉无回归
- [x] 7.4 iPhone SE 375x667 由 `mobile-layout.spec.js` 5 个场景覆盖；iPad 768 手动验收（非阻塞）

## 8. 部署

- [ ] 8.1 提交 commit：`feat(mobile): responsive mobile UI optimization`
- [ ] 8.2 部署 fly.io：`flyctl deploy`
- [ ] 8.3 生产验证（Mobile Safari / Chrome Mobile）
