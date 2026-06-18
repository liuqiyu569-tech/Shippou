package com.taskmanagement.common.exception;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException() {
        super(404, "用户不存在");
    }
}
