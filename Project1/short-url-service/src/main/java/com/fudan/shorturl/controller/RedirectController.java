package com.fudan.shorturl.controller;

import com.fudan.shorturl.service.ShortUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 短链 302 跳转
 * <p>
 * 路径正则 {1,16} 限定 base62 字符，避免吞掉 favicon.ico / doc.html 等
 */
@Tag(name = "短链跳转", description = "GET /{shortCode} → 302 → 原始长链")
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final ShortUrlService shortUrlService;

    @Operation(summary = "短链跳转")
    @GetMapping("/{shortCode:[0-9A-Za-z]{1,16}}")
    public void redirect(@PathVariable String shortCode,
                         HttpServletResponse response) throws IOException {
        String longUrl = shortUrlService.getLongUrl(shortCode);
        response.sendRedirect(longUrl);
    }
}
