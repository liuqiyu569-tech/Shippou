package com.taskmanagement.common.exception;

public class TaskAccessDeniedException extends BusinessException {

    public TaskAccessDeniedException(String message) {
        super(403, message);
    }
}
