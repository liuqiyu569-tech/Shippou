package com.taskmanagement.common.exception;

public class TaskDependencyNotFoundException extends BusinessException {

    public TaskDependencyNotFoundException() {
        super(404, "任务依赖不存在");
    }
}
