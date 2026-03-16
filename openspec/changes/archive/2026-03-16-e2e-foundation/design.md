## Context

项目已有 185 个单元测试（后端 70 + 前端 115），全部通过，但两个真实 UI 交互 bug 未被发现：

1. 切换队伍后球员列表不刷新
2. 添加球员后左侧计数不更新

根本原因：现有测试策略缺少跨组件集成测试和 E2E 测试，组件间的响应式数据流只在真实浏览器中才能被验证。

当前架构：Vue 3 前端（:5173）通过 `/api` 代理调用 Spring Boot 后端（:8080），数据存储于 JSON 文件。

## Goals / Non-Goals

**Goals:**
- 安装并配置 Playwright，仅使用 Chromium 减少安装体积
- 建立 `frontend/e2e/` 目录结构，包含页面对象模型（POM）
- 为 4 个核心用户流程编写 E2E 测试
- 配置 `webServer` 自动启动 Vite dev server
- 更新 `openspec/config.yaml` 使后续 change 自动包含 E2E 测试任务
- 更新 `CLAUDE.md` 补充 E2E 运行命令

**Non-Goals:**
- CI/CD 集成（不在此 change 范围内）
- Firefox / WebKit 浏览器支持
- 视觉回归测试（截图对比）
- 后端自动启动（后端需手动预先运行）
- 100% 路径覆盖（仅核心 Happy Path）

## Decisions

### 1. 使用 Playwright 而非 Cypress

**选择 Playwright**，原因：
- 原生支持 Windows，无需额外配置
- `@playwright/test` 内置测试运行器，无需额外集成 Vitest
- 页面对象模型（POM）支持更自然
- 并发执行默认启用，速度更快

替代方案 Cypress：配置更复杂，Windows 下偶有稳定性问题。

### 2. 仅安装 Chromium

```bash
npx playwright install chromium
```

Windows 环境下 Playwright 需要下载浏览器二进制，仅装 Chromium 可将下载体积从 ~300MB 降至 ~100MB，满足本项目需求。

### 3. 使用页面对象模型（POM）

每个页面/功能区域封装为独立 class，测试代码只调用 POM 方法，不直接操作 DOM 选择器。

```
frontend/e2e/
  pages/
    TeamManagerPage.js    # 队伍管理页面操作封装
  fixtures/
    test-data.js          # 测试用数据常量
  tests/
    team-management.spec.js
    player-management.spec.js
    team-switch.spec.js
    batch-import.spec.js
```

替代方案（不用 POM）：选择器散落在每个测试中，一旦 UI 结构变化需要修改多处。

### 4. webServer 配置自动启动前端

`playwright.config.js` 使用 `webServer` 选项自动启动 Vite dev server：

```js
webServer: {
  command: 'npm run dev',
  url: 'http://localhost:5173',
  reuseExistingServer: !process.env.CI,
}
```

`reuseExistingServer: true`（本地开发）：如果 dev server 已运行则复用，不重复启动。后端需手动预先启动。

### 5. E2E 与单元测试完全独立

新增 `test:e2e` 脚本，不影响现有 `npm test`（Vitest 单元测试）。

```json
"test:e2e": "playwright test",
"test:e2e:ui": "playwright test --ui"
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| 后端未运行时 E2E 测试失败 | CLAUDE.md 明确说明运行前需先启动后端；测试错误信息能清晰指向连接失败 |
| Windows 路径问题（斜杠/编码） | Playwright 原生支持 Windows；文件上传使用相对路径 |
| 测试数据污染（测试间残留数据） | 每个测试开始前清理相关数据，或使用唯一命名（时间戳后缀）避免冲突 |
| Vite dev server 启动慢导致测试超时 | 配置 `timeout: 30000` 等待服务器就绪 |

## Migration Plan

1. 安装 `@playwright/test` devDependency
2. 下载 Chromium：`npx playwright install chromium`
3. 创建 `frontend/playwright.config.js`
4. 创建 `frontend/e2e/` 目录结构（pages + fixtures + tests）
5. 编写 4 个核心流程测试
6. 验证本地运行通过：`npm run test:e2e`
7. 更新 `openspec/config.yaml` 测试策略
8. 更新 `CLAUDE.md`

无需数据库迁移，无 API 变更，回滚只需删除 `frontend/e2e/` 和 `playwright.config.js`，并还原 `package.json`。

## Open Questions

- 测试数据隔离策略：使用时间戳唯一命名 vs. beforeEach/afterEach 清理？推荐时间戳命名，避免 afterEach 在测试失败时未执行导致残留。
