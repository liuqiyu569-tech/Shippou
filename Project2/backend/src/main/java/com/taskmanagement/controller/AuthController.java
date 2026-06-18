package com.taskmanagement.controller;

import com.taskmanagement.common.dto.ApiResponse;
import com.taskmanagement.dto.request.LoginRequest;
import com.taskmanagement.dto.request.RegisterRequest;
import com.taskmanagement.dto.response.AuthResponse;
import com.taskmanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器，提供用户注册和登录接口。
 *
 * <p>所有接口均无需认证即可访问（在 SecurityConfig 中放行）。</p>
 *
 * @author user-auth
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册。
     *
     * <p>对请求参数进行 Bean Validation 校验（用户名正则、密码正则），
     * 校验通过后完成注册并返回 JWT 令牌。</p>
     *
     * @param request 注册请求
     * @return 包含 JWT 令牌和用户名的认证响应
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("注册成功"));
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 包含 JWT 令牌和用户名的认证响应
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }
}
