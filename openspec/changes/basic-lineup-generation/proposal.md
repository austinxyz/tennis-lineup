## Why

组队管理功能（3.1）已完成，队伍和球员数据已可完整录入。本次实现 3.2 基础排阵生成功能：用户选择队伍和策略后，系统自动生成满足所有硬约束的双打阵容，是整个赛前备战流程的核心功能。

## What Changes

- **新增排阵生成 API**：`POST /api/lineups/generate`，接受 teamId 和策略参数，返回生成的排阵
- **新增排阵历史 API**：`GET /api/teams/{id}/lineups`，`DELETE /api/lineups/{id}`
- **新增后端服务**：`ConstraintService`（硬约束验证）、`LineupService`（排阵生成逻辑）、`AiService`（AI 策略选择）
- **扩展数据模型**：在 `Team` 中支持 `lineups` 字段，新增 `Lineup`、`Pair` 模型类
- **新增前端页面**：`LineupGenerator.vue`（排阵生成页），包含队伍选择、策略选择、结果展示
- **新增前端组件**：`StrategySelector.vue`（策略选择器）、`LineupCard.vue`（排阵展示卡片）
- **新增前端路由**：`/lineup` 路由，并在侧边栏添加「排阵生成」入口

## Capabilities

### New Capabilities

- `lineup-generation`: 排阵生成核心逻辑——回溯算法枚举所有合法配对组合，硬约束过滤，AI/启发式策略选优，返回最终阵容
- `lineup-history`: 排阵历史管理——保存到队伍数据、查询历史列表、删除历史排阵

### Modified Capabilities

- `team-management`: 扩展 Team/Lineup 数据模型，为现有 `GET /api/teams/{id}` 响应中增加 `lineups` 字段

## Impact

- **后端**：新增 `LineupController`、`LineupService`、`ConstraintService`、`AiService`；扩展 `JsonRepository` 支持排阵读写；新增 `Lineup`、`Pair`、`GenerateLineupRequest`、`LineupResponse` 数据类
- **前端**：新增 `LineupGenerator.vue`、`StrategySelector.vue`、`LineupCard.vue`；新增 `useLineup.js` composable；扩展路由配置及侧边栏导航
- **外部依赖**：集成智谱 AI SDK（`cn.bigmodel.openapi:oapi-java-sdk:4.0.0`），需配置 `ZHIPU_API_KEY` 环境变量
- **数据文件**：`tennis-data.json` 中的 Team 对象新增 `lineups` 数组字段（向后兼容，无则视为空数组）
