package com.taskmanagement.dto.response;

import com.taskmanagement.entity.enums.TaskStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskDependencyNodeResponse {

    private Long id;
    private String title;
    private TaskStatus status;
}
