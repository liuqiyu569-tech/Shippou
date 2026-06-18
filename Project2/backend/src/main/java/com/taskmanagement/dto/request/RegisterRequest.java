package com.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户注册请求 DTO。
 *
 * <p>对用户名和密码进行正则校验：
 * <ul>
 *   <li>用户名：4-20 位字母、数字或下划线</li>
 *   <li>密码：至少 6 位，且同时包含字母和数字</li>
 * </ul>
 * </p>
 *
 * @author user-auth
 */
@Getter
@Setter
public class RegisterRequest {

    /**
     * 用户名，4-20 位字母/数字/下划线。
     */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$",
            message = "用户名只能包含字母、数字和下划线，长度为 4〜20 位")
    private String username;

    /**
     * 密码，至少 6 位且同时包含字母和数字。
     */
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{6,}$",
            message = "密码不少于 6 位，且必须同时包含字母和数字")
    private String password;
}
