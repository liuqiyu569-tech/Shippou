package com.fudan.shorturl.dto;

import lombok.Data;

@Data
public class ShortUrlVO {
    private String shortCode;
    private String shortUrl;
    private String longUrl;
}
