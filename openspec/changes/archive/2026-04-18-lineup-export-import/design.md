## Context

已保存排阵存储在后端 JSON 文件（`/data/tennis-data.json`）中，隶属于各队伍对象（`team.lineups[]`）。本地开发环境与生产环境（fly.io）各自独立存储，数据不互通。用户在本地生成并保存排阵后，需手动在生产环境重新生成，无法直接迁移。

现有排阵历史 API：
- `GET /api/teams/{id}/lineups` — 获取排阵列表
- `POST /api/teams/{id}/lineups` — 保存排阵
- `DELETE /api/lineups/{id}` — 删除排阵

## Goals / Non-Goals

**Goals:**
- 支持将单个队伍的已保存排阵导出为 JSON 文件（浏览器下载）
- 支持从导出的 JSON 文件将排阵导入到目标队伍（跨环境）
- 导入时按球员组合去重，避免重复导入
- 前端提供导出/导入按钮，操作简单

**Non-Goals:**
- 跨队伍批量导出（一次只处理一个队伍）
- 球员数据同步（仅同步排阵快照，不同步球员信息）
- 导入时验证球员是否存在于目标队伍（排阵存储快照，自包含）

## Decisions

**决策 1：导出格式用自定义 JSON envelope，而非直接返回 Lineup 数组**

导出文件格式：
```json
{
  "exportedAt": "2026-04-18T10:00:00Z",
  "teamId": "team-xxx",
  "teamName": "浙江队",
  "lineups": [ ...Lineup[] ]
}
```

理由：包含元数据（来源队伍、导出时间）便于用户识别文件内容，导入时也可以展示来源信息给用户确认。

**决策 2：导入时重新分配 ID，保留原有快照数据**

导入的每条排阵重新生成 `lineup-{nanoTime}` ID，`createdAt` 设为当前时间。其余字段（pairs、strategy、totalUtr 等）原样保留。

理由：避免与目标环境的 ID 冲突；排阵是数据快照，球员姓名/UTR 都内嵌在 pairs 中，无需与目标环境球员 ID 对应。

**决策 3：去重策略 — 按球员姓名集合（有序）做字符串 key**

导入前检查目标队伍现有排阵，用 pairs 中 8 名球员的 **player1Name / player2Name 有序集合** 拼接为字符串 key，若与待导入排阵相同则跳过。返回 `{ imported: N, skipped: N }` 告知用户结果。

key 构造示例：`"张三,李四,王五,赵六,陈七,刘八,孙九,周十"`（8 个姓名排序后 join）

理由：跨环境导入时球员 ID 必然不同（本地 `player-111` vs 生产 `player-222`），按 ID 去重会导致每次导入都全量写入，无法防止重复。同一队伍内球员姓名唯一，用姓名集合去重既能覆盖跨环境场景，也能防止同一文件重复导入。姓名变更属于极罕见操作，可接受偶发漏判。

**决策 4：前端导出用 `<a>` 标签下载，不经过 useApi**

后端 `GET /api/teams/{id}/lineups/export` 返回 `application/json` 并设置 `Content-Disposition: attachment; filename=...`，前端通过 `window.location.href` 或创建临时 `<a>` 触发下载。

理由：文件下载无需 axios 拦截，直接让浏览器处理更简单可靠。

**决策 5：导入用 multipart/form-data，复用 useApi 的 FormData 路径**

理由：与现有 `POST /api/teams/import`（球员批量导入）保持一致，useApi 已有 FormData 不序列化的处理逻辑。

## Risks / Trade-offs

- **姓名去重的边界情况** → 同队伍姓名唯一（系统约束），跨环境导入时姓名一致性由用户保证。极少数情况下球员改名后再导入会误判为新排阵（可接受）。
- **导入文件格式校验不严** → 缓解：校验顶层字段（`lineups` 必须为数组），单条 Lineup 格式错误跳过并记录，不中断整体导入。
- **大量排阵导入性能** → 低风险：排阵数量通常 < 50 条，JSON 文件 < 100KB，无需流式处理。
