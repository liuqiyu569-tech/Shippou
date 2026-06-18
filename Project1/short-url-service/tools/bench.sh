#!/usr/bin/env bash
# Week 3 Redis 缓存压测（curl --parallel 版，单进程多路复用）
set -e

API_BASE="http://localhost:8080"
N=500
PARALLEL=50

echo "===== 1. 准备 $N 条不同长链 ====="
CODES_FILE=$(mktemp)
URLS_FILE=$(mktemp)
trap 'rm -f "$CODES_FILE" "$URLS_FILE"' EXIT

for i in $(seq 1 $N); do
    code=$(curl -s -X POST "$API_BASE/api/url/create" \
        -H "Content-Type: application/json" \
        -d "{\"longUrl\":\"https://benchmark.example.com/page-$i?ts=$(date +%s%N)\"}" \
        | grep -o '"shortCode":"[^"]*"' | cut -d'"' -f4)
    echo "$code" >> "$CODES_FILE"
    echo "url = \"$API_BASE/$code\"" >> "$URLS_FILE"
done
echo "已创建 $(wc -l < "$CODES_FILE") 个 shortCode"

run_round() {
    local label="$1"
    local start_ns end_ns elapsed_ms qps
    start_ns=$(date +%s%N)

    curl -s -K "$URLS_FILE" \
        --parallel --parallel-max "$PARALLEL" \
        -o /dev/null

    end_ns=$(date +%s%N)
    elapsed_ms=$(( (end_ns - start_ns) / 1000000 ))
    [ "$elapsed_ms" -le 0 ] && elapsed_ms=1
    qps=$(( N * 1000 / elapsed_ms ))
    avg_ms=$(awk "BEGIN{printf \"%.2f\", $elapsed_ms/$N}")
    printf "  [%s] N=%d 并发=%d  总耗时=%d ms  QPS≈%d  平均=%s ms\n" \
        "$label" "$N" "$PARALLEL" "$elapsed_ms" "$qps" "$avg_ms"
}

# 预热：JVM JIT、连接池
echo ""
echo "===== 2. 预热（让 JIT 编译热代码）====="
docker exec short-url-redis redis-cli FLUSHDB > /dev/null
run_round "warmup-DB"
run_round "warmup-Redis"

echo ""
echo "===== 3. Test A: 清空 Redis → 全量 DB miss ====="
docker exec short-url-redis redis-cli FLUSHDB > /dev/null
run_round "DB"

echo ""
echo "===== 4. Test B: 缓存已被回写 → 全量 Redis hit ====="
run_round "Redis"

echo ""
echo "===== 5. 再跑 2 轮 Redis 取平均 ====="
run_round "Redis"
run_round "Redis"
