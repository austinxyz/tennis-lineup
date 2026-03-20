## Why

排阵生成时每次手动配置球员约束（谁不上、谁固定位置）效率低；而球员 UTR 更新后，已保存排阵可能已违反约束，用户无法直观知晓；另外从排阵生成页跳转到已保存排阵页需要手动导航。这三个问题共同影响日常生成排阵的工作流效率。

## What Changes

- **约束配置保存**：球员约束（excludePlayers / includePlayers / pinPlayers）可命名保存为预设，生成排阵时可直接选择已有预设，支持创建、覆盖、删除预设。
- **已保存排阵 UTR 合法性重校验**：每次访问已保存排阵页时，用当前球员 UTR 对所有已保存排阵进行重新校验，标识合法 / 不合法状态及违规原因。
- **排阵生成页跳转链接**：在排阵生成页顶部或生成结果区域提供"查看已保存排阵"快捷链接，一键跳转。

## Capabilities

### New Capabilities
- `constraint-presets`: 约束预设的 CRUD（保存、加载、删除），按队伍存储，支持多个命名预设。

### Modified Capabilities
- `lineup-history`: 增加 UTR 合法性重校验逻辑——访问已保存排阵页时，用当前球员 UTR 实时重校验每条排阵，在列表中标识合法 / 不合法状态。
- `lineup-generation`: 增加"查看已保存排阵"快捷跳转链接；增加约束预设选择器（加载 / 保存预设）。

## Impact

- **后端**：新增 `ConstraintPreset` 模型；`TeamData` 增加 `constraintPresets` 字段；新增 `/api/teams/{id}/constraint-presets` CRUD 端点；`LineupController` 或前端直接调 `ConstraintService.validateLineup` 对已保存排阵进行重校验（也可在前端调现有 generate 接口的校验逻辑）。
- **前端**：`LineupGenerator.vue` 新增预设选择器组件；`LineupHistory.vue`（或等价页）增加合法性标识；新增跳转链接。
- **数据**：`tennis-data.json` 增加 `constraintPresets` 数组，结构向后兼容（无预设时为空数组）。
- **风险**：已保存排阵重校验逻辑需与生成时的 `ConstraintService.validateLineup` 保持一致，避免校验标准不统一。
