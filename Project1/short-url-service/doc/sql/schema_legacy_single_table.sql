-- ================================================================
-- 短链接系统 数据库初始化脚本
-- 适用：MySQL 8.0+ / MySQL 9.x
-- ================================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS `short_url`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `short_url`;

-- 2. 短链主表
DROP TABLE IF EXISTS `t_short_url`;
CREATE TABLE `t_short_url` (
    `id`          BIGINT       NOT NULL                COMMENT '雪花算法ID',
    `short_code`  VARCHAR(16)  NOT NULL                COMMENT '短链编码（Base62）',
    `long_url`    VARCHAR(2048) NOT NULL               COMMENT '原始长链接',
    `long_url_hash` BIGINT     NOT NULL                COMMENT '长链hash（用于查重）',
    `expire_time` DATETIME     DEFAULT NULL            COMMENT '过期时间，NULL=永不过期',
    `access_count` BIGINT      NOT NULL DEFAULT 0      COMMENT '累计访问次数',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                       COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_short_code` (`short_code`),
    KEY `idx_long_url_hash` (`long_url_hash`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链接表';

-- 后续扩展（Week 5 访问日志会用到，先不创建）
-- DROP TABLE IF EXISTS `t_short_url_access_log`;
-- CREATE TABLE `t_short_url_access_log` ( ... );
