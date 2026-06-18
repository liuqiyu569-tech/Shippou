# Short URL Service 交付检查清单

用途：每次继续开发、演示或部署前，按这份清单快速确认项目状态。

## 本地启动检查

- `.env` 已存在，并基于 `.env.example` 填写真实密码。
- Docker Desktop 已启动。
- Compose 配置可解析：

```powershell
docker compose config --quiet
```

- 启动完整环境：

```powershell
docker compose up -d --build
```

- 确认容器健康：

```powershell
docker compose ps
```

- 确认应用健康：

```powershell
curl.exe -s http://localhost:8080/actuator/health
```

期望结果：返回 `{"status":"UP"}`。

## 测试检查

- 不启动任何中间件也能跑自动化测试：

```powershell
.\mvnw.cmd test
```

- 期望结果：`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- 当前已覆盖：
  - Spring Boot `test` profile 上下文启动。
  - 首次创建短链。
  - 同一长链重复创建返回已有短链。
  - 短码不存在返回 404。
  - 短链过期返回 410。
  - Redis 缓存命中时不查 DB。
  - RabbitMQ 投递失败不影响跳转主流程。

## 接口检查

- 创建短链：

```powershell
$body = @{ longUrl = 'https://www.fudan.edu.cn' } | ConvertTo-Json -Compress
$response = Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/url/create' `
  -ContentType 'application/json' `
  -Body $body
$response
```

- 验证跳转：

```powershell
curl.exe -s -i -o NUL -w "HTTP_STATUS=%{http_code}`nREDIRECT_URL=%{redirect_url}`n" `
  "http://localhost:8080/$($response.data.shortCode)"
```

期望结果：`HTTP_STATUS=302`，`REDIRECT_URL` 指向原始长链。

## 数据库检查

- 查询短码所在分片和访问计数：

```powershell
$code = $response.data.shortCode
$pw = (Get-Content .env | Where-Object { $_ -match '^MYSQL_ROOT_PASSWORD=' } | Select-Object -First 1) -replace '^MYSQL_ROOT_PASSWORD=', ''
$sql = "SELECT 't_short_url_0' AS shard, short_code, access_count FROM t_short_url_0 WHERE short_code = '$code' UNION ALL SELECT 't_short_url_1', short_code, access_count FROM t_short_url_1 WHERE short_code = '$code' UNION ALL SELECT 't_short_url_2', short_code, access_count FROM t_short_url_2 WHERE short_code = '$code' UNION ALL SELECT 't_short_url_3', short_code, access_count FROM t_short_url_3 WHERE short_code = '$code';"
docker compose exec -T mysql mysql -uroot "-p$pw" short_url -N -e $sql
```

期望结果：能查到一行，`access_count` 在访问短链并等待数秒后增加。

## 部署检查

- ECS 上 `/opt/short-url` 已存在 `docker-compose.yml` 和 `.env`。
- ECS 已登录 SWR 镜像仓库。
- ECS 部署脚本可执行：

```bash
bash deploy.sh
```

- 部署后检查：

```bash
docker compose ps
curl http://localhost:8080/actuator/health
```

## 常见问题

- `Unable to connect to Redis server: localhost/127.0.0.1:6379`：测试应使用 `test` profile；本地运行应用时确认 `REDIS_HOST` 和 `REDIS_PORT` 指向正确容器端口。
- 创建短链返回 500，日志提示 JSON parse error：Windows PowerShell 下不要用复杂转义拼 `curl -d`，优先用 `Invoke-RestMethod` 或 `ConvertTo-Json`。
- MySQL 连接失败：确认 `.env` 中 `MYSQL_ROOT_PASSWORD` 正确，Compose 中 app 连接容器内 `mysql:3306`，宿主机手动运行应用连接 `localhost:3307`。
- 端口冲突：当前宿主端口为 app `8080`、MySQL `3307`、Redis `6380`、RabbitMQ `5673`、RabbitMQ 管理台 `15673`。
- 历史数据迁移：不要用 `CRC32(short_code) MOD 4` 手动拆分数据；当前 ShardingSphere `HASH_MOD` 使用 Java `String.hashCode()`。
- 生成产物不提交：`target/`、`jmeter.log`、`tools/jmeter/result.jtl`、`tools/jmeter/report/` 都可重新生成，保持 ignored 即可。
