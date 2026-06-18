package com.taskmanagement.security;

import com.taskmanagement.common.exception.UnauthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserIdPrincipal userIdPrincipal) {
            return userIdPrincipal.userId();
        }

        if (principal instanceof Long userId) {
            return userId;
        }

        if (principal instanceof String text && text.matches("\\d+")) {
            return Long.parseLong(text);
        }

        // 兼容常见 Spring Security 自定义 principal 命名方式，避免与队友实现耦合。
        try {
            Object value = principal.getClass().getMethod("getUserId").invoke(principal);
            if (value instanceof Number number) {
                return number.longValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // 如果没有 getUserId，则继续按未认证处理
        }

        throw new UnauthorizedException();
    }

    public record UserIdPrincipal(Long userId) {
    }
}
