package com.taskmanagement.service.impl;

import com.taskmanagement.common.exception.BusinessException;
import com.taskmanagement.dto.request.LoginRequest;
import com.taskmanagement.dto.request.RegisterRequest;
import com.taskmanagement.dto.response.AuthResponse;
import com.taskmanagement.dto.response.AuthResponse.UserInfo;
import com.taskmanagement.entity.User;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.JwtTokenProvider;
import com.taskmanagement.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现类，处理用户注册与登录的核心业务逻辑。
 *
 * <p>注册时对密码进行 BCrypt 哈希加密后存储，登录时校验密码哈希。</p>
 *
 * @author user-auth
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * {@inheritDoc}
     *
     * <p>流程：检查用户名唯一性 → BCrypt 加密密码 → 持久化 → 生成 JWT。</p>
     *
     * @throws BusinessException 用户名已被注册时抛出（HTTP 409）
     */
    @Override
    @Transactional
    public AuthResponse register(@NonNull RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(409, "用户名 " + request.getUsername() + " 已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser);
    }

    /**
     * {@inheritDoc}
     *
     * <p>流程：按用户名查找 → BCrypt 校验密码 → 生成 JWT。</p>
     *
     * @throws BusinessException 用户名或密码错误时抛出（HTTP 401）
     */
    @Override
    public AuthResponse login(@NonNull LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        return buildAuthResponse(user);
    }

    /**
     * 构建符合 Apifox 接口规范的认证响应。
     *
     * @param user 已认证的用户实体
     * @return 包含 token、tokenType、expiresIn 和 user 信息的响应
     */
    private AuthResponse buildAuthResponse(User user) {
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .user(UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .build())
                .build();
    }
}
