package com.fudan.shorturl.controller;

import com.fudan.shorturl.common.result.Result;
import com.fudan.shorturl.dto.CreateShortUrlRequest;
import com.fudan.shorturl.dto.ShortUrlVO;
import com.fudan.shorturl.service.ShortUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "短链管理", description = "长链生成、查询")
@RestController
@RequestMapping("/api/url")
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    @Operation(summary = "创建短链", description = "传入长链，返回短链编码与完整短链 URL")
    @PostMapping("/create")
    public Result<ShortUrlVO> create(@Valid @RequestBody CreateShortUrlRequest req) {
        return Result.success(shortUrlService.createShortUrl(req));
    }
}
