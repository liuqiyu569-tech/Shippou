package com.taskmanagement.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.taskmanagement.entity.enums.TaskPriority;
import com.taskmanagement.entity.enums.TaskStatus;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TeamTaskUpdateRequest {

    @Size(max = 100, message = "任务标题最多100个字符")
    private String title;

    @Size(max = 2000, message = "任务描述最多2000个字符")
    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dueAt;
}
