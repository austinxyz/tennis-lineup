# 网球排阵管理系统

基于 UTR（Universal Tennis Rating）的网球团体赛排阵管理工具，支持队伍与球员管理、智能排阵生成、对手策略分析，并集成 AI 推荐与逐线评析。支持桌面与移动端响应式布局。

**线上演示：** https://tennis-lineup.fly.dev/

## 功能特性

### 队伍与球员管理
- **队伍 CRUD** — 创建、编辑、删除队伍，维护队伍名称与时间戳
- **球员 CRUD** — 添加球员、维护 UTR / 实际 UTR / 性别 / 已验证状态 / 个人主页链接
- **批量导入** — 从 CSV / JSON 文件批量导入球员，含校验与错误报告
- **批量 UTR 编辑** — 全员内联编辑 UTR，一键保存
- **球员备注** — 为每位球员记录打法风格等个人备注
- **搭档笔记** — 记录己方或对手球员搭档配合情况，用于 AI 分析上下文

### 排阵生成
- **智能生成** — 根据 UTR 自动生成最优配对方案，支持平衡 / 激进 / 自定义 AI 策略，一次返回最多 6 个候选方案
- **位置约束** — 为每位球员设置约束（中立 / 不上 / 一定上 / D1–D4 固定位），批量预设可保存复用
- **手动换人** — 生成结果中手动互换任意球员，自动按 UTR 重排位置（D1–D4）
- **排阵保存与去重** — 显式保存排阵，按「配对组合」去重（同 8 人不同配对视为不同排阵）
- **内存限制** — 生产环境内存敏感，可选球员池限制为 UTR 最高的 18 人（显著降低 C(n,8) 组合量）

### 已保存排阵（`/teams/:id/lineups`）
- **自定义名称与备注** — 内联编辑排阵名称（如「主力阵容」「备选」）与备注，Enter/失焦保存，空值清除
- **上移/下移排序** — ↑/↓ 按钮调整排阵顺序，首位自动标记「⭐ 首选」
- **调整配对（Swap）** — 展开折叠面板，不改动球员的前提下互换两人位置
- **替换球员** — 从队伍球员下拉中选择替换，即时校验 UTR / 搭档差 / 位置顺序 / 性别约束，违规阻止保存
- **合法性自检** — 球员被删除或 UTR 变动后，排阵显示「已失效」徽章与具体违规原因
- **导出 / 导入** — JSON 格式导出队伍所有排阵（带 envelope 元数据），跨环境导入时按球员**姓名**映射解决 ID 冲突，按「配对组合」去重

### 对手策略分析（`/opponent-analysis`）
- **最佳三阵（Mode A）** — 选择对手已保存排阵，系统自动从己方所有已保存排阵中找出胜率最高的前 3 候选，两列展示「算法推荐（UTR）」与「AI 推荐」
- **逐线对比（Mode B）** — 选定己方与对手各一份排阵，详细呈现每线胜率、UTR 差值
- **AI 排阵推荐** — 基于实际 UTR 和搭档笔记，由 AI 给出推荐排阵与理由
- **AI 逐线评析** — 对选定双方排阵生成逐线策略评析，使用实际 UTR 差值，结合搭档笔记；AI 不可用时自动回退到规则兜底
- **胜率计算** — 全程使用实际 UTR（`actualUtr`）计算，无实际 UTR 时回退官方 UTR
- **搭档笔记上下文** — 我方与对手搭档笔记一并送入 AI，提高推荐相关性

### 移动端支持
- 全页面响应式布局，断点 `lg` (1024px) 统一切换
- Mobile 顶部 AppHeader：汉堡菜单 + 返回按钮 + 标题 + 页面 actions
- 队伍管理：列表 / 详情 URL 驱动二选一，避免双栏挤压
- 球员卡片：可展开行（Verified 标签、备注、编辑/删除按钮），支持键盘操作
- 排阵卡片：每对 D1–D4 每人一行，带性别徽章与独立 UTR 显示
- 对手分析：结果页双 Tab（算法 Top 3 / AI 推荐），每条 line 3 行纵向布局，Top 1 展开、Top 2/3 点击展开

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2 + Java 17 |
| 前端 | Vue 3 + Vite + Tailwind CSS |
| 存储 | JSON 文件（无需数据库） |
| AI | 智谱 AI GLM-4（ZhipuAI），带规则兜底 |
| 测试 | JUnit 5 + Mockito（后端）/ Vitest + Vue Test Utils + Playwright E2E（前端）|

## 项目结构

```
tennis/
├── backend/          # Spring Boot 后端
│   └── src/
│       ├── main/java/com/tennis/
│       │   ├── controller/   # REST 控制器
│       │   ├── service/      # 业务逻辑（含 AI 集成）
│       │   ├── repository/   # JSON 文件读写
│       │   ├── model/        # 数据模型
│       │   └── exception/    # 异常处理
│       └── test/             # 后端单元/集成测试
├── frontend/         # Vue 3 前端
│   └── src/
│       ├── views/        # 页面组件
│       ├── components/   # 可复用组件
│       ├── composables/  # Vue 组合式函数
│       └── router/       # 路由配置
│   └── e2e/              # Playwright E2E 测试
├── data/             # JSON 数据文件（运行时生成）
├── docs/             # 项目文档
└── openspec/         # 功能规格文档
    ├── specs/        # 各功能 spec（当前版本）
    └── changes/archive/  # 已归档的变更记录
```

## 主要页面

| 路由 | 说明 |
|------|------|
| `/` | 队伍列表 |
| `/teams/:id` | 队伍详情（球员管理、个人备注、搭档笔记） |
| `/lineup-generator` | 排阵生成器（生成、调整、保存、约束预设） |
| `/teams/:id/lineups` | 已保存排阵（命名 / 备注 / 排序 / swap / 替换球员 / 导出 / 导入） |
| `/opponent-analysis` | 对手策略分析（最佳三阵 / 逐线对比 / AI 推荐） |

## 前置条件

- **Java 17+**
- **Maven 3.6+**（或使用项目内 mvnw wrapper）
- **Node.js 18+** + **npm**

## 开发环境搭建

### 1. 克隆项目

```bash
git clone https://github.com/austinxyz/tennis-lineup.git
cd tennis
```

### 2. 启动后端

```bash
cd backend

# Windows（需设置 UTF-8 编码）
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
mvn spring-boot:run

# macOS / Linux
JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 mvn spring-boot:run
```

后端启动后监听 `http://localhost:8080`。数据文件自动创建于 `data/tennis-data.json`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动后访问 `http://localhost:5173`。前端通过 Vite 代理将 `/api` 请求转发到 `localhost:8080`。

## 运行测试

### 后端测试

```bash
cd backend

# Windows
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
mvn test

# macOS / Linux
JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 mvn test
```

当前覆盖：253+ 个测试（Controller / Service / Repository，含 JsonRepository 并发安全与原子写入场景）全部通过。

### 前端单元测试

```bash
cd frontend
npm test              # 单次运行
npm run test:watch    # 监听模式
npm run test:coverage # 覆盖率报告
```

当前覆盖：442 个测试（Composables / Components / Views，含移动端布局与响应式组件）全部通过。

### 前端 E2E 测试

```bash
cd frontend
npm run test:e2e      # 无头模式（需后端和前端 dev server 同时运行）
npm run test:e2e:ui   # 交互式 UI 模式
```

当前覆盖：53 个 E2E 测试（Playwright / Chromium，含移动端 viewport 375x667）全部通过。

> **注意**：若环境存在 socks5 代理（如企业 VPN），`npx playwright` 会抛 `Protocol 'socks5:' not supported`。跑 E2E 前先 `unset all_proxy ALL_PROXY http_proxy HTTP_PROXY https_proxy HTTPS_PROXY`。

## 生产构建

```bash
# 构建前端静态资源
cd frontend
npm run build         # 输出到 frontend/dist/

# 打包后端（含静态资源）
cd backend
mvn package
java -Dfile.encoding=UTF-8 -jar target/tennis-backend-1.0.0.jar
```

## 部署（Fly.io）

项目已配置 `fly.toml` + `Dockerfile`（多阶段构建：前端 Vite → 后端 Maven jar → 最终 JRE image）。

```bash
flyctl deploy
```

- **容器规格**：`shared-cpu-1x`，256MB，区域 `nrt`（东京）
- **数据持久化**：Fly Volume 挂载到 `/data`，后端读写 `/data/tennis-data.json`
- **Terraform 基础设施**：`terraform/` 下含 Fly provider lock；手动备份脚本见 `scripts/backup-fly-data.sh`

部署后建议冒烟测试：

```bash
curl -I https://tennis-lineup.fly.dev/            # 预期 200
curl -I https://tennis-lineup.fly.dev/api/teams   # 预期 200
```

## 配置

后端配置文件：`backend/src/main/resources/application.yml`

```yaml
server:
  port: 8080            # 后端端口

storage:
  data-file: ./data/tennis-data.json   # 数据文件路径（相对于启动目录）
```

## 常见问题

**端口被占用**

```bash
# Windows
taskkill /F /IM java.exe
taskkill /F /IM node.exe
```

**中文乱码**

确保启动后端时设置了 `-Dfile.encoding=UTF-8`。

**前端请求 API 失败**

确认后端已启动在 8080 端口，Vite 代理配置见 `frontend/vite.config.js`。

## 相关文档

- [API 接口文档](docs/api.md)
- [数据导入格式说明](docs/import-format.md)
- [需求与设计文档](docs/tennis-lineup-app-design.md)
- [前端最佳实践](docs/frontend-best-practices.md)
- [Claude Code 协作指南](CLAUDE.md)
- [功能规格](openspec/specs/)（按 capability 拆分的 spec；`openspec/changes/archive/` 含历史变更）
- [开发日志](docs/log/)（按日期 `YYYY-MM-DD.md` 组织）
