## Why

网球排阵管理系统需要基础的数据管理能力来支持后续的排阵生成和对手分析功能。当前系统没有任何队伍或球员数据，用户无法进行任何有意义的操作。组队管理是最高优先级（3.1）的功能，为整个系统提供数据基础。

## What Changes

实现完整的队伍和球员管理功能，包括：

- **队伍 CRUD**：创建、查看、更新队名、删除队伍
- **球员管理**：添加、编辑、删除球员
- **批量导入**：支持 CSV 和 JSON 格式批量导入球员数据
- **数据持久化**：使用 JSON 文件存储所有队伍和球员信息

## Capabilities

### New Capabilities

- `team-crud`: 提供队伍的完整 CRUD 操作，支持创建队伍、获取队伍列表、更新队名、删除队伍
- `player-crud`: 提供球员的增删改查操作，支持添加球员、更新球员信息、删除球员
- `batch-import`: 支持批量导入球员数据，支持 CSV 和 JSON 两种格式，包含数据验证和错误处理
- `data-persistence`: 基础的 JSON 文件存储服务，支持线程安全的读写操作

### Modified Capabilities

（无，这是全新功能）

## Impact

**代码影响**：
- 新增后端服务：TeamService、PlayerService、BatchImportService、JsonRepository
- 新增后端模型：Team、Player、TeamData
- 新增后端控制器：TeamController
- 新增前端布局：MainLayout.vue（三栏容器）、TeamManagerView.vue（两栏内容）
- 新增前端组件：NavSidebar.vue（功能导航）、TeamListPanel.vue（队伍列表+操作面板）
- 新增前端页面：TeamDetail.vue、HomeView.vue（空状态）
- 批量导入集成于 TeamListPanel modal，不作为独立页面
- 新增前端 Composables：useTeams、usePlayers、useBatchImport、useApi

**API 影响**：
- 新增 REST 端点：
  - GET /api/teams
  - GET /api/teams/{id}
  - POST /api/teams
  - PUT /api/teams/{id}
  - DELETE /api/teams/{id}
  - GET /api/teams/{id}/players
  - POST /api/teams/{id}/players
  - PUT /api/teams/{id}/players/{playerId}
  - DELETE /api/teams/{id}/players/{playerId}
  - POST /api/teams/import

**依赖影响**：
- Spring Boot（后端框架）
- Vitest + @vue/test-utils（前端测试）
- 无外部 API 依赖（此阶段不涉及 AI 调用）
