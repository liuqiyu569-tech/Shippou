-- ================================================================
-- Week 7: ShardingSphere 分库分表
-- 改造：t_short_url 一张表 → t_short_url_0..3 四张分片表 + t_long_url_index 全局索引表
-- ================================================================
-- 说明：
-- 1. 这是历史单表迁移到分片结构的 DDL 参考，不是干净环境初始化脚本。
-- 2. 干净 Docker 环境请使用 doc/sql/init-docker.sql。
-- 3. 不要用 MySQL CRC32(short_code) MOD 4 迁移数据；当前 HASH_MOD 使用 Java
--    String.hashCode() 路由，两者结果不同，直接分桶会导致 ShardingSphere 查错分片。
-- 4. 历史数据迁移应通过应用层或专用迁移程序走 ShardingSphere 逻辑表重新写入，
--    由 ShardingSphere 自动路由到 t_short_url_0..3。

USE `short_url`;

-- ----------------------------------------------------------------
-- 1. 全局索引表（不分片）：longUrlHash → shortCode 映射
--    用途：createShortUrl 查重时只查这张表，避免对 4 张分片表广播查询
-- ----------------------------------------------------------------
DROP TABLE IF EXISTS `t_long_url_index`;
CREATE TABLE `t_long_url_index` (
    `long_url_hash` BIGINT      NOT NULL                COMMENT 'FNV-1a 64bit hash',
    `short_code`    VARCHAR(16) NOT NULL                COMMENT '对应短码',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`long_url_hash`),
    KEY `idx_short_code` (`short_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='长链hash全局索引（不分片）';

-- ----------------------------------------------------------------
-- 2. 分片表 t_short_url_0..3（结构与原 t_short_url 完全一致）
-- ----------------------------------------------------------------
DROP TABLE IF EXISTS `t_short_url_0`;
CREATE TABLE `t_short_url_0` (
    `id`            BIGINT       NOT NULL                COMMENT '雪花算法ID',
    `short_code`    VARCHAR(16)  NOT NULL                COMMENT '短链编码（Base62）',
    `long_url`      VARCHAR(2048) NOT NULL               COMMENT '原始长链接',
    `long_url_hash` BIGINT       NOT NULL                COMMENT '长链hash',
    `expire_time`   DATETIME     DEFAULT NULL            COMMENT '过期时间',
    `access_count`  BIGINT       NOT NULL DEFAULT 0      COMMENT '累计访问次数',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_short_code` (`short_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链分片0';

CREATE TABLE `t_short_url_1` LIKE `t_short_url_0`;
CREATE TABLE `t_short_url_2` LIKE `t_short_url_0`;
CREATE TABLE `t_short_url_3` LIKE `t_short_url_0`;

-- ----------------------------------------------------------------
-- 3. 历史数据迁移方式
-- ----------------------------------------------------------------
-- 请不要在 MySQL 中用 CRC32 / MOD 手动拆分 t_short_url。
-- 正确做法是：
-- 1. 保留原表备份。
-- 2. 编写一次性迁移程序，从原表读取历史数据。
-- 3. 通过当前应用的数据访问层写入逻辑表 t_short_url 和 t_long_url_index。
-- 4. 让 ShardingSphere 按 HASH_MOD 自动改写到真实分片表。
-- 5. 校验 4 张分片表总行数、全局索引行数、抽样短码查询结果。
