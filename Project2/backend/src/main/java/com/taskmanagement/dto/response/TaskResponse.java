package com.taskmanagement.dto.response;

import com.taskmanagement.entity.enums.DueStatus;
import com.taskmanagement.entity.enums.TaskPriority;
import com.taskmanagement.entity.enums.TaskStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDateTime dueAt;
    private DueStatus dueStatus;
    private Long teamId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
