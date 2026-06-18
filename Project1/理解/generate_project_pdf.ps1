$ErrorActionPreference = "Stop"

$Root = "C:\Users\25979\Desktop\Project"
$Project = Join-Path $Root "short-url-service"
$Deploy = Join-Path $Root "deploy-ecs"
$OutDir = Join-Path $Root "理解"
$MdPath = Join-Path $OutDir "短链接项目完整讲解.md"
$PdfPath = Join-Path $OutDir "短链接项目完整讲解.pdf"
$GeneratedDate = (Get-Date).ToString("yyyy-MM-dd")

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Read-Utf8Lines([string]$Path) {
    if (-not (Test-Path $Path)) { return @() }
    return [System.IO.File]::ReadAllLines($Path, [System.Text.Encoding]::UTF8)
}

function Rel([string]$Path) {
    return $Path.Replace($Root + "\", "").Replace("\", "/")
}

function Explain-Line([string]$line, [string]$file) {
    $t = $line.Trim()
    if ($t.Length -eq 0) { return "空行：用于分隔代码块，提高可读性。" }
    if ($t.StartsWith("//") -or $t.StartsWith("#") -or $t.StartsWith("/*") -or $t.StartsWith("*") -or $t.StartsWith("--")) { return "注释：说明设计意图、配置原因或历史踩坑，不参与运行逻辑。" }
    if ($t -match '^package ') { return "声明 Java 包名，决定该类在项目分层和命名空间中的位置。" }
    if ($t -match '^import ') { return "导入外部类或框架 API，供当前文件后续代码直接使用。" }
    if ($t -match '^@SpringBootApplication') { return "标记 Spring Boot 主应用，启用自动配置、组件扫描和配置绑定。" }
    if ($t -match '^@MapperScan') { return "指定 MyBatis Mapper 接口扫描路径，使接口能被注册为数据库访问 Bean。" }
    if ($t -match '^@RestController') { return "声明 REST 控制器，方法返回值会序列化为 HTTP 响应。" }
    if ($t -match '^@ControllerAdvice') { return "声明全局控制器增强，用于集中处理异常。" }
    if ($t -match '^@Service') { return "声明业务服务组件，由 Spring 容器管理并注入到控制器。" }
    if ($t -match '^@Configuration') { return "声明配置类，内部 @Bean 方法会创建框架组件。" }
    if ($t -match '^@Bean') { return "定义一个 Spring Bean，供其他组件通过依赖注入使用。" }
    if ($t -match '^@RabbitListener') { return "声明 RabbitMQ 消费入口，监听指定队列中的消息。" }
    if ($t -match '^@GetMapping|^@PostMapping|^@RequestMapping') { return "声明 HTTP 路由映射，把 URL 请求转交给该方法处理。" }
    if ($t -match '^@Value') { return "从配置文件或环境变量注入配置值，避免硬编码。" }
    if ($t -match '^@Data|^@Getter|^@Setter|^@RequiredArgsConstructor|^@Slf4j|^@TableName|^@TableId|^@TableField') { return "Lombok 或 MyBatis-Plus 注解：减少样板代码或建立实体与数据库表字段的映射。" }
    if ($t -match '^public class|^class |^public interface|^interface ') { return "定义类或接口，是当前文件的主体抽象。" }
    if ($t -match '^public static void main') { return "程序入口，启动 Spring Boot 应用。" }
    if ($t -match 'SpringApplication\.run') { return "调用 Spring Boot 启动流程，加载配置、创建 Bean、启动 Web 容器。" }
    if ($t -match '^private static final|^public static final') { return "定义常量，集中管理 key、队列名、TTL、锁参数等不可变配置。" }
    if ($t -match '^private final') { return "声明构造器注入的依赖，体现当前类依赖哪些组件协作。" }
    if ($t -match '^public .* createShortUrl') { return "创建短链的业务入口：负责幂等去重、短码生成、入库、缓存和索引维护。" }
    if ($t -match '^public .* getLongUrl') { return "短链解析业务入口：负责从短码查长链，并承担缓存、防穿透、防击穿和异步计数。" }
    if ($t -match 'selectById|selectOne|insert|update') { return "数据库访问语句，通过 MyBatis-Plus Mapper 查询、插入或更新记录。" }
    if ($t -match 'stringRedisTemplate|opsForValue|Redis') { return "Redis 缓存操作，用于降低数据库压力并提升跳转性能。" }
    if ($t -match 'shortUrlLocalCache|Caffeine') { return "Caffeine 本地缓存操作，是 Redis 故障或热点访问时的 L1 缓存兜底。" }
    if ($t -match 'shortCodeBloomFilter|RBloomFilter') { return "布隆过滤器逻辑，用于快速拦截明显不存在的短码，缓解缓存穿透。" }
    if ($t -match 'redissonClient|getLock|tryLock|unlock') { return "Redisson 分布式锁逻辑，用于缓存重建互斥，避免热点 key 击穿。" }
    if ($t -match 'rabbitTemplate|convertAndSend') { return "向 RabbitMQ 投递访问事件，把计数更新从跳转主流程中异步化。" }
    if ($t -match 'basicAck|basicNack|Channel') { return "RabbitMQ 手动确认或拒绝消息，保证消费成功后再确认。" }
    if ($t -match 'BusinessException') { return "业务异常，用明确状态码表达不存在、过期、限流等业务结果。" }
    if ($t -match 'return ') { return "返回当前方法的结果，结束本分支执行。" }
    if ($t -match '^if\s*\(') { return "条件分支，根据命中缓存、是否存在、是否过期、是否拿到锁等情况选择不同处理路径。" }
    if ($t -match '^for\s*\(|^while\s*\(') { return "循环处理多条数据或字符，直到满足退出条件。" }
    if ($t -match '^try\s*\{|^catch\s*\(|^finally\s*\{') { return "异常处理结构，保证降级、日志、ACK 或释放锁等收尾行为可靠执行。" }
    if ($t -match '^server:|^spring:|^mybatis-plus:|^shorturl:|^management:|^logging:') { return "YAML 顶级配置块，控制应用端口、框架组件、业务域名、监控和日志。" }
    if ($t -match 'CREATE TABLE|CREATE DATABASE|INSERT INTO|PRIMARY KEY|UNIQUE KEY|KEY ') { return "SQL DDL/DML：创建库表、索引或初始化数据，是持久化结构的基础。" }
    if ($t -match 'FROM |RUN |COPY |ENTRYPOINT|EXPOSE|WORKDIR') { return "Dockerfile 指令，定义镜像构建、文件复制、运行入口和暴露端口。" }
    if ($t -match 'services:|image:|ports:|volumes:|depends_on:|healthcheck:|environment:') { return "docker-compose 编排配置，定义容器、端口、依赖、健康检查和环境变量。" }
    if ($t -match '^<dependency>|^<groupId>|^<artifactId>|^<version>|^<plugin>|^<properties>|^<build>') { return "Maven 构建配置，声明依赖、版本或插件。" }
    return "普通语句/声明：参与当前文件的具体业务、配置或结构定义，结合上下文完成项目功能。"
}

function Add-FileSection([System.Collections.Generic.List[string]]$doc, [string]$path, [string]$purpose, [bool]$includeLines = $true) {
    $rel = Rel $path
    $doc.Add("")
    $doc.Add("## 文件讲解：$rel")
    $doc.Add("")
    $doc.Add("**文件作用**：$purpose")
    if (-not $includeLines) { return }
    $lines = Read-Utf8Lines $path
    $doc.Add("")
    $doc.Add("| 行号 | 原代码 / 内容 | 这一行的作用 |")
    $doc.Add("|---:|---|---|")
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $raw = $lines[$i].Replace("|", "\|")
        if ($raw.Length -gt 150) { $raw = $raw.Substring(0, 150) + " ..." }
        $exp = (Explain-Line $lines[$i] $rel).Replace("|", "\|")
        $doc.Add("| $($i + 1) | " + $raw + " | " + $exp + " |")
    }
}

$doc = [System.Collections.Generic.List[string]]::new()
$doc.Add("# Short URL Service 分布式短链接系统完整讲解")
$doc.Add("")
$doc.Add("生成时间：$GeneratedDate")
$doc.Add("")
$doc.Add("本文档用于系统讲解 short-url-service 与 deploy-ecs 两部分内容，覆盖项目用途、业务流程、技术栈、架构分层、核心难点、运行部署、测试压测，以及源码和配置文件的逐文件逐行说明。")
$doc.Add("")
$doc.Add("## 1. 项目到底有什么用")
$doc.Add("")
$doc.Add("这个项目是一个分布式短链接服务。它把用户输入的长 URL，例如 https://www.fudan.edu.cn，转换成更短的地址，例如 http://localhost:8080/2SPJrLaE2XA。用户访问短链接时，服务会快速找到原始长链接并返回 HTTP 302 重定向。")
$doc.Add("")
$doc.Add("短链接系统常用于短信、社交分享、营销投放、二维码、活动页跳转、访问统计和风控分析。它的工程价值不只在缩短 URL，更在于高并发跳转、缓存命中、热点保护、幂等生成、访问计数、海量数据分片和自动化部署。")
$doc.Add("")
$doc.Add("## 2. 整体业务闭环")
$doc.Add("")
$doc.Add("创建短链流程：客户端提交长链接 -> 参数校验 -> 计算长链 hash -> 查全局索引表判断是否已创建 -> 未命中则用雪花 ID 生成唯一数字 ID -> Base62 编码为短码 -> 写入分片表 -> 写全局索引表 -> 写 Redis 缓存和布隆过滤器 -> 返回短链。")
$doc.Add("")
$doc.Add("访问短链流程：浏览器请求 /{shortCode} -> Sentinel 限流 -> 本地 Caffeine 查询 -> 布隆过滤器判断是否可能存在 -> Redis 查询 -> 缓存未命中时分布式锁互斥回源 DB -> 校验过期 -> 返回 302 跳转 -> RabbitMQ 异步累计访问次数。")
$doc.Add("")
$doc.Add("## 3. 技术栈和每项技术的职责")
$doc.Add("")
$doc.Add("- Java 21：主开发语言。")
$doc.Add("- Spring Boot 3.5：Web 服务、依赖注入、配置管理和 Actuator 监控。")
$doc.Add("- MyBatis-Plus：简化 CRUD、实体映射、逻辑删除和雪花 ID 策略。")
$doc.Add("- MySQL 8：保存短链、长链、访问次数和全局索引。")
$doc.Add("- Redis：作为 L2 缓存，提升短链跳转速度。")
$doc.Add("- Redisson：提供布隆过滤器和分布式锁。")
$doc.Add("- Caffeine：本地 L1 缓存，在 Redis 故障或热点读时兜底。")
$doc.Add("- RabbitMQ：把访问计数从跳转主链路剥离，提高跳转吞吐。")
$doc.Add("- Sentinel：对接口做限流，保护服务不被过量请求压垮。")
$doc.Add("- ShardingSphere-JDBC：把逻辑表 t_short_url 路由到 t_short_url_0..3 四张物理表。")
$doc.Add("- Docker / Compose：启动应用、MySQL、Redis、RabbitMQ 的完整运行环境。")
$doc.Add("- CodeArts / ECS：实现构建、推镜像、远程部署的自动化上线。")
$doc.Add("")
$doc.Add("## 4. 架构分层")
$doc.Add("")
$doc.Add("Controller 层只负责 HTTP 入参和响应；Service 层承载核心业务策略；Mapper 层负责数据库读写；Entity 对应数据库表；DTO/VO 负责请求和响应对象；Config 负责框架组件装配；Consumer 负责消息消费；Util 放通用工具函数。")
$doc.Add("")
$doc.Add("## 5. 项目难点总览")
$doc.Add("")
$doc.Add("- 幂等创建：同一个长链接重复提交时，应返回同一个短链，而不是创建多条记录。")
$doc.Add("- 哈希碰撞：长链 hash 虽然概率极低仍可能碰撞，所以命中索引后还要二次校验 longUrl。")
$doc.Add("- 高并发竞争：并发创建同一长链时，使用唯一键和 DuplicateKeyException 兜底，保证最终只有一个赢家。")
$doc.Add("- 缓存穿透：非法短码不应每次打到 DB，布隆过滤器和空值哨兵共同处理。")
$doc.Add("- 缓存击穿：热点短码缓存失效时，分布式锁让只有一个请求回源 DB。")
$doc.Add("- 缓存雪崩：TTL 加随机抖动，避免大量 key 同一时刻过期。")
$doc.Add("- Redis 故障降级：Caffeine L1 缓存和 DB 回源让跳转链路仍能工作。")
$doc.Add("- 访问统计性能：跳转请求不直接更新 DB，而是发 MQ 异步聚合更新。")
$doc.Add("- 分库分表路由：short_code 使用 ShardingSphere HASH_MOD 路由，不能用 MySQL CRC32 代替。")
$doc.Add("- 全局索引：t_long_url_index 避免按长链 hash 查询分片表时发生广播查询。")
$doc.Add("")
$doc.Add("## 6. 文件总览")
$allFiles = @()
$allFiles += Get-ChildItem $Project -Recurse -File | Where-Object { $_.FullName -notmatch '\\target\\|\\.git\\' }
$allFiles += Get-ChildItem $Deploy -Recurse -File
foreach ($f in ($allFiles | Sort-Object FullName)) {
    $rel = Rel $f.FullName
    $doc.Add("- " + $rel + "：项目文件，后文按类型说明其职责。")
}

$purposes = @{
    "short-url-service/src/main/java/com/fudan/shorturl/ShortUrlServiceApplication.java" = "Spring Boot 启动入口，扫描 Mapper 并启动 Web 服务。"
    "short-url-service/src/main/java/com/fudan/shorturl/common/exception/BusinessException.java" = "业务异常模型，携带业务状态码和错误信息。"
    "short-url-service/src/main/java/com/fudan/shorturl/common/exception/GlobalExceptionHandler.java" = "全局异常处理器，把校验异常、业务异常和未知异常统一包装成 Result。"
    "short-url-service/src/main/java/com/fudan/shorturl/common/result/Result.java" = "统一响应对象，封装 code、message、data。"
    "short-url-service/src/main/java/com/fudan/shorturl/config/BloomFilterConfig.java" = "创建 Redisson 布隆过滤器，用于短码存在性预判。"
    "short-url-service/src/main/java/com/fudan/shorturl/config/BloomFilterWarmUpRunner.java" = "应用启动后从数据库加载已有短码，预热布隆过滤器。"
    "short-url-service/src/main/java/com/fudan/shorturl/config/CaffeineConfig.java" = "创建本地缓存，作为 Redis 之前的 L1 缓存。"
    "short-url-service/src/main/java/com/fudan/shorturl/config/Knife4jConfig.java" = "配置 OpenAPI/Knife4j 接口文档。"
    "short-url-service/src/main/java/com/fudan/shorturl/config/RabbitConfig.java" = "声明 RabbitMQ 交换机、队列、绑定、消息转换器和确认回调。"
    "short-url-service/src/main/java/com/fudan/shorturl/config/RedisConfig.java" = "配置 Redis 序列化和 Redisson 客户端。"
    "short-url-service/src/main/java/com/fudan/shorturl/config/SentinelConfig.java" = "手写 Sentinel Web 拦截器与规则，适配 Spring Boot 3 jakarta。"
    "short-url-service/src/main/java/com/fudan/shorturl/config/ShardingSphereDataSourceConfig.java" = "程序化加载 ShardingSphere 配置并注入 MySQL 环境变量。"
    "short-url-service/src/main/java/com/fudan/shorturl/consumer/AccessCountConsumer.java" = "消费访问事件，批量聚合后更新访问次数。"
    "short-url-service/src/main/java/com/fudan/shorturl/controller/RedirectController.java" = "处理短码访问，返回 302 重定向。"
    "short-url-service/src/main/java/com/fudan/shorturl/controller/ShortUrlController.java" = "处理创建短链的 REST API。"
    "short-url-service/src/main/java/com/fudan/shorturl/dto/AccessEvent.java" = "RabbitMQ 访问事件消息体。"
    "short-url-service/src/main/java/com/fudan/shorturl/dto/CreateShortUrlRequest.java" = "创建短链请求 DTO，包含 longUrl 和 expireTime。"
    "short-url-service/src/main/java/com/fudan/shorturl/dto/ShortUrlVO.java" = "创建短链接口返回对象。"
    "short-url-service/src/main/java/com/fudan/shorturl/entity/LongUrlIndex.java" = "全局索引表实体，用 longUrlHash 映射 shortCode。"
    "short-url-service/src/main/java/com/fudan/shorturl/entity/ShortUrl.java" = "短链主表实体，对应分片表。"
    "short-url-service/src/main/java/com/fudan/shorturl/mapper/LongUrlIndexMapper.java" = "全局索引 Mapper。"
    "short-url-service/src/main/java/com/fudan/shorturl/mapper/ShortUrlMapper.java" = "短链主表 Mapper。"
    "short-url-service/src/main/java/com/fudan/shorturl/service/ShortUrlService.java" = "短链业务接口。"
    "short-url-service/src/main/java/com/fudan/shorturl/service/impl/ShortUrlServiceImpl.java" = "核心业务实现：创建、跳转、缓存、锁、布隆过滤器、MQ、TTL、hash。"
    "short-url-service/src/main/java/com/fudan/shorturl/util/Base62.java" = "Base62 编码工具，把数字 ID 编成短码。"
}

$doc.Add("")
$doc.Add("## 7. 核心源码逐文件逐行讲解")
foreach ($path in ($purposes.Keys | Sort-Object)) {
    Add-FileSection $doc (Join-Path $Root $path.Replace("/", "\")) $purposes[$path] $true
}

$doc.Add("")
$doc.Add("## 8. 配置、SQL、Docker、测试与部署文件讲解")
$extraFiles = @(
    @{ Path="short-url-service/pom.xml"; Purpose="Maven 项目模型，定义 Spring Boot 父工程、依赖版本、第三方依赖和构建插件。" },
    @{ Path="short-url-service/src/main/resources/application.yml"; Purpose="Spring Boot 运行配置，包含端口、Redis、RabbitMQ、MyBatis-Plus、业务域名、监控和日志。" },
    @{ Path="short-url-service/src/main/resources/sharding-config.yaml"; Purpose="ShardingSphere-JDBC 分片配置，定义数据源、逻辑表、HASH_MOD 算法和单表规则。" },
    @{ Path="short-url-service/src/main/resources/static/index.html"; Purpose="静态首页，提供浏览器端短链创建表单、健康状态检测、结果展示、复制和跳转入口。" },
    @{ Path="short-url-service/doc/sql/init-docker.sql"; Purpose="Docker 环境初始化 SQL，创建当前版本所需的分片表和索引表。" },
    @{ Path="short-url-service/doc/sql/schema_legacy_single_table.sql"; Purpose="早期单表版本 DDL，便于理解项目演进。" },
    @{ Path="short-url-service/doc/sql/schema_sharding.sql"; Purpose="分片表结构和迁移参考，说明 HASH_MOD 与 CRC32 的差异风险。" },
    @{ Path="short-url-service/Dockerfile"; Purpose="多阶段镜像构建文件，先用 Maven 编译，再用 JRE 运行。" },
    @{ Path="short-url-service/docker-compose.yml"; Purpose="本地完整环境编排，启动 app、MySQL、Redis、RabbitMQ。" },
    @{ Path="short-url-service/.env.example"; Purpose="环境变量模板，说明需要配置哪些端口和密码。" },
    @{ Path="short-url-service/.env"; Purpose="本地真实环境变量文件，记录本地 Docker Compose 和应用运行所需的端口、账号、密码等配置。" },
    @{ Path="short-url-service/.dockerignore"; Purpose="控制 Docker 构建上下文，排除无关文件提升构建速度。" },
    @{ Path="short-url-service/.gitignore"; Purpose="控制 Git 忽略文件，避免提交 target、IDE 缓存、真实 .env 等。" },
    @{ Path="short-url-service/src/test/java/com/fudan/shorturl/ShortUrlServiceApplicationTests.java"; Purpose="Spring Boot 上下文测试，验证应用能启动。" },
    @{ Path="short-url-service/src/test/java/com/fudan/shorturl/service/impl/ShortUrlServiceImplTest.java"; Purpose="核心业务单元测试，验证创建、查询、缓存和异常分支。" },
    @{ Path="short-url-service/src/test/resources/application-test.yml"; Purpose="测试环境配置，隔离测试依赖。" },
    @{ Path="short-url-service/tools/bench.sh"; Purpose="压测辅助脚本，用于自动化访问创建或跳转接口。" },
    @{ Path="short-url-service/tools/jmeter/redirect-bench.jmx"; Purpose="JMeter 压测计划，验证短链跳转吞吐和延迟。" },
    @{ Path="deploy-ecs/docker-compose.yml"; Purpose="ECS 生产环境 Compose 编排，负责线上服务容器运行。" },
    @{ Path="deploy-ecs/.env"; Purpose="ECS 部署使用的真实环境变量文件，记录线上容器运行所需的镜像、端口、账号、密码等配置。" },
    @{ Path="deploy-ecs/deploy.sh"; Purpose="ECS 上的部署脚本，拉取镜像并滚动更新容器。" },
    @{ Path="deploy-ecs/init-docker.sql"; Purpose="ECS MySQL 初始化 SQL。" }
)
foreach ($item in $extraFiles) {
    Add-FileSection $doc (Join-Path $Root $item.Path.Replace("/", "\")) $item.Purpose $true
}

$doc.Add("")
$doc.Add("## 9. 其余工程文件作用")
$remaining = $allFiles | Where-Object {
    $r = Rel $_.FullName
    -not $purposes.ContainsKey($r) -and -not ($extraFiles | Where-Object { $_.Path -eq $r })
}
foreach ($f in ($remaining | Sort-Object FullName)) {
    $r = Rel $f.FullName
    $role = "辅助文件。"
    if ($r -match '\.idea/') { $role = "IntelliJ IDEA 项目元数据，记录 IDE 工程、编码、仓库、编译器等设置，不属于业务运行核心。" }
    elseif ($r -match 'mvnw|mvnw.cmd|maven-wrapper') { $role = "Maven Wrapper，用于在未安装 Maven 的机器上下载并使用指定 Maven 版本构建项目。" }
    elseif ($r -match 'README') { $role = "项目说明文档，概括技术亮点、运行方式、路线图和验证结果。" }
    elseif ($r -match 'LICENSE') { $role = "许可证文件，说明代码使用授权。" }
    elseif ($r -match 'doc/') { $role = "项目文档，记录完成计划、检查清单或历史方案。" }
    elseif ($r -match '\.env$') { $role = "真实环境变量文件，包含端口、账号、密码等运行配置；已按用户要求在逐行讲解中展开。" }
    $doc.Add("- " + $r + "：" + $role)
}

$doc.Add("")
$doc.Add("## 10. 学习与面试讲解重点")
$doc.Add("")
$doc.Add("你可以把这个项目讲成一个从 MVP 到高并发分布式系统演进的项目：第一阶段完成短链生成和跳转；第二阶段加入 Redis 缓存提升性能；第三阶段用布隆过滤器、空值缓存、分布式锁、TTL 抖动解决缓存三大问题；第四阶段用 RabbitMQ 异步计数提高跳转吞吐；第五阶段用 Sentinel 限流保护接口；第六阶段引入 ShardingSphere 分片支持海量数据；最后用 Docker 和 CodeArts 部署上线。")
$doc.Add("")
$doc.Add("最值得展开的难点是：为什么幂等不能只查分片表、为什么全局索引能避免广播查询、为什么 HASH_MOD 不能用 CRC32 模拟、为什么访问计数不能同步更新 DB、为什么布隆过滤器和空值哨兵要同时存在、为什么 Redis 故障时还需要 Caffeine 兜底。")

[System.IO.File]::WriteAllLines($MdPath, $doc, [System.Text.Encoding]::UTF8)

Add-Type -AssemblyName System.Drawing

function Wrap-Text([System.Drawing.Graphics]$g, [string]$text, [System.Drawing.Font]$font, [int]$maxWidth) {
    $result = New-Object System.Collections.Generic.List[string]
    if ($text -eq $null) { $result.Add(""); return $result }
    $line = ""
    foreach ($ch in $text.ToCharArray()) {
        $try = $line + $ch
        if ($g.MeasureString($try, $font).Width -gt $maxWidth -and $line.Length -gt 0) {
            $result.Add($line)
            $line = [string]$ch
        } else {
            $line = $try
        }
    }
    $result.Add($line)
    return $result
}

$pageW = 1240
$pageH = 1754
$margin = 70
$bodyFont = New-Object System.Drawing.Font("Microsoft YaHei", 18, [System.Drawing.FontStyle]::Regular)
$titleFont = New-Object System.Drawing.Font("Microsoft YaHei", 30, [System.Drawing.FontStyle]::Bold)
$headingFont = New-Object System.Drawing.Font("Microsoft YaHei", 24, [System.Drawing.FontStyle]::Bold)
$monoFont = New-Object System.Drawing.Font("Consolas", 15, [System.Drawing.FontStyle]::Regular)
$brush = [System.Drawing.Brushes]::Black
$gray = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(80,80,80))
$imgPaths = New-Object System.Collections.Generic.List[string]
$bmp = $null
$g = $null
$y = 0
$page = 0

function New-Page {
    if ($script:g -ne $null) {
        $footer = "第 $script:page 页"
        $script:g.DrawString($footer, $script:bodyFont, $script:gray, $script:margin, $script:pageH - 45)
        $p = Join-Path $script:OutDir ("project_explain_page_{0:D4}.jpg" -f $script:page)
        $script:bmp.Save($p, [System.Drawing.Imaging.ImageFormat]::Jpeg)
        $script:imgPaths.Add($p)
        $script:g.Dispose()
        $script:bmp.Dispose()
    }
    $script:page++
    $script:bmp = New-Object System.Drawing.Bitmap($script:pageW, $script:pageH)
    $script:g = [System.Drawing.Graphics]::FromImage($script:bmp)
    $script:g.Clear([System.Drawing.Color]::White)
    $script:g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $script:y = $script:margin
}

function Add-LineToPage([string]$text, [string]$kind) {
    $font = $script:bodyFont
    $leading = 8
    if ($kind -eq "title") { $font = $script:titleFont; $leading = 16 }
    elseif ($kind -eq "heading") { $font = $script:headingFont; $leading = 12 }
    elseif ($kind -eq "code") { $font = $script:monoFont; $leading = 6 }
    $maxWidth = $script:pageW - 2 * $script:margin
    $wrapped = Wrap-Text $script:g $text $font $maxWidth
    foreach ($w in $wrapped) {
        $h = [int][Math]::Ceiling($script:g.MeasureString("测", $font).Height) + $leading
        if ($script:y + $h -gt $script:pageH - 70) { New-Page }
        $script:g.DrawString($w, $font, $script:brush, $script:margin, $script:y)
        $script:y += $h
    }
}

New-Page
foreach ($line in $doc) {
    if ($line.StartsWith("# ")) { Add-LineToPage $line.Substring(2) "title" }
    elseif ($line.StartsWith("## ")) { Add-LineToPage $line.Substring(3) "heading" }
    elseif ($line.StartsWith("|")) { Add-LineToPage $line "code" }
    elseif ($line.StartsWith("- ")) { Add-LineToPage $line "body" }
    else { Add-LineToPage $line "body" }
}
New-Page
if ($g -ne $null) {
    $g.DrawString("文档结束", $headingFont, $brush, $margin, $margin)
    $g.DrawString("Markdown 原文也已生成，便于继续编辑和复制。", $bodyFont, $brush, $margin, $margin + 60)
    $g.DrawString("第 $page 页", $bodyFont, $gray, $margin, $pageH - 45)
    $last = Join-Path $OutDir ("project_explain_page_{0:D4}.jpg" -f $page)
    $bmp.Save($last, [System.Drawing.Imaging.ImageFormat]::Jpeg)
    $imgPaths.Add($last)
    $g.Dispose()
    $bmp.Dispose()
}

function Write-Ascii([System.IO.MemoryStream]$ms, [string]$s) {
    $bytes = [System.Text.Encoding]::ASCII.GetBytes($s)
    $ms.Write($bytes, 0, $bytes.Length)
}

$ms = New-Object System.IO.MemoryStream
Write-Ascii $ms "%PDF-1.4`n"
$offsets = New-Object System.Collections.Generic.List[int64]
$objects = New-Object System.Collections.Generic.List[object]

function Add-Obj([scriptblock]$writer) {
    $script:objects.Add($writer) | Out-Null
}

$pageCount = $imgPaths.Count
$catalogId = 1
$pagesId = 2
$nextId = 3
$pageObjIds = @()
$contentObjIds = @()
$imageObjIds = @()
for ($i = 0; $i -lt $pageCount; $i++) {
    $pageObjIds += $nextId; $nextId++
    $contentObjIds += $nextId; $nextId++
    $imageObjIds += $nextId; $nextId++
}

$offsets.Add(0) | Out-Null
for ($id = 1; $id -lt $nextId; $id++) {
    $offsets.Add($ms.Position) | Out-Null
    Write-Ascii $ms "$id 0 obj`n"
    if ($id -eq $catalogId) {
        Write-Ascii $ms "<< /Type /Catalog /Pages $pagesId 0 R >>`n"
    } elseif ($id -eq $pagesId) {
        $kids = ($pageObjIds | ForEach-Object { "$_ 0 R" }) -join " "
        Write-Ascii $ms "<< /Type /Pages /Count $pageCount /Kids [ $kids ] >>`n"
    } else {
        $idx = [Array]::IndexOf($pageObjIds, $id)
        if ($idx -ge 0) {
            $imgName = "Im$idx"
            Write-Ascii $ms "<< /Type /Page /Parent $pagesId 0 R /MediaBox [0 0 595 842] /Resources << /XObject << /$imgName $($imageObjIds[$idx]) 0 R >> >> /Contents $($contentObjIds[$idx]) 0 R >>`n"
        } else {
            $idx = [Array]::IndexOf($contentObjIds, $id)
            if ($idx -ge 0) {
                $imgName = "Im$idx"
                $stream = "q`n595 0 0 842 0 0 cm`n/$imgName Do`nQ`n"
                Write-Ascii $ms "<< /Length $([System.Text.Encoding]::ASCII.GetByteCount($stream)) >>`nstream`n"
                Write-Ascii $ms $stream
                Write-Ascii $ms "endstream`n"
            } else {
                $idx = [Array]::IndexOf($imageObjIds, $id)
                $bytes = [System.IO.File]::ReadAllBytes($imgPaths[$idx])
                Write-Ascii $ms "<< /Type /XObject /Subtype /Image /Width $pageW /Height $pageH /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length $($bytes.Length) >>`nstream`n"
                $ms.Write($bytes, 0, $bytes.Length)
                Write-Ascii $ms "`nendstream`n"
            }
        }
    }
    Write-Ascii $ms "endobj`n"
}

$xref = $ms.Position
Write-Ascii $ms "xref`n0 $nextId`n"
Write-Ascii $ms "0000000000 65535 f `n"
for ($i = 1; $i -lt $nextId; $i++) {
    Write-Ascii $ms ("{0:D10} 00000 n `n" -f $offsets[$i])
}
Write-Ascii $ms "trailer`n<< /Size $nextId /Root $catalogId 0 R >>`nstartxref`n$xref`n%%EOF"
[System.IO.File]::WriteAllBytes($PdfPath, $ms.ToArray())
$ms.Dispose()

foreach ($p in $imgPaths) { Remove-Item -LiteralPath $p -Force }

Write-Host "Markdown: $MdPath"
Write-Host "PDF: $PdfPath"
Write-Host "Pages: $pageCount"
