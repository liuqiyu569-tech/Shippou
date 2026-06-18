package com.taskmanagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 令牌工具类，负责令牌的生成、解析与校验。
 *
 * <p>使用 HMAC-SHA 算法签名，密钥和有效期从配置文件注入。</p>
 *
 * @author user-auth
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    /**
     * 构造方法，根据配置初始化签名密钥。
     *
     * @param secret       JWT 密钥字符串（{@code app.jwt.secret}）
     * @param expirationMs 令牌有效期（毫秒，{@code app.jwt.expiration-ms}）
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 为指定用户生成 JWT 令牌。
     *
     * @param userId   用户 ID，写入 subject
     * @param username 用户名，写入 claims
     * @return 签名后的 JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 从令牌中解析用户 ID。
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 校验令牌是否有效（签名正确且未过期）。
     *
     * @param token JWT 字符串
     * @return 有效返回 {@code true}，否则返回 {@code false}
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 获取令牌有效期（秒）。
     *
     * @return 有效期秒数
     */
    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    /**
     * 解析并验证 JWT，返回 Claims。
     *
     * @param token JWT 字符串
     * @return 解析后的 Claims
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
