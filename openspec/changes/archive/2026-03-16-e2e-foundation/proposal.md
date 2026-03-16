## Why

现有 185 个单元测试全部通过，但两个真实 UI 交互 bug 未被发现（切换队伍球员不刷新、添加球员后左侧计数不更新），根本原因是测试策略缺少跨组件集成测试和 E2E 测试。需要建立 Playwright E2E 测试基础，并将 E2E 测试纳入后续每个 change 的标准测试任务。

## What Changes

- 安装并配置 Playwright（支持 Chromium，与现有 Vite 开发服务器集成）
- 建立 E2E 测试目录结构和公共工具（页面对象模型 / helpers）
- 为现有核心用户流程补写 E2E 测试：
  - 队伍管理：创建、删除队伍
  - 球员管理：添加球员（验证左侧计数同步）、编辑、删除球员
  - 队伍切换：切换队伍后球员列表正确刷新
  - 批量导入：上传 CSV 文件，验证导入结果展示
- 更新 `openspec/config.yaml` 测试策略，要求后续每个 change 的 tasks 包含对应 E2E 测试任务
- 更新 `CLAUDE.md` 补充 E2E 测试运行命令

## Capabilities

### New Capabilities

- `e2e-testing`: Playwright E2E 测试框架配置及核心用户流程测试套件

### Modified Capabilities

（无现有 spec 需要修改）

## Impact

- **新增依赖**：`@playwright/test`（devDependency，仅前端）
- **新增文件**：`frontend/e2e/`（测试文件）、`frontend/playwright.config.js`
- **配置变更**：`frontend/package.json` 新增 `test:e2e` 脚本
- **流程变更**：后续 change 的 tasks.md 须包含 E2E 测试任务
- **无 API 变更、无后端变更、无破坏性变更**

## Success Criteria

- `npm run test:e2e` 可在本地成功运行，所有 E2E 测试通过
- 覆盖 4 个核心用户流程（队伍管理、球员管理、队伍切换、批量导入）
- 测试在真实浏览器中执行（不依赖 jsdom mock）
- `openspec/config.yaml` 测试策略更新完毕，后续 change propose 时自动包含 E2E 任务

## Risks

| 风险 | 缓解措施 |
|------|---------|
| Playwright 需要真实前后端同时运行 | 使用 `webServer` 配置自动启动 Vite dev server；后端需手动预启动 |
| Windows 环境 Playwright 浏览器下载慢 | 只安装 Chromium（`--browser=chromium`），减少下载体积 |
| E2E 测试速度慢影响开发体验 | E2E 与单元测试分开运行（独立 npm script），不强制每次提交都跑 |

## Effort

约 3-4 小时（Playwright 配置 1h + 核心流程 E2E 测试 2-3h + config 更新 0.5h）
