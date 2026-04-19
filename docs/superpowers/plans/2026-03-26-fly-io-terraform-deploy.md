# Fly.io + Terraform 部署计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Spring Boot 后端 + Vue3 前端打包为单一 Docker 镜像，通过 Terraform 部署到 Fly.io，持久化 JSON 数据文件，并将所有变量集中管理在一个 `terraform.tfvars` 文件中。

**Architecture:** 多阶段 Dockerfile 将前端编译产物复制进 Spring Boot 的 `resources/static/`，由 Spring Boot 同时 serve 静态页面和 API；Terraform 管理 Fly.io App、持久化 Volume、公网 IP 和 Machine；`application.yml` 通过环境变量接收数据文件路径和 API Key。

**Tech Stack:** Docker multi-stage build、Spring Boot 3.2 (Java 17)、Vue 3 + Vite、Fly.io (`fly-apps/fly` Terraform provider ~0.1)、Terraform >= 1.5

---

## 用户需要提供的变量

**填写后保存为 `terraform/terraform.tfvars`（此文件不会提交 Git）：**

```hcl
# Fly.io 账号 API Token
# 获取方式：flyctl auth token
fly_api_token = "fo1_xxxxxxxxxxxxxxxxxxxx"

# Fly.io 组织名（个人账号默认是你的用户名）
# 获取方式：flyctl orgs list
fly_org = "your-fly-username"

# 应用名（全局唯一，会成为默认域名 <app_name>.fly.dev）
fly_app_name = "tennis-lineup"

# 部署区域（hkg=香港, nrt=东京, sin=新加坡）
fly_region = "hkg"

# 智谱 AI API Key（不填则 AI 功能降级为规则兜底）
zhipu_api_key = "your-zhipu-api-key"
```

---

## 文件结构

```
tennis/
├── Dockerfile                                        # 新建：多阶段构建
├── .dockerignore                                     # 新建：排除不必要文件
├── terraform/
│   ├── main.tf                                       # 新建：Fly.io 资源定义
│   ├── variables.tf                                  # 新建：变量声明
│   ├── outputs.tf                                    # 新建：输出（App URL 等）
│   ├── terraform.tfvars                              # 新建：用户填写（不提交 Git）
│   └── .gitignore                                    # 新建：排除 tfvars 和 state
├── backend/src/main/resources/
│   ├── application.yml                               # 修改：移除 include: local，storage 路径改环境变量
│   └── application-local.yml                        # 已存在（含本地密钥，不提交，不进 Docker）
└── .gitignore                                        # 修改：追加 terraform.tfvars
```

---

## Task 1：修改 application.yml

**Files:**
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1.1：移除 `include: local`，改为环境变量驱动的 storage 路径**

将 `application.yml` 修改为：

```yaml
server:
  port: ${SERVER_PORT:8080}
  servlet:
    encoding:
      charset: UTF-8
      force: true

spring:
  application:
    name: tennis-lineup-app
  mandatory-file-encoding: UTF-8
  # 注意：移除了 profiles.include: local
  # 本地开发请通过 IDE 或命令行设置 spring.profiles.active=local

storage:
  data-file: ${DATA_FILE_PATH:./data/tennis-data.json}
  config-file: ${CONFIG_FILE_PATH:./data/tennis-config.json}

zhipu:
  api:
    key: ${ZHIPU_API_KEY:}
```

> **说明：** 移除 `spring.profiles.include: local` 是关键。原来该行会导致 `application-local.yml`（含硬编码密钥）在所有环境下自动加载，覆盖环境变量。现在本地开发需要手动激活 local profile（见 Step 1.2）。

- [ ] **Step 1.2：确认本地开发仍可正常启动**

本地启动后端时，加上 `-Dspring.profiles.active=local` 激活本地 profile：

```bash
cd backend
JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8" \
  /c/Users/lorra/tools/apache-maven-3.9.6/bin/mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

Expected：后端正常启动，`application-local.yml` 中的 Zhipu Key 被读取。

- [ ] **Step 1.3：Commit**

```bash
git add backend/src/main/resources/application.yml
git commit -m "config: remove auto-include of local profile; use env vars for storage paths"
```

---

## Task 2：创建 Dockerfile 和 .dockerignore

**Files:**
- Create: `Dockerfile`（项目根目录）
- Create: `.dockerignore`

- [ ] **Step 2.1：创建 `.dockerignore`**

```
# 开发环境本地配置（含硬编码密钥，绝对不能进镜像）
backend/src/main/resources/application-local.yml

# 构建产物和依赖
frontend/node_modules
frontend/dist
backend/target

# 数据和运行时文件
data

# 版本控制
.git
.gitignore

# 文档和规格
*.md
terraform
docs
openspec
scripts
```

- [ ] **Step 2.2：创建 `Dockerfile`**

```dockerfile
# ── Stage 1: 构建前端 ──────────────────────────────────────────
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci --prefer-offline
COPY frontend/ ./
RUN npm run build

# ── Stage 2: 构建后端（含前端静态文件）────────────────────────
FROM maven:3.9.6-eclipse-temurin-17-alpine AS backend-build
WORKDIR /app/backend
# 先复制 pom.xml 利用 Docker 层缓存，避免每次重新下载依赖
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -q
# 复制前端产物到 Spring Boot static 目录（Spring Boot 自动 serve）
COPY --from=frontend-build /app/frontend/dist src/main/resources/static/
# 复制后端源码并构建（跳过测试，CI/CD 已在推送前运行）
COPY backend/src ./src
RUN mvn package -DskipTests -q

# ── Stage 3: 最小化运行时镜像 ──────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 创建数据目录（Fly.io Volume 会挂载到此路径）
RUN mkdir -p /data
COPY --from=backend-build /app/backend/target/tennis-backend-1.0.0.jar app.jar

# 默认环境变量（可在 Fly.io 或 docker run 中覆盖）
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
ENV DATA_FILE_PATH=/data/tennis-data.json
ENV CONFIG_FILE_PATH=/data/tennis-config.json
ENV SERVER_PORT=8080

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2.3：本地验证 Docker 构建**

```bash
cd /c/Users/lorra/projects/tennis
docker build -t tennis-lineup:local .
```

Expected：三个 stage 全部成功，最终镜像约 200–350MB。如果失败，检查 Maven 和 Node 版本兼容性。

- [ ] **Step 2.4：本地运行容器，验证前后端均正常**

```bash
docker run --rm -p 8080:8080 \
  -e ZHIPU_API_KEY="" \
  -v "$(pwd)/data:/data" \
  tennis-lineup:local
```

浏览器访问 `http://localhost:8080`，确认：
- 前端页面正常加载（Vue 路由 `/`）
- `http://localhost:8080/api/teams` 返回 JSON

- [ ] **Step 2.5：Commit**

```bash
git add Dockerfile .dockerignore
git commit -m "build: add multi-stage Dockerfile for Fly.io deployment"
```

---

## Task 3：创建 Terraform 配置

**Files:**
- Create: `terraform/variables.tf`
- Create: `terraform/main.tf`
- Create: `terraform/outputs.tf`
- Create: `terraform/terraform.tfvars`（用户填写，不提交）
- Create: `terraform/.gitignore`

- [ ] **Step 3.1：创建 `terraform/.gitignore`**

```
# 用户密钥，绝对不提交
terraform.tfvars

# Terraform 运行时文件
.terraform/
*.tfstate
*.tfstate.backup

# 注意：.terraform.lock.hcl 故意不排除，应提交以锁定 provider 版本
```

- [ ] **Step 3.2：创建 `terraform/variables.tf`**

```hcl
variable "fly_api_token" {
  description = "Fly.io API Token (run: flyctl auth token)"
  type        = string
  sensitive   = true
}

variable "fly_org" {
  description = "Fly.io organization slug (run: flyctl orgs list)"
  type        = string
}

variable "fly_app_name" {
  description = "App name — becomes <app_name>.fly.dev (must be globally unique on Fly.io)"
  type        = string
}

variable "fly_region" {
  description = "Fly.io deployment region code (hkg=Hong Kong, nrt=Tokyo, sin=Singapore)"
  type        = string
  default     = "hkg"
}

variable "zhipu_api_key" {
  description = "Zhipu AI API Key (leave empty to use rule-based fallback)"
  type        = string
  sensitive   = true
  default     = ""
}

variable "memory_mb" {
  description = "Machine memory in MB (256 = free tier)"
  type        = number
  default     = 256
}

variable "volume_size_gb" {
  description = "Persistent volume size in GB (stores tennis-data.json)"
  type        = number
  default     = 1
}
```

- [ ] **Step 3.3：创建 `terraform/main.tf`**

```hcl
terraform {
  required_version = ">= 1.5"
  required_providers {
    fly = {
      source  = "fly-apps/fly"
      version = "~> 0.1"
    }
  }
}

provider "fly" {
  fly_api_token = var.fly_api_token
}

# ── Fly App ───────────────────────────────────────────────────
resource "fly_app" "tennis" {
  name = var.fly_app_name
  org  = var.fly_org
}

# ── 公网 IP ───────────────────────────────────────────────────
resource "fly_ip" "ipv4" {
  app  = fly_app.tennis.name
  type = "v4"

  depends_on = [fly_app.tennis]
}

resource "fly_ip" "ipv6" {
  app  = fly_app.tennis.name
  type = "v6"

  depends_on = [fly_app.tennis]
}

# ── 持久化 Volume（存放 tennis-data.json 和 tennis-config.json）
resource "fly_volume" "data" {
  name   = "tennis_data"
  app    = fly_app.tennis.name
  size   = var.volume_size_gb
  region = var.fly_region

  depends_on = [fly_app.tennis]
}

# ── Machine（应用实例）───────────────────────────────────────
# 注意：fly-apps/fly v0.1.x 的 mounts/services/checks 使用 HCL block 语法，
#       不是 list-of-map 字面量。
resource "fly_machine" "app" {
  app    = fly_app.tennis.name
  region = var.fly_region
  name   = "${var.fly_app_name}-app"

  # 镜像在 Task 5 中由 docker push + flyctl deploy 推送
  image = "registry.fly.io/${var.fly_app_name}:latest"

  cpus     = 1
  memorymb = var.memory_mb

  env = {
    DATA_FILE_PATH    = "/data/tennis-data.json"
    CONFIG_FILE_PATH  = "/data/tennis-config.json"
    ZHIPU_API_KEY     = var.zhipu_api_key
    SERVER_PORT       = "8080"
    JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
  }

  # block 语法（非 = [...] 赋值）
  mounts {
    volume = fly_volume.data.id
    path   = "/data"
  }

  services {
    ports {
      port     = 443
      handlers = ["tls", "http"]
    }
    ports {
      port     = 80
      handlers = ["http"]
    }
    protocol      = "tcp"
    internal_port = 8080

    # 健康检查：使用 /api/teams（首次部署前已由 Task 5.3 写入初始数据）
    checks {
      type     = "http"
      interval = "15s"
      timeout  = "10s"
      path     = "/api/teams"
      method   = "GET"
    }
  }

  depends_on = [fly_volume.data, fly_app.tennis]
}
```

- [ ] **Step 3.4：创建 `terraform/outputs.tf`**

```hcl
output "app_url" {
  description = "Application URL"
  value       = "https://${fly_app.tennis.name}.fly.dev"
}

output "app_name" {
  description = "Fly.io app name"
  value       = fly_app.tennis.name
}

output "ipv4" {
  description = "Public IPv4 address"
  value       = fly_ip.ipv4.address
}
```

- [ ] **Step 3.5：创建 `terraform/terraform.tfvars`（用户填写）**

```hcl
fly_api_token  = "FILL_IN_YOUR_FLY_API_TOKEN"
fly_org        = "FILL_IN_YOUR_FLY_ORG"
fly_app_name   = "tennis-lineup"
fly_region     = "hkg"
zhipu_api_key  = "FILL_IN_YOUR_ZHIPU_API_KEY"
```

- [ ] **Step 3.6：在根目录 `.gitignore` 追加条目**

```
# Terraform
terraform/terraform.tfvars
terraform/.terraform/
terraform/*.tfstate
terraform/*.tfstate.backup
```

- [ ] **Step 3.7：Commit（不含 tfvars）**

```bash
git add terraform/variables.tf terraform/main.tf terraform/outputs.tf \
        terraform/.gitignore .gitignore
git commit -m "infra: add Terraform configuration for Fly.io deployment"
```

---

## Task 4：安装工具并填写变量（用户手动操作）

> 这些步骤需要用户在本机执行。

- [ ] **Step 4.1：安装 flyctl**

```bash
# Windows (PowerShell，以管理员身份运行)
iwr https://fly.io/install.ps1 -useb | iex

# macOS / Linux
curl -L https://fly.io/install.sh | sh
```

验证：`flyctl version`

- [ ] **Step 4.2：注册并登录 Fly.io**

访问 https://fly.io 注册账号，然后：

```bash
flyctl auth login
# 浏览器完成登录后，回到终端自动认证
```

- [ ] **Step 4.3：获取 API Token 和 Org slug**

```bash
flyctl auth token
# 输出类似：fo1_abc123...，复制后填入 terraform.tfvars 的 fly_api_token

flyctl orgs list
# 输出你的 org slug，填入 terraform.tfvars 的 fly_org
```

- [ ] **Step 4.4：安装 Terraform**

```bash
# Windows（用 Chocolatey）
choco install terraform

# 验证
terraform version
# Expected：Terraform v1.5.x 或更高
```

- [ ] **Step 4.5：填写 `terraform/terraform.tfvars`**

将上面获取的值替换文件中的 4 个 `FILL_IN_*` 占位符。

- [ ] **Step 4.6：初始化 Terraform 并提交 lock 文件**

```bash
cd /c/Users/lorra/projects/tennis/terraform
terraform init
```

Expected：`Terraform has been successfully initialized!` 并生成 `.terraform.lock.hcl`。

```bash
git add .terraform.lock.hcl
git commit -m "infra: add Terraform provider lock file"
```

---

## Task 5：首次部署

- [ ] **Step 5.1：Terraform 创建基础设施（App、IP、Volume、Machine）**

> **重要：** 不要提前手动运行 `flyctl apps create`。让 Terraform 完全拥有 App 资源，避免冲突。

```bash
cd /c/Users/lorra/projects/tennis/terraform
terraform plan    # 预览将要创建的 5 个资源
terraform apply   # 输入 yes 确认
```

Expected：创建 `fly_app`、`fly_ip`×2、`fly_volume`、`fly_machine` 共 5 个资源。

> 首次 apply 时 `fly_machine` 引用的镜像还不存在，Fly.io 会创建 Machine 但标记为不健康——这是正常的，Step 5.3 推送镜像后自动恢复。

- [ ] **Step 5.2：在 Volume 上写入初始数据（避免健康检查失败）**

首次部署后 Volume 是空的，`/api/teams` 会返回空数组（HTTP 200），健康检查可以通过。如果后端启动时找不到文件会自动创建，无需手动干预。验证：

```bash
flyctl logs --app tennis-lineup | grep -i "data file"
# Expected 日志：Data file not found, creating new file: /data/tennis-data.json
```

- [ ] **Step 5.3：构建并推送 Docker 镜像**

```bash
cd /c/Users/lorra/projects/tennis

# 登录 Fly.io 镜像仓库
flyctl auth docker

# 构建镜像（约 3–5 分钟）
docker build -t registry.fly.io/tennis-lineup:latest .

# 推送到 Fly.io registry
docker push registry.fly.io/tennis-lineup:latest
```

- [ ] **Step 5.4：部署新镜像到 Machine**

```bash
flyctl deploy --app tennis-lineup --image registry.fly.io/tennis-lineup:latest
```

Expected：输出 `Visit your newly deployed app at https://tennis-lineup.fly.dev`

- [ ] **Step 5.5：验证部署**

```bash
flyctl status --app tennis-lineup
# Expected：1 machine, state = started

flyctl logs --app tennis-lineup
# Expected：Spring Boot 启动日志，无 ERROR
```

浏览器访问 `https://tennis-lineup.fly.dev`，确认：
- 首页正常显示
- 可以创建队伍、添加球员
- AI 功能正常（若填写了 ZHIPU_API_KEY）

---

## Task 6：备份脚本

- [ ] **Step 6.1：创建 `scripts/backup.sh`**

```bash
#!/bin/bash
# 将 Fly.io Volume 上的数据备份到本地
# 用法: ./scripts/backup.sh [app-name]
APP=${1:-tennis-lineup}
DATE=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="./backups"
mkdir -p "$BACKUP_DIR"
echo "Backing up $APP data..."
flyctl ssh console --app "$APP" -C "cat /data/tennis-data.json" > "$BACKUP_DIR/tennis-data-$DATE.json"
flyctl ssh console --app "$APP" -C "cat /data/tennis-config.json" > "$BACKUP_DIR/tennis-config-$DATE.json"
echo "Done. Files saved to $BACKUP_DIR/"
```

```bash
mkdir -p scripts
# 创建文件后：
chmod +x scripts/backup.sh
git add scripts/backup.sh
git commit -m "ops: add manual backup script for Fly.io volume data"
```

---

## Task 7：后续更新流程

每次修改代码后：

```bash
# 1. 构建新镜像
docker build -t registry.fly.io/tennis-lineup:latest .

# 2. 推送
docker push registry.fly.io/tennis-lineup:latest

# 3. 零停机滚动部署
flyctl deploy --app tennis-lineup --image registry.fly.io/tennis-lineup:latest
```

修改基础设施（调整 region、内存等）时，修改 `terraform.tfvars` 后：

```bash
cd terraform && terraform apply
```

---

## 常见问题

**`fly_app_name` 已被占用**
Fly.io App 名称全局唯一。改一个名字（如 `tennis-lineup-yourname`）并同步更新 `terraform.tfvars`。

**Terraform apply 报 machine 镜像错误**
首次 apply 时镜像不存在是正常现象，machine 会处于非健康状态。按 Task 5.3–5.4 推送镜像后自动恢复。

**本地后端启动后 AI 不工作**
确认 `application-local.yml` 中有 `zhipu.api.key`，并且启动时传了 `--spring.profiles.active=local`。

**数据丢失风险**
`terraform destroy` 会删除 Volume，数据不可恢复。正式使用前请先运行 `scripts/backup.sh` 备份。

**IPv4 费用**
`fly_ip` 的 `type = "v4"` 是独立 IPv4，Fly.io 收费约 $2/月。如果想完全免费，可将 `type = "v4"` 改为 `type = "shared_v4"`（使用 Fly.io 共享 anycast IP，功能相同，个人项目足够用）。
