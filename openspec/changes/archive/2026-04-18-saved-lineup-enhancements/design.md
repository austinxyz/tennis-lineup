## Context

已保存排阵（`LineupHistoryView`）目前只读。本次在同一页面内增加四个编辑能力，共用一个后端 `PATCH` 端点，前端所有编辑在 card 内就地完成（inline editing），不跳转新页面。

现有相关实现：
- `LineupSwapPanel.vue` — 排阵生成页 swap 面板，完整实现了 swap 逻辑（已含约束校验提示）
- `ConstraintService.java` — 后端约束校验（UTR 上限、搭档差值 ≤ 3.5、位置顺序递减）
- `PATCH /api/teams/{teamId}/lineups/{lineupId}` — 本次新增

## Goals / Non-Goals

**Goals:**
- 替换单个球员（下拉选队内球员），替换后前端即时显示约束违规
- Swap 两名不同位置球员，自动重排 D1-D4
- 给排阵取名、加备注，持久化
- 上移/下移调整排阵顺序，持久化

**Non-Goals:**
- 拖拽排序（用上下箭头代替，实现简单）
- 跨队伍操作
- 历史版本回退

## Decisions

**决策 1：单一 PATCH 端点处理所有编辑**

`PATCH /api/teams/{teamId}/lineups/{lineupId}` 接收部分更新：
```json
{
  "pairs": [...],      // 替换球员或 swap 后的新 pairs
  "label": "首选方案",  // null = 用策略名
  "comment": "备注",
  "sortOrder": 2
}
```

理由：四个功能都是对同一个 Lineup 对象的字段更新，合并为一个端点简化 API 表面积。前端各个功能独立调用，每次只传变化的字段（其余字段 null 表示不更新）。

**决策 2：替换球员的约束校验在前端实现**

前端在用户选择替换球员后，用 JS 重新计算 combinedUtr 并检查：
- 总 UTR ≤ 40.5
- 每对搭档 UTR 差 ≤ 3.5
- D1 ≥ D2 ≥ D3 ≥ D4（combinedUtr）

违规时在 UI 内用红色提示（不阻止保存，由用户决定是否强制保存）。

理由：后端 `ConstraintService` 的校验结果通过 `getLineupsByTeam` 已返回 `currentValid`/`currentViolations`，但前端需要即时反馈。轻量 JS 校验延迟低；保存后后端 `PATCH` 不再做约束校验（用户已知情）。

**决策 3：sortOrder 用整数，后端按 sortOrder 升序返回**

`Lineup.sortOrder` 默认值 = 列表中的位置索引。上移/下移时交换相邻两条的 sortOrder，各发一次 PATCH。

理由：简单可靠，无需维护全局序列。

**决策 4：Swap 功能复用 LineupSwapPanel**

`LineupHistoryView` 中每张 card 展开一个 `LineupSwapPanel`（和排阵生成页相同），swap 完成后发 PATCH 持久化。

理由：`LineupSwapPanel` 已完整实现 swap + 重排位逻辑，不重复造轮子。

**决策 5：label 默认显示策略名，为空时回退**

`LineupCard` 中显示 `lineup.label || lineup.strategy`，label 字段为空时不显示输入框占位。

## Risks / Trade-offs

- **前端约束校验与后端不完全一致** → 可接受：前端只做主要约束的即时提示，后端 `getLineupsByTeam` 返回的 `currentViolations` 是权威校验结果，保存后刷新列表用户可见。
- **sortOrder 交换需要两次 PATCH** → 可接受：操作不频繁，两次请求顺序执行即可。
- **替换球员后 dedup key 变化** → 已处理：PATCH 中用最新 pairs 球员名重建 dedup key（服务层处理）。
