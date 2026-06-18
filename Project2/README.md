# TaskFlow - 团队协作任务管理系统

基于 **Vue 3** + **Spring Boot 3** 的全栈团队协作任务管理平台，支持 JWT 认证、多角色权限控制、任务生命周期管理、操作审计与统计分析。

## 运行环境

| 工具 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17+ | `backend/pom.xml` 使用 Java 17 |
| Maven | 3.9+ | 后端构建、启动、测试 |
| Node.js | 20 LTS | 前端开发环境 |
| npm | 10+ | 前端依赖管理 |
| MySQL | 8.0 | 可使用本仓库 `docker-compose.yml` 启动 |

## 项目启动方式

### 1. 启动数据库

推荐使用本机 MySQL，请手动创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS taskflow
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

或使用 Docker 启动 MySQL：

```bash
docker-compose up -d
```

该命令会启动 `taskflow-mysql` 容器，并自动创建 `taskflow` 数据库。默认连接信息：

```text
host: localhost
port: 3306
database: taskflow
username: root
password: root
```

### 2. 配置并启动后端

```bash
cd backend
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
```

根据实际 MySQL 环境修改 `src/main/resources/application-dev.yml` 中的数据库连接信息。

启动后端：

```bash
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 数据库初始化与迁移说明

项目使用 **Flyway** 管理数据库结构，后端启动时会自动执行 `backend/src/main/resources/db/migration` 下尚未执行过的迁移脚本。当前迁移包括：

| 迁移文件 | 说明 |
| --- | --- |
| `V1__init_schema.sql` | 用户与个人任务基础表 |
| `V2__add_team_tables.sql` | 团队、成员、任务分配相关表 |
| `V3__add_task_assignee_indexes.sql` | 任务分配索引优化 |
| `V4__align_schema.sql` | Schema 对齐优化 |
| `V5__add_task_dependencies.sql` | 任务依赖关系表 |
| `V6__add_task_operation_logs.sql` | 任务操作日志表 |

## 核心设计

### 角色权限模型

系统采用三级角色模型：**Owner**（拥有者）> **Admin**（管理员）> **Member**（普通成员）。

- 创建团队的用户自动成为 Owner，拥有全部管理权限。
- Admin 可创建、修改、删除任务并分配成员，但不能管理团队成员。
- Member 仅可浏览团队任务，且只能修改分配给自己的任务状态。
- **所有权限在后端 Service 层强制校验**，前端仅做 UI 级别展示控制。

### 数据隔离策略

- **用户级隔离**：个人任务接口基于 `creator_user_id` 过滤，用户只能操作自己的任务。
- **团队级隔离**：所有团队接口入口统一校验成员角色，非团队成员一律返回 403。
- **任务归属区分**：`tasks.team_id IS NULL` 为个人任务，非空为团队任务，两个查询路径互不交叉。

### 技术选型要点

- **JWT 无状态认证**：Token 含 userId 和 username，24 小时有效期，前端 Axios 拦截器自动附加 Authorization 头。
- **Flyway 数据库迁移**：通过版本化 SQL 脚本管理表结构演进，启动时自动执行未应用的迁移。
- **JPA Specification 动态查询**：团队任务列表筛选（状态、优先级、分配人、时间范围）通过组合 Specification 实现。

## 功能概览

- 用户注册/登录（JWT 认证）
- 个人任务 CRUD（创建、编辑、状态流转、优先级、截止时间）
- 团队管理（创建、加入、成员管理、角色变更、所有权转让、解散）
- 团队任务管理（创建、分配、状态追踪、多条件筛选与分页）
- 任务依赖关系（添加/移除前置依赖、循环检测、可视化依赖图）
- 任务操作日志（自动记录、权限隔离、已删除任务历史回溯）
- 团队任务统计（状态分布、优先级分布、逾期/即将到期统计）
- 关键词搜索（按标题/描述搜索，与筛选分页兼容）
- 截止时间提醒（动态计算 dueStatus 并前端展示）

## 测试运行

### 后端测试

```bash
cd backend
mvn test
```

测试覆盖：任务依赖业务规则、团队成员管理、权限校验、操作日志权限隔离、接口请求校验等。

### 前端检查

```bash
cd frontend
npm install
npm run build
```

`npm run build` 会先执行 `vue-tsc -b` 类型检查，再执行 Vite 生产构建。

## 文档索引

- 环境配置说明：`docs/环境配置说明.md`
- 开发指南：`docs/开发指南.md`
- 接口文档：`docs/接口文档.md`
- 数据库设计文档：`docs/数据库设计文档.md`
- 系统回归测试与验收用例：`docs/系统回归测试与验收用例.md`
