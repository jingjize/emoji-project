# 多阶段构建 Dockerfile
# 阶段1: 构建应用
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests

# 阶段2: 运行应用
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装 wget 用于健康检查
RUN apk add --no-cache wget

# 创建非 root 用户
RUN addgroup -S spring && adduser -S spring -G spring

# 从构建阶段复制 JAR 文件
COPY --from=build /app/target/*.jar app.jar

# 创建输出目录和日志目录
RUN mkdir -p /app/output /app/logs && \
    chown -R spring:spring /app

# 切换到非 root 用户
USER spring:spring

# 暴露端口
EXPOSE 8443

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/meme/health || exit 1

# 启动应用
# Spring Boot 会自动提供静态资源（前端页面）
ENTRYPOINT ["java", "-jar", "app.jar"]

