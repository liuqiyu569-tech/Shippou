package com.taskmanagement.dto.response;

import com.taskmanagement.entity.enums.TaskLogObjectType;
import com.taskmanagement.entity.enums.TaskLogOperationType;
import com.taskmanagement.entity.enums.TaskLogScopeType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskOperationLogResponse {

    private Long id;
    private UserInfoResponse operator;
    private TaskLogOperationType operationType;
    private TaskLogObjectType objectType;
    private TaskLogScopeType scopeType;
    private TaskSnapshot task;
    private TeamSnapshot team;
    private String summary;
    private LocalDateTime operationTime;

    @Getter
    @Builder
    public static class TaskSnapshot {
        private Long id;
        private String title;
    }

    @Getter
    @Builder
    public static class TeamSnapshot {
        private Long id;
        private String name;
    }
}
