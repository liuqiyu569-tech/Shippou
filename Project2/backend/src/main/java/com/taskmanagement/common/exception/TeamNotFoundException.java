package com.taskmanagement.common.exception;

public class TeamNotFoundException extends BusinessException {

    public TeamNotFoundException() {
        super(404, "团队不存在");
    }
}
