## Why

已保存排阵页面目前只能查看和删除排阵，缺乏实际比赛准备所需的管理能力。教练在赛前需要能微调排阵（换人、交换搭档）、给每套排阵命名备注、并按重要性排序。这些操作目前必须删了重新生成，效率极低。

## What Changes

- **替换球员**：在已保存排阵中，可以将某条线（D1-D4）的某个位置替换为队内其他球员，替换后实时校验是否违反约束（UTR 上限、搭档差值、位置顺序等），违规则高亮提示并允许保存或撤回。
- **Swap 球员**：选取排阵内两名来自不同位置的球员进行互换，自动重新计算 combinedUtr 并重新排位（此能力已在排阵生成页面的 LineupSwapPanel 实现，需移植/复用到已保存排阵页面）。
- **命名 & 备注**：给每套排阵取一个自定义名称（可选，默认显示策略名）并添加文字备注（comment），持久化保存。
- **排序调整**：允许用户上移/下移已保存排阵的顺序，排在最前面的视为"首选"，顺序持久化。

## Capabilities

### New Capabilities

- `lineup-player-replace`: 在已保存排阵中替换单个球员，含实时约束校验与反馈
- `lineup-history-swap`: 在已保存排阵中 swap 两名球员（复用 LineupSwapPanel 逻辑）
- `lineup-label-comment`: 给已保存排阵命名和添加备注
- `lineup-reorder`: 调整已保存排阵的显示顺序

### Modified Capabilities

- `lineup-history`: 已保存排阵页面增加微调、命名、排序操作入口
- `lineup-save-dedup`: 替换球员后保存时的去重规则需更新（按最新 pairs 的球员名）

## Impact

- **后端模型**：`Lineup` 新增 `label`（string, nullable）、`comment`（string, nullable）、`sortOrder`（int）字段
- **后端 API**：新增 `PATCH /api/teams/{teamId}/lineups/{lineupId}` 端点，支持更新 pairs、label、comment、sortOrder
- **前端**：`LineupHistoryView.vue` 新增替换、swap、命名、排序 UI；复用 `LineupSwapPanel`；新增 `useLineupHistory` 中的 update 方法
- **约束校验**：前端复用后端已有的约束逻辑描述给用户反馈（后端 `ConstraintService` 已实现）
- **风险**：替换球员后的约束校验在前端实现（轻量），后端在保存时再做最终校验；swap 逻辑复用已有 `LineupSwapPanel` 降低风险
