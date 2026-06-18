package com.taskmanagement.service;

import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.dto.response.TaskOperationLogResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.enums.TaskLogObjectType;
import com.taskmanagement.entity.enums.TaskLogOperationType;
import java.time.LocalDateTime;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface TaskOperationLogService {

    void record(
        @NonNull Task task,
        @NonNull TaskLogOperationType operationType,
        @NonNull TaskLogObjectType objectType,
        @NonNull String summary
    );

    PageResult<TaskOperationLogResponse> queryPersonalTaskLogs(
        @NonNull Long taskId,
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        int page,
        int pageSize
    );

    PageResult<TaskOperationLogResponse> queryCurrentUserPersonalLogs(
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        @Nullable String keyword,
        @Nullable LocalDateTime startTime,
        @Nullable LocalDateTime endTime,
        int page,
        int pageSize
    );

    PageResult<TaskOperationLogResponse> queryTeamTaskLogs(
        @NonNull Long teamId,
        @NonNull Long taskId,
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        int page,
        int pageSize
    );

    PageResult<TaskOperationLogResponse> queryTeamLogs(
        @NonNull Long teamId,
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        @Nullable String keyword,
        @Nullable LocalDateTime startTime,
        @Nullable LocalDateTime endTime,
        int page,
        int pageSize
    );
}
