## 1. 安装与配置 Playwright

- [x] 1.1 在 `frontend/` 下安装 `@playwright/test` devDependency：`npm install -D @playwright/test`
- [x] 1.2 下载 Chromium 浏览器：`npx playwright install chromium`
- [x] 1.3 创建 `frontend/playwright.config.js`，配置 Chromium、webServer（指向 localhost:5173）、`reuseExistingServer: true`、超时 30s
- [x] 1.4 在 `frontend/package.json` 新增脚本：`"test:e2e": "playwright test"` 和 `"test:e2e:ui": "playwright test --ui"`

## 2. 建立 E2E 目录结构与 POM

- [x] 2.1 创建目录 `frontend/e2e/pages/`、`frontend/e2e/fixtures/`、`frontend/e2e/tests/`
- [x] 2.2 创建 `frontend/e2e/pages/TeamManagerPage.js`，封装以下操作：`createTeam(name)`、`deleteTeam(name)`、`selectTeam(name)`、`getTeamPlayerCount(name)`
- [x] 2.3 创建 `frontend/e2e/pages/PlayerDetailPage.js`，封装：`addPlayer(data)`、`editPlayer(name, data)`、`deletePlayer(name)`、`getPlayerNames()`
- [x] 2.4 创建 `frontend/e2e/fixtures/test-data.js`，定义测试用常量（队伍名前缀、球员数据样本、CSV 文件内容）

## 3. 编写核心流程 E2E 测试

- [x] 3.1 创建 `frontend/e2e/tests/team-management.spec.js`：测试创建队伍（验证出现在列表）、删除队伍（验证从列表消失）
- [x] 3.2 创建 `frontend/e2e/tests/player-management.spec.js`：测试添加球员后左侧计数+1、编辑球员信息更新、删除球员后消失
- [x] 3.3 创建 `frontend/e2e/tests/team-switch.spec.js`：准备两个队伍各含不同球员，验证切换队伍后右侧列表正确更新
- [x] 3.4 创建 `frontend/e2e/tests/batch-import.spec.js`：上传有效 CSV 验证成功计数、上传含无效行 CSV 验证失败计数和错误信息展示

## 4. 验证与收尾

- [x] 4.1 确保后端运行，执行 `npm run test:e2e`，所有测试通过
- [x] 4.2 更新 `openspec/config.yaml` 测试策略，要求后续 change 的 tasks 包含对应 E2E 测试任务
- [x] 4.3 更新 `CLAUDE.md` 补充 E2E 运行命令（`npm run test:e2e`、`npm run test:e2e:ui`）及前置条件（后端须先启动）
