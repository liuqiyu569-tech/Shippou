package com.fudan.shorturl.service;

import com.fudan.shorturl.dto.CreateShortUrlRequest;
import com.fudan.shorturl.dto.ShortUrlVO;

public interface ShortUrlService {

    /** 长链 → 短链 */
    ShortUrlVO createShortUrl(CreateShortUrlRequest req);

    /** 短链 → 长链（用于跳转） */
    String getLongUrl(String shortCode);
}
