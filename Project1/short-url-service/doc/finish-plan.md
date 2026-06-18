# Short URL Service 收尾工作日程

目标：不继续扩展新功能，集中把项目补到“可测试、可复现、可交付”的状态。

## Day 1：让测试可独立跑通（已完成）

目标：`mvnw test` 在本机不启动 Redis、MySQL、RabbitMQ 的情况下也能通过。

具体任务：

- 新建 `src/test/resources/application-test.yml`。
- 给 `ShortUrlServiceApplicationTests` 增加 `@ActiveProfiles("test")`。
- 在 test profile 中禁用或替换会硬连外部服务的 Bean，例如 Redisson、RabbitMQ、ShardingSphere 数据源。
- 先保证 `contextLoads` 能稳定通过。

验收命令：

```powershell
.\mvnw.cmd test
```

验收标准：

- 不启动任何中间件，测试通过。
- Surefire 报告中没有 `Unable to connect to Redis server: localhost/127.0.0.1:6379`。
- 2026-06-06 已验证：`.\mvnw.cmd test` 通过，结果为 `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

## Day 2：补核心业务测试（已完成）

目标：核心业务不是只靠手测，至少有单元测试覆盖主要分支。

具体任务：

- 为 `ShortUrlServiceImpl` 增加单元测试。
- Mock `ShortUrlMapper`、`LongUrlIndexMapper`、`StringRedisTemplate`、`RBloomFilter`、`RedissonClient`、`RabbitTemplate`。
- 覆盖以下场景：
  - 首次创建短链成功。
  - 同一长链重复创建返回已有短链。
  - 短码不存在返回 404。
  - 短链过期返回 410。
  - 缓存命中时不查 DB。
  - RabbitMQ 投递失败不影响跳转主流程。

验收命令：

```powershell
.\mvnw.cmd test
```

验收标准：

- 测试通过。
- 核心业务分支有明确断言，不只检查“不报错”。
- 2026-06-06 已验证：`.\mvnw.cmd test` 通过，结果为 `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。

## Day 3：修正文档和 SQL 一致性（已完成）

目标：README 和 SQL 脚本与当前 ShardingSphere 分片版本一致，别人照文档能跑。

具体任务：

- 修正 `README.md` 中的本地建库说明。
- 明确区分这些 SQL 文件：
  - `doc/sql/schema_legacy_single_table.sql`：旧版单表结构。
  - `doc/sql/init-docker.sql`：当前 Docker 干净环境初始化脚本。
  - `doc/sql/schema_sharding.sql`：历史迁移参考脚本。
- 修改或标注 `schema_sharding.sql` 中的 `CRC32(short_code) MOD 4` 迁移逻辑，避免被误用于当前 `HASH_MOD` 分片。
- 修正 `docker-compose.yml` 中不准确的 MySQL 版本注释。
- README 增加本地依赖启动方式：

```powershell
docker compose up -d mysql redis rabbitmq
.\mvnw.cmd spring-boot:run
```

验收标准：

- 新读者不会按 README 执行到旧单表 SQL。
- `schema_sharding.sql` 不再暗示 `CRC32` 和 ShardingSphere `HASH_MOD` 一致。
- 2026-06-06 已验证：README 已改为 Docker Compose 优先启动方式，`schema_sharding.sql` 已移除可执行的 `CRC32(short_code) MOD 4` 分桶语句，`.\mvnw.cmd test` 通过。

## Day 4：完整本地集成验证（已完成）

目标：验证 Docker Compose 环境下创建、跳转、访问计数链路可用。

具体任务：

- 根据 `.env.example` 创建 `.env` 并填入真实值。
- 启动完整环境：

```powershell
docker compose up -d --build
```

- 检查服务状态：

```powershell
docker compose ps
curl http://localhost:8080/actuator/health
```

- 创建短链：

```powershell
curl -X POST http://localhost:8080/api/url/create `
  -H "Content-Type: application/json" `
  -d "{\"longUrl\":\"https://www.fudan.edu.cn\"}"
```

- 访问返回的短链，确认 302 跳转。
- 检查数据库中的 `access_count` 是否异步增加。

验收标准：

- Docker 一键启动成功。
- 创建短链成功。
- 短链跳转成功。
- RabbitMQ 消费后访问计数能写回数据库。
- 2026-06-06 已验证：
  - `docker compose up -d --build` 成功。
  - `docker compose ps` 显示 app、mysql、redis、rabbitmq 均为 healthy。
  - `GET /actuator/health` 返回 `{"status":"UP"}`。
  - `POST /api/url/create` 创建短码 `2SPJrLaE2XA`。
  - `GET /2SPJrLaE2XA` 返回 `302`，跳转到 `https://www.fudan.edu.cn/`。
  - MySQL 分片表 `t_short_url_2` 中该短码 `access_count=1`。
  - `.\mvnw.cmd test` 通过，结果为 `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。

## Day 5：整理交付材料（已完成）

目标：项目具备可展示、可复查、可交接的最终状态。

具体任务：

- README 增加最终验证结果：
  - `mvnw test` 通过。
  - Docker Compose 启动通过。
  - 创建短链接口示例。
  - 跳转接口示例。
- 新增或完善 `doc/checklist.md`：
  - 本地启动检查。
  - 测试检查。
  - 部署检查。
  - 常见问题：Redis 未启动、MySQL 密码、端口冲突。
- 清理不应提交的临时文件，例如旧日志、临时测试输出。
- 最后检查：

```powershell
git status --short
.\mvnw.cmd test
```

验收标准：

- Git 工作区只包含计划内改动。
- 测试通过。
- 文档能支撑后续继续开发或项目展示。
- 2026-06-06 已完成：
  - README 已增加最终验证结果、接口验证示例和 Docker Compose 验证记录。
  - 已新增 `doc/checklist.md`，覆盖本地启动、测试、接口、数据库、部署和常见问题。
  - 已检查 ignored 文件，`.env`、`target/`、JMeter 输出等保持不提交。
  - 已清理 ignored 的构建产物和压测输出，并将旧单表 SQL 重命名为 `doc/sql/schema_legacy_single_table.sql`。

## 当前剩余改进项

- `AccessCountConsumer` 当前按 shortCode 逐条 `UPDATE`，后续可优化为单 SQL 批量更新。
- Mockito 在 JDK 23 下会提示动态加载 agent 未来将受限；当前不影响测试，后续可按 Mockito 文档配置显式 Java agent。
- Sentinel 测试日志在 Windows 终端中偶尔显示中文乱码，不影响业务和测试结果。

## 收尾结论

Day 1 到 Day 5 均已完成。项目当前具备可独立运行的自动化测试、可复现的 Docker Compose 集成验证、更新后的 README 和交付检查清单。
