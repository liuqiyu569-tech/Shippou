package com.taskmanagement.common.exception;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(401, "未登录或 Token 已过期");
    }
}
