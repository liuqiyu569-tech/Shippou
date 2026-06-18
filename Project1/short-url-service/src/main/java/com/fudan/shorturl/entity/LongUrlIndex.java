package com.fudan.shorturl.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 全局索引表（不分片）：longUrlHash → shortCode 映射
 * 用途：createShortUrl 查重时只查这张单表，避免对 4 张分片表广播查询
 */
@Data
@TableName("t_long_url_index")
public class LongUrlIndex {

    @TableId
    private Long longUrlHash;

    private String shortCode;

    private LocalDateTime createTime;
}
