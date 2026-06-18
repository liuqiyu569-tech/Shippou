package com.fudan.shorturl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_short_url")
public class ShortUrl {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String shortCode;

    private String longUrl;

    private Long longUrlHash;

    private LocalDateTime expireTime;

    private Long accessCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
