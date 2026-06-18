# ============================================================
# Stage 1: build —— 容器内跑 mvn package，避免依赖宿主机本地环境
# ============================================================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 先拷 pom.xml 跑 dependency:go-offline，利用 Docker 层缓存，源码变更不重拉依赖
COPY pom.xml .
RUN mvn -B -e -q dependency:go-offline

COPY src ./src
RUN mvn -B -e -q clean package -DskipTests

# ============================================================
# Stage 2: runtime —— 只带 JRE，不含 Maven/JDK/源码，运行镜像约 215MB
# 刻意不装 curl/tzdata：健康检查改用 Alpine 自带的 busybox wget；时区交给
# JVM 内置 tzdb（-Duser.timezone）。原因：CodeArts 构建节点无法稳定访问
# Alpine 官方 CDN（dl-cdn.alpinelinux.org DNS 解析失败），apk add 会挂——
# 索性让运行层零外网包依赖，CI 构建更稳、镜像更小
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

# JVM 调优：容器内存感知 + G1GC + 时区（JVM 自带 tzdb，无需 OS tzdata）
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

# 健康检查用 busybox wget（Alpine 基础镜像自带，无需 apk 装 curl）
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
