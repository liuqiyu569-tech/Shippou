package com.taskmanagement.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 认证响应 DTO，符合 Apifox 接口规范。
 *
 * <p>包含 JWT 令牌、令牌类型、有效期（秒）以及嵌套的用户信息。</p>
 *
 * @author user-auth
 */
@Getter
@Builder
public class AuthResponse {

    /** JWT 令牌。 */
    private final String token;

    /** 令牌类型，固定为 {@code "Bearer"}。 */
    private final String tokenType;

    /** 令牌有效期，单位：秒。 */
    private final long expiresIn;

    /** 用户基本信息。 */
    private final UserInfo user;

    /**
     * 用户基本信息，嵌套在认证响应中返回。
     */
    @Getter
    @Builder
    public static class UserInfo {

        /** 用户 ID。 */
        private final Long id;

        /** 用户名。 */
        private final String username;
    }
}
