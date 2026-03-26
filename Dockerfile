# ── Stage 1: 构建前端 ──────────────────────────────────────────
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
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
