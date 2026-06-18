package com.fudan.shorturl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateShortUrlRequest {

    @NotBlank(message = "longUrl 不能为空")
    @Pattern(
            regexp = "^https?://.+",
            message = "longUrl 必须以 http:// 或 https:// 开头"
    )
    private String longUrl;

    /** 过期时间，null 表示永久 */
    private LocalDateTime expireTime;
}
