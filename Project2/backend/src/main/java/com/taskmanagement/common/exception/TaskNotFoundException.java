package com.taskmanagement.common.exception;

public class TaskNotFoundException extends BusinessException {

    public TaskNotFoundException() {
        super(404, "任务不存在");
    }
}
