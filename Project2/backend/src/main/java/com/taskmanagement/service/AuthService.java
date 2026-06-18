package com.taskmanagement.service;

import com.taskmanagement.dto.request.LoginRequest;
import com.taskmanagement.dto.request.RegisterRequest;
import com.taskmanagement.dto.response.AuthResponse;
import org.springframework.lang.NonNull;

/**
 * 认证服务接口，定义用户注册和登录操作。
 *
 * @author user-auth
 */
public interface AuthService {

    /**
     * 用户注册。
     *
     * @param request 注册请求（包含用户名和密码）
     * @return 认证响应（包含 JWT 令牌和用户名）
     */
    AuthResponse register(@NonNull RegisterRequest request);

    /**
     * 用户登录。
     *
     * @param request 登录请求（包含用户名和密码）
     * @return 认证响应（包含 JWT 令牌和用户名）
     */
    AuthResponse login(@NonNull LoginRequest request);
}
