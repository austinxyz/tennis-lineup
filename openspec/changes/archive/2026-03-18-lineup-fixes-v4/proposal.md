## Why

三个已知问题影响排阵生成的实用性（算法低效、性别/UTR 不显示），加上自动持久化设计不合理：用户希望先浏览生成结果再决定保留哪个，而不是每次生成都自动保存。

## What Changes

- **算法重构**：枚举 8 人子集时按总 UTR 接近 40.5 从大到小排列；先处理 top-20 子集检查约束，若有效排阵 < 6 个则扩展到 top-40
- **历史排阵数据补全**：`getLineupsByTeam` 读取保存排阵时，从球员数据中补全缺失的 `player1Utr`/`player2Utr`/`player1Gender`/`player2Gender` 字段（老格式兼容）
- **性别显示**：`LineupCard` 对 null gender 做防御处理（不再默认显示"男"）
- **去除自动持久化**：生成排阵时不再自动保存到 JSON 文件；用户在结果页手动点击"保留"保存选中排阵
- **新增手动保存**：前端排阵结果卡片加"保留此排阵"按钮，点击后调用保存 API
- **排阵历史页独立**：专门的排阵历史浏览页（已有 LineupHistory 相关逻辑），支持删除

## Capabilities

### New Capabilities
- `lineup-manual-save`: 用户手动从生成结果中选择并保留排阵（替代自动保存）

### Modified Capabilities
- `lineup-generation`: 算法改为 top-20/top-40 子集批次处理；去除生成时的自动持久化
- `lineup-history`: 读取历史排阵时补全 player UTR 和 gender 字段（老数据兼容）

## Impact

- **后端**：`LineupGenerationService.generateCandidates` 算法逻辑；`LineupService.generateMultipleAndSave` 移除持久化逻辑；新增 `LineupService.saveLineup(teamId, lineup)` 接口；`LineupService.getLineupsByTeam` 加字段补全
- **前端**：`LineupResultGrid.vue` 各卡片加"保留"按钮；`useLineup.js` 新增 `saveLineup` 函数；历史页无需变更（已有删除功能）
- **API**：新增 `POST /api/teams/{id}/lineups/{lineupId}/save` 或 `POST /api/lineups` 接口
- **存储**：不影响现有 JSON 格式；老 lineups 数据通过 enrichment 向前兼容
