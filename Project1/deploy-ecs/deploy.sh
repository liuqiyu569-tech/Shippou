#!/usr/bin/env bash
# ============================================================
# ECS 端部署脚本 —— 由 CodeArts「主机部署」远程调用
# 作用：拉取 SWR 最新镜像 → 滚动重启容器 → 清理旧镜像
# 前提：/opt/short-url 下已有 docker-compose.yml + .env；ECS 已长期 docker login SWR
# ============================================================
set -euo pipefail
# 把 stderr 并入 stdout：CodeArts「执行shell命令」只回显 stdout，
# docker compose 的进度/报错默认走 stderr，不合流会被部署日志吞掉（debug 用）
exec 2>&1
cd /opt/short-url
echo "[1/4] 拉取最新镜像 (仅 app；基础镜像 mysql/redis/rabbitmq 本地已固化，交给 up -d 按需)..."
docker compose pull app
echo "[2/4] 滚动重启容器..."
docker compose up -d
echo "[3/4] 清理悬空旧镜像..."
docker image prune -f
echo "[4/4] 当前状态:"
docker compose ps --format 'table {{.Name}}\t{{.Status}}'
echo "[deploy] 完成"
