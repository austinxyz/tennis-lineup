## Why

三个相互关联的问题需要一并解决：①排阵生成时每次手动配置球员约束效率低，无保存机制；②球员 UTR 更新后已保存排阵的合法性无法感知；③`tennis-data.json` 将随配置数据（约束预设等）持续膨胀，配置数据与核心实体数据（Team/Player/Lineup）混在一起不合理。在约束预设功能上线前同步完成存储拆分是最佳时机，避免后续迁移成本。

## What Changes

- **存储层拆分（方案 B）**：将配置类数据从 `tennis-data.json` 分离到独立的 `tennis-config.json`，两文件各有独立 ReadWriteLock，Service 层以上零感知。
- **约束预设保存**：球员约束（excludePlayers / includePlayers / pinPlayers）可命名保存为预设，存入 `tennis-config.json`（`ConfigData`），生成排阵时可选择加载、删除预设。
- **已保存排阵 UTR 合法性重校验**：`GET /api/teams/{id}/lineups` 响应时，用当前球员 UTR 重新校验每条排阵，附加 `currentValid` / `currentViolations` 字段，前端在历史页展示合法 / 失效标识。
- **排阵生成页跳转链接**：新增"查看已保存排阵 →"路由链接。

## Capabilities

### New Capabilities
- `constraint-presets`：约束预设 CRUD（按队伍存储于 `ConfigData`），含 API 端点和前端预设选择器组件。

### Modified Capabilities
- `data-persistence`：存储层从单文件扩展为双文件（`tennis-data.json` 核心 + `tennis-config.json` 配置），`JsonRepository` 新增 `readConfig()` / `writeConfig()` 方法及独立锁。
- `lineup-history`：`GET /api/teams/{id}/lineups` 响应新增 `currentValid` / `currentViolations` 实时重校验字段；前端历史页展示合法性徽标。
- `lineup-generation`：新增约束预设选择器 UI；新增已保存排阵跳转链接。

## Impact

- **后端**：新增 `ConfigData` 模型、`ConstraintPreset` 模型；`JsonRepository` 扩展双文件；新增 `ConstraintPresetService` + `ConstraintPresetController`；`LineupService.getLineups` 附加实时校验字段；`application.yml` 新增 `storage.config-file`。
- **前端**：新增 `ConstraintPresetSelector.vue` 组件 + `useConstraintPresets.js`；`LineupGenerator.vue` 集成预设选择器和跳转链接；`LineupHistory.vue` 展示合法性标识。
- **数据**：`tennis-data.json` 结构完全不变；`tennis-config.json` 首次启动自动创建（空文件）。
- **测试**：`JsonRepositoryTest` 需提供两个独立临时路径；新增预设服务测试和排阵校验测试；新增前端组件测试。
