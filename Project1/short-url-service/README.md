# Short URL Service · 分布式短链接系统

> 个人独立开发项目，目标：在高并发场景下提供低延迟的长链 → 短链生成与跳转服务

## ✨ 技术亮点

- **生成算法**：Base62 + 雪花算法（Snowflake）双方案对比
- **缓存**：Redis 多级缓存 + 布隆过滤器，解决缓存穿透 / 击穿 / 雪崩
- **高并发**：消息队列异步处理访问统计，单机 QPS 目标 5000+
- **海量数据**：ShardingSphere 分库分表（按短链 hash 路由）
- **限流降级**：Sentinel 接口限流 + 熔断
- **工程化**：华为云 CodeArts 流水线 CI/CD，Docker 部署到 ECS

## 🛠 技术栈

| 类别 | 技术 |
|------|------|
| 语言 / 框架 | Java 21 · Spring Boot 3.5 |
| 持久层 | MyBatis-Plus 3.5 · MySQL 8 |
| 缓存 | Redis 7 |
| 消息队列 | RabbitMQ 3.13 |
| 分库分表 | ShardingSphere-JDBC 5.5.2 |
| 限流 | Sentinel 1.8 + 自写 jakarta 拦截器 |
| 接口文档 | Knife4j (OpenAPI 3) |
| 构建 / 部署 | Maven · Docker · 华为云 CodeArts |

## 📁 目录结构

```
src/main/java/com/fudan/shorturl
├── ShortUrlServiceApplication.java   主启动类
├── controller   控制器层（REST API）
├── service      业务接口
│   └── impl     业务实现
├── mapper       MyBatis Mapper
├── entity       数据库实体
├── dto          请求 / 响应对象
├── config       配置类（Redis / Knife4j / Web）
├── common       通用：异常处理 / 统一返回
└── util         工具：Base62 / Snowflake
```

## 🚀 本地启动

### 方式 A：Docker Compose 一键启动（推荐）

1. 根据模板创建环境变量文件：
   ```bash
   cp .env.example .env
   ```
2. 修改 `.env` 中的 MySQL / RabbitMQ 密码。
3. 启动完整环境：
   ```bash
   docker compose up -d --build
   ```
4. 检查健康状态：
   ```bash
   curl http://localhost:8080/actuator/health
   ```
5. 访问接口文档：http://localhost:8080/doc.html

### 方式 B：本机运行应用，依赖用 Docker 启动

1. 启动依赖服务：
   ```bash
   docker compose up -d mysql redis rabbitmq
   ```
2. 启动应用：

   ```bash
   # Windows PowerShell
   $env:MYSQL_HOST="localhost"
   $env:MYSQL_PORT="3307"
   $env:MYSQL_PASSWORD="你的 .env 中 MYSQL_ROOT_PASSWORD"
   $env:REDIS_HOST="localhost"
   $env:REDIS_PORT="6380"
   $env:RABBITMQ_HOST="localhost"
   $env:RABBITMQ_PORT="5673"
   $env:RABBITMQ_USERNAME="你的 .env 中 RABBITMQ_USERNAME"
   $env:RABBITMQ_PASSWORD="你的 .env 中 RABBITMQ_PASSWORD"
   .\mvnw.cmd spring-boot:run

   # Linux / macOS
   MYSQL_HOST=localhost MYSQL_PORT=3307 MYSQL_PASSWORD=你的密码 \
   REDIS_HOST=localhost REDIS_PORT=6380 \
   RABBITMQ_HOST=localhost RABBITMQ_PORT=5673 \
   RABBITMQ_USERNAME=admin RABBITMQ_PASSWORD=你的密码 \
   ./mvnw spring-boot:run
   ```

### SQL 脚本说明

- `doc/sql/init-docker.sql`：当前版本的干净环境初始化脚本，创建 `t_short_url_0..3` 分片表和 `t_long_url_index` 全局索引表。
- `doc/sql/schema_legacy_single_table.sql`：旧版单表结构，仅用于早期 MVP 参考，不适用于当前 ShardingSphere 分片版本。
- `doc/sql/schema_sharding.sql`：历史单表迁移到分片结构的 DDL 参考。不要使用 `CRC32(short_code) MOD 4` 迁移数据；`HASH_MOD` 使用 Java `String.hashCode()`，路由规则不同。

测试创建短链：
   ```bash
   curl -X POST http://localhost:8080/api/url/create \
     -H "Content-Type: application/json" \
     -d '{"longUrl":"https://www.fudan.edu.cn"}'
   ```

## ✅ 当前验证结果

验证时间：2026-06-06

- **自动化测试**：`.\mvnw.cmd test` 通过，结果为 `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- **Docker Compose**：`docker compose up -d --build` 通过，app、mysql、redis、rabbitmq 均为 healthy。
- **健康检查**：`GET http://localhost:8080/actuator/health` 返回 `{"status":"UP"}`。
- **创建短链**：`POST /api/url/create` 已验证可返回短码，例如 `2SPJrLaE2XA`。
- **短链跳转**：`GET /2SPJrLaE2XA` 返回 `302`，跳转到 `https://www.fudan.edu.cn/`。
- **异步计数**：RabbitMQ 消费后访问计数已写回 MySQL 分片表，验证样例为 `t_short_url_2.access_count=1`。

PowerShell 验证示例：

```powershell
$body = @{ longUrl = 'https://www.fudan.edu.cn' } | ConvertTo-Json -Compress
$response = Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/url/create' `
  -ContentType 'application/json' `
  -Body $body
$response

curl.exe -s -i -o NUL -w "HTTP_STATUS=%{http_code}`nREDIRECT_URL=%{redirect_url}`n" `
  "http://localhost:8080/$($response.data.shortCode)"
```

## 📈 开发路线图

- [x] Week 1：项目骨架 + 包结构
- [x] Week 2：MVP（长链 → 短链 / 跳转 / 幂等 / 校验 / 404）
- [x] Week 3：Redis 缓存 Cache-Aside（自定义序列化 / TTL 抖动 / 压测对比 +24% QPS）
- [x] Week 4：缓存防护三件套（布隆过滤器 / Redisson 分布式锁 / Caffeine 二级缓存，停 Redis 仍 8.6ms 兜底）
- [x] Week 5：RabbitMQ 异步访问统计（批量聚合双触发 flush，跳转 QPS 1272 → 2212）
- [x] Week 6：Sentinel 限流 + JMeter 压测（1000 QPS 精准限流，HTML 报告吞吐 1333 QPS / P99 38ms）
- [x] Week 7：ShardingSphere-JDBC 分库分表（HASH_MOD 4 分片 + 全局索引表避免广播查询）
- [x] Week 8：CodeArts CI/CD + ECS 部署

## 🧩 Week 7：分库分表实践要点

- **依赖**：`shardingsphere-jdbc 5.5.2`（适配 Spring Boot 3 jakarta）
- **分片策略**：`t_short_url` 逻辑表 → `t_short_url_0..3` 4 张物理分片表，按 `short_code` 走 `HASH_MOD`（Java `Math.abs(hashCode()) % 4`）
- **全局索引表 `t_long_url_index`**：`longUrlHash → shortCode` 单表索引，让"长链去重"从原本会被 ShardingSphere 改写成 4 表广播查询，降级为 1 次 PK 单表查询 + 1 次 SK 分片查询
- **DataSource 装配**：自写 `ShardingSphereDataSourceConfig`，用 Spring 的 `@Value("${MYSQL_PASSWORD:}")` 注入密码后程序化构建 `DataSource`，绕开 ShardingSphere yaml 占位符 `$${...::}` 在 `mvn spring-boot:run` 模式下取不到环境变量的问题
- **配置坑点**：
  - 5.5.x 把 `HASH_MOD/MOD` 划为"自动分片算法",**必须**放在 `autoTables` 配置块,不能用普通 `tables` + `actualDataNodes`
  - 5.5.x 严格模式下，未在 sharding rule 中的表（如全局索引表）必须用 `!SINGLE` 显式声明为单表（这里用 `*.*.*` 通配）
- **历史数据迁移陷阱**：`HASH_MOD` 内部用 Java `String.hashCode()`，与 MySQL `CRC32()` **完全不同**。第一版迁移 SQL 用 `CRC32 MOD 4` 分桶,实际查询时 ShardingSphere 按 `hashCode` 路由会找错分片。正确做法:**让应用层通过 ShardingSphere 重写一次**，由它自动路由

## 🐳 Week 8：容器化部署 + CI/CD 自动化上线

### 容器化
- **多阶段 Dockerfile**：`maven:3.9.9-eclipse-temurin-21` 构建层编译打包 → `eclipse-temurin:21-jre-alpine` 运行层，最终镜像 ~215 MB（仅 JRE，不含 Maven / JDK / 源码）
- **docker-compose 编排**：app + MySQL + Redis + RabbitMQ 四服务，用 `depends_on.condition: service_healthy` 控制启动顺序（不靠 sleep 硬等）
- **配置外部化**：敏感信息全走环境变量 / `.env`（已 gitignore），镜像内零密码

### CI/CD 全自动流水线（华为云 CodeArts）

```
git push (master)
      │
      ▼
   流水线源
      │
      ▼
   构建 ── mvn package + docker build + 推送 SWR 私有镜像仓库
      │
      ▼
   部署 ── SSH 远程执行 ECS deploy.sh（docker compose pull && up -d 滚动重启）
      │
      ▼
   上线 http://124.71.171.162:8080
```

- **触发**：代码提交事件 + 分支过滤 `master`（主干保护：只有合入主干的代码才自动上线）
- **构建**：CodeArts Build 在容器内执行多阶段构建，镜像推送至 SWR 私有仓库
- **部署**：CodeArts Deploy 通过 SSH 远程执行 ECS 上的 `deploy.sh`（`docker compose pull && up -d` 滚动重启 + `docker image prune` 自动清理旧镜像）
- **成果**：实现「**提交即上线**」—— git push 到 master 后自动完成 构建 → 推送 → 部署 全流程，无需任何手动介入

### 生产部署（华为云 ECS）
- **环境**：HECS x86_64 / Ubuntu 22.04 / 2C·3.6G / 50G SSD，Docker 29 + Compose v2
- **资源隔离**：每容器设 `mem_limit`（app 1.5G / mysql 768M / mq 512M / redis 256M）—— 避免 JVM `MaxRAMPercentage` 按整机内存计算撑爆其他容器 OOM；额外加 2G swap（swappiness=10）
- **安全加固**：仅对外暴露 8080；RabbitMQ 管理台 15672 绑定 `127.0.0.1`，仅经 SSH 隧道访问
- **线上地址**：http://124.71.171.162:8080
