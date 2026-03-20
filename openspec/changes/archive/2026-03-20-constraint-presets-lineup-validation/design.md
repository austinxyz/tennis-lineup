## Context

当前排阵生成页每次都需要用户手动设置所有球员约束（排除、必须上、固定位置），没有保存机制。已保存排阵也无法感知球员 UTR 的变化，只能看静态快照。三个功能（约束预设、UTR 重校验、页面跳转链接）相互独立，共同改善排阵工作流。

## Goals / Non-Goals

**Goals:**
- 约束预设 CRUD：每个队伍可保存多个命名预设（含 excludePlayers / includePlayers / pinPlayers），生成时可选择加载。
- 已保存排阵 UTR 合法性实时重校验：访问历史页时，用当前球员 UTR 重新跑 `ConstraintService.validateLineup`，展示合法 / 不合法状态。
- 排阵生成页增加跳转链接到已保存排阵页。

**Non-Goals:**
- 不提供跨队伍复用预设。
- 不自动触发 UTR 变更后的推送通知。
- 不修改排阵保存逻辑本身（非本次范围）。

## Decisions

### 1. 约束预设存储位置：嵌入 Team JSON
将 `constraintPresets: ConstraintPreset[]` 直接加到 `Team`（或 `TeamData`）模型。

**理由**：与现有 `players`、`lineups` 存储方式一致，无需额外文件/数据库；向后兼容（null/空数组时等价于无预设）。

**备选**：独立 JSON 文件。→ 增加读写复杂度，无明显收益，排除。

### 2. 约束预设 API：独立 CRUD 端点
新增 `/api/teams/{id}/constraint-presets` 端点（GET list、POST create/update、DELETE）。

**理由**：职责分离，前端可独立管理预设；与 players、lineups 端点风格一致。

**备选**：通过 PUT /api/teams/{id} 全量更新。→ 需传输全量数据，并发风险高，排除。

### 3. UTR 重校验：前端请求时后端实时计算，结果不持久化
`GET /api/teams/{id}/lineups` 响应中，每条 Lineup 新增 `currentValid: boolean` 和 `currentViolations: string[]` 字段，由后端在接口响应时用当前球员 UTR 重新计算。

**理由**：校验逻辑已在 `ConstraintService.validateLineup` 中集中；不持久化校验结果，避免数据冗余；每次获取都是最新校验。

**备选**：前端拉到排阵后自己计算。→ 需要把 Java 约束逻辑复制到 JS，维护两套，排除。

### 4. ConstraintPreset 数据模型
```json
{
  "id": "preset-<nanoTime>",
  "name": "主力阵容",
  "createdAt": "ISO8601",
  "excludePlayers": ["player-xxx"],
  "includePlayers": ["player-yyy"],
  "pinPlayers": { "player-zzz": "D1" }
}
```

### 5. 排阵生成页跳转链接：纯路由链接
在 `LineupGenerator.vue` 的顶部信息区或结果区新增 `<router-link>` 指向 `/teams/:id/lineups`。实现零成本，无需后端改动。

## Risks / Trade-offs

- **预设名称冲突**：同名预设允许覆盖（由用户确认），不报错。→ 前端弹确认框。
- **球员被删除后预设仍含其 ID**：加载预设时忽略不存在的球员 ID，前端展示提示"部分球员已不存在"。
- **重校验性能**：历史排阵数量通常 < 50，每条调用 `validateLineup` 的计算量极小，无性能风险。
- **向后兼容**：`TeamData` 新增 `constraintPresets` 字段，Jackson 反序列化时若字段缺失默认 null，代码中统一 `getOrDefault(null, List.of())` 处理。

## Migration Plan

1. 后端先扩展模型，部署后前端字段可选接收，零停机。
2. 现有 `team.lineups` 结构不变，只在 GET 响应时动态附加 `currentValid` / `currentViolations`。
3. 无数据迁移脚本需求，旧数据正常兼容。
