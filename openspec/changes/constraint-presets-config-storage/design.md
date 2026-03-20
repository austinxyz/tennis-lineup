## Context

当前 `JsonRepository` 管理单个 `tennis-data.json`（ReadWriteLock + 原子写入）。`TeamData` 含 `teams: List<Team>`，每个 Team 含 players 和 lineups。配置类数据（约束预设）若混入 `TeamData` 会导致核心文件无限膨胀，且写锁争用加剧。

本次变更同时实现：①存储拆分（基础设施）；②约束预设功能（使用新 config 存储）；③排阵 UTR 重校验；④页面导航改善。

## Goals / Non-Goals

**Goals:**
- `tennis-config.json` 存放配置类数据，独立 ReadWriteLock，与核心数据锁互不干扰
- 约束预设 CRUD，存于 `ConfigData.constraintPresets`（按 teamId 分组的 Map）
- `GET /api/teams/{id}/lineups` 响应附加实时 UTR 校验字段（不持久化）
- 排阵生成页新增预设选择器和跳转链接
- Service 层以上对存储拆分零感知

**Non-Goals:**
- 不做按 team 分文件（方案 A 已排除）
- 不跨队伍共享预设
- 不自动推送 UTR 变更通知
- 不修改排阵保存逻辑

## Decisions

### 1. 存储：ConfigData 独立文件，双锁双路径
`JsonRepository` 新增 `configFilePath` + `ReadWriteLock configLock`，对应 `readConfig(): ConfigData` / `writeConfig(ConfigData)` 方法。原 `read()` / `write(TeamData)` 完全不变。

```yaml
storage:
  data-file: data/tennis-data.json      # 已有
  config-file: data/tennis-config.json  # 新增，有默认值
```

**理由**：最小改动，零破坏性；两把锁独立，核心写锁不阻塞配置读取；`@Value` 加 `defaultValue` 确保未配置时不报错。

### 2. ConfigData 模型：Map 按 teamId 分组
```java
public class ConfigData {
    private Map<String, List<ConstraintPreset>> constraintPresets = new HashMap<>();
    // key = teamId
}
```
选 Map 而非在 `Team` 内嵌套，原因：config 文件独立于核心数据文件，不需要加载 Team 对象即可读写预设，避免跨文件 join。

**备选**（来自旧 constraint-presets design）：将 `constraintPresets` 加到 `Team` / `TeamData` → 配置数据混回核心文件，失去分离意义，**推翻**。

### 3. ConstraintPreset 模型
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

### 4. 约束预设 API：独立 CRUD 端点
`GET /api/teams/{id}/constraint-presets`、`POST /api/teams/{id}/constraint-presets`、`DELETE /api/teams/{id}/constraint-presets/{presetId}`。与 players、lineups 端点风格一致。

### 5. UTR 重校验：后端实时计算，结果不持久化
`LineupService.getLineups()` 对每条已保存排阵调用 `ConstraintService.validateLineup(lineup, currentPlayers)`，将结果作为 transient 字段附加到响应 DTO。

**理由**：校验逻辑集中在 `ConstraintService`，不复制到前端；每次 GET 保证最新校验；不污染存储层。

### 6. 前端预设选择器：独立组件 + composable
新增 `ConstraintPresetSelector.vue`（下拉 + 保存 + 删除）和 `useConstraintPresets.js`（API 调用），在 `LineupGenerator.vue` 中集成，加载预设时 emit 更新 `PlayerConstraintSelector` 状态。

## Risks / Trade-offs

- **双文件不一致**：预设删除与队伍删除之间宕机可能留下孤立预设。→ 接受，影响极低；`getPresets` 时可过滤不存在的 teamId。
- **球员被删除后预设含旧 ID**：加载预设时过滤不存在球员 ID，前端提示"部分球员已不在队伍中"。
- **测试路径隔离**：`JsonRepositoryTest` 需为 config 文件提供独立 `@TempDir` 路径。→ 改动量小。
- **重校验性能**：历史排阵通常 < 50 条，`validateLineup` 计算量极小，无风险。

## Migration Plan

1. 先实现 `JsonRepository` 双文件扩展（不影响现有功能）
2. 实现 `ConfigData` + `ConstraintPreset` 模型（新增，无破坏）
3. 实现预设 API，读写 `tennis-config.json`
4. 实现 UTR 重校验（GET 响应层，无存储变更）
5. 实现前端预设选择器和历史页标识
6. 重启后端：`tennis-config.json` 自动创建，`tennis-data.json` 完全不变
7. 回滚：删除 `tennis-config.json`，恢复旧代码，数据无损失
