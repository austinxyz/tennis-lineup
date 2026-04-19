## Requirements

### Requirement: Swap players in a saved lineup
系统 SHALL 在已保存排阵 card 中提供 swap 面板，允许用户选取两名来自不同位置的球员进行互换，自动重新计算 combinedUtr 并按 UTR 降序重排 D1-D4 位置，确认后持久化。

复用现有 `LineupSwapPanel.vue` 组件，swap 完成后通过 `PATCH /api/teams/{teamId}/lineups/{lineupId}` 保存。

#### Scenario: 展开 swap 面板
- **WHEN** 用户点击已保存排阵 card 上的「调整配对」按钮
- **THEN** 展开 swap 面板，列出 4 条线的 8 名球员供选择

#### Scenario: 执行 swap
- **WHEN** 用户选中两名来自不同位置的球员并点击「互换」
- **THEN** 两人交换搭档，combinedUtr 重新计算，位置按 UTR 降序重排，显示变更预览

#### Scenario: 保存 swap 结果
- **WHEN** 用户点击「保存修改」
- **THEN** 调用 PATCH 端点更新 pairs，card 刷新显示新配对

#### Scenario: 重置 swap
- **WHEN** 用户点击「重置」
- **THEN** 恢复到本次 swap 前的状态，不调用 API
