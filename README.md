# 网球排阵管理系统

基于 UTR（Universal Tennis Rating）的网球团体赛排阵管理工具，支持队伍与球员管理、智能排阵生成、对手策略分析，并集成 AI 推荐与逐线评析。

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
- **排阵保存与去重** — 显式保存排阵，自动跳过 8 人组合相同的重复排阵
- **排阵历史** — 查看所有已保存排阵（含当前 UTR、实际 UTR、性别），显示约束违规提示，支持删除

### 对手策略分析（`/opponent-analysis`）
- **最佳三阵（Mode A）** — 选择对手已保存排阵，系统自动找出胜率最高的己方前三候选，逐线显示 UTR 与实际 UTR 对比
- **逐线对比（Mode B）** — 选定己方与对手各一份排阵，详细呈现每线胜率、UTR 差值
- **AI 排阵推荐** — 基于实际 UTR 和搭档笔记，由 AI 给出最优排阵方案与推荐理由
- **AI 逐线评析** — 对选定双方排阵生成逐线策略评析，使用实际 UTR 差值，结合搭档笔记；AI 不可用时自动回退到规则兜底
- **胜率计算** — 全程使用实际 UTR（`actualUtr`）计算，无实际 UTR 时回退官方 UTR

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
| `/teams/:id` | 队伍详情（球员管理、备注、搭档笔记） |
| `/lineup-generator` | 排阵生成器（生成、调整、保存、约束预设） |
| `/teams/:id/lineups` | 排阵历史（查看、删除已保存排阵） |
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

当前覆盖：228 个测试（Controller / Service / Repository）全部通过。

### 前端单元测试

```bash
cd frontend
npm test              # 单次运行
npm run test:watch    # 监听模式
npm run test:coverage # 覆盖率报告
```

当前覆盖：327 个测试（Composables / Components / Views）全部通过。

### 前端 E2E 测试

```bash
cd frontend
npm run test:e2e      # 无头模式（需后端和前端 dev server 同时运行）
npm run test:e2e:ui   # 交互式 UI 模式
```

当前覆盖：44 个 E2E 测试（Playwright / Chromium）全部通过。

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
