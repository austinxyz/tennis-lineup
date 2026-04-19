## Context

Vue 3 + Tailwind 前端当前布局：
- `MainLayout.vue`：三层嵌套（`NavSidebar` + router-view）
- `TeamManagerView.vue`：两栏（`TeamListPanel` + 嵌套路由详情/空态）
- `LineupHistoryView.vue`：`grid-cols-1 lg:grid-cols-2`（已单列 on mobile）
- 其他页面（排阵生成、对手分析）目前只有桌面布局

Tailwind `lg` = 1024px。断点以下称"Mobile"。

本次只改前端，无后端/API/数据模型变更。

## Goals / Non-Goals

**Goals:**
- `< lg` 屏幕：汉堡菜单 + 单栏/单列布局 + 手指友好的触控尺寸
- `>= lg` 屏幕：**pixel-perfect 保持现状**（任何 Mobile 样式必须 `lg:` 前缀还原）
- 球员行显示姓名/性别徽章/UTR/实际UTR，点击展开细节（Verified Doubles、Notes、操作）
- 已保存排阵、排阵生成结果：卡片内每名球员独占一行，信息清晰
- 对手分析：4 个下拉（我方队伍/排阵 + 对手队伍/排阵）+ 即时预览

**Non-Goals:**
- PWA、离线支持、推送通知
- 手势操作（滑动返回）
- 桌面端重构
- 独立 Mobile App

## Decisions

**决策 1：单断点 `lg` (1024px)**

不区分 Mobile/Tablet。768-1024px 的 iPad 归入 Mobile 布局。
理由：简化实现和测试矩阵；iPad 竖屏更适合 Mobile 体验；如需调整可后续改断点。

**决策 2：全局 sidebarOpen 状态用 ref + provide/inject**

避免引入 Pinia。`MainLayout.vue` 定义 `const sidebarOpen = ref(false)` 并 `provide('sidebarOpen', sidebarOpen)`，`AppHeader.vue` inject 后切换。

**决策 3：队伍管理用 URL 驱动切换**

- `/` → TeamManagerView 显示 `TeamListPanel`（桌面两栏时右侧为 HomeView 空态）
- `/teams/:id` → TeamManagerView 显示 TeamDetail

Mobile 下：
- 根据 `route.params.id` 判断：有值显示详情，无值显示列表
- 两者互斥，无并列
- 返回按钮 `router.push('/')`

桌面下：
- 始终并排显示（现状不变）

**决策 4：LineupCard 球员行结构重做**

每对 2 行（上下堆叠 player1 / player2），每行：
```
[性别徽章] 姓名(加粗 flex-1) UTR(小灰) 实:UTR(橙色)
```

桌面端不需变化（当前横排"player1Name / player2Name"在宽屏 OK）。实际上，参考既有视觉习惯，**桌面也切换到新的两行布局更一致**，避免维护两套样式。评估后在实现阶段决定。

**暂定：两种布局都用新的 2 行布局**（桌面也适用，只是因为宽度大视觉密度看起来不同）。

**决策 5：TeamDetail 移动卡片视图作为独立块**

桌面保留现有表格（`hidden lg:block`），Mobile 新增卡片列表（`lg:hidden`）。
两者互斥显示，代码上并存，避免复杂响应式表格样式。

**决策 6：对手分析完全改为下拉**

移除现有文本解析逻辑（如果有的话）。前提：用户已经把对手队伍建好并保存了至少一条排阵。
若对手队伍没有排阵：排阵下拉显示禁用状态 + "该队伍暂无排阵，请先添加"。

这是一个 UX 突破性变更——用户不能即兴输入"我刚听说对手的阵容"临时分析。
**缓解**：用户可以快速新建对手队伍 + 用「导入排阵」或手动保存一条排阵后再分析。

**决策 7：AppHeader 只在 Mobile 显示**

桌面端不显示顶部栏（现有布局没有顶部栏，侧栏 + 内容直接并排）。
`AppHeader.vue` 外包 `<header class="lg:hidden ...">`。

## Risks / Trade-offs

- **对手分析 UX 变更** → 用户需先建对手队伍。缓解：文档/首次使用引导；后续若反馈强烈，可恢复"文本输入"作为 alternate 入口
- **iPad 竖屏体验** → 断点 `lg` = 1024px，iPad (768px) 归入 Mobile 可能不是所有用户期待。可后续调整
- **桌面视觉回归** → 所有 Mobile 样式必须 `lg:` 前缀，否则桌面会被破坏。测试：在 1280px 分辨率下视觉 diff 截图
- **LineupCard 被多处使用**（排阵生成、保存排阵、对手分析），改动扩散风险 → 完整回归测试覆盖所有使用点
- **全局 sidebar 状态管理**：ref + provide 简单但无持久化。用户刷新页面后侧栏重置关闭状态——可接受

## Migration Plan

这是纯前端变更，无数据迁移。

部署步骤：
1. 合并 PR 后触发 fly.io 构建
2. 部署后用户下次访问自动获得新界面
3. 桌面用户无感知（样式不变）
4. Mobile 用户直接看到新布局

回滚：git revert + 重新部署。

## Open Questions

- **iPad 断点**：目前归 Mobile。若用户反馈 iPad 横屏想用桌面样式，可改到 `md` (768px) 或引入 `xl` (1280px)
- **是否需要首次使用引导**：对手分析 UX 变更可能让老用户困惑，考虑加个一次性 toast 或引导
- **排阵生成结果页：默认全展开还是首方案展开其他折叠？** Brainstorming 确认"全展开"
