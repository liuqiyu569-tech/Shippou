package com.taskmanagement.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.taskmanagement.entity.enums.TaskPriority;
import com.taskmanagement.entity.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TaskUpdateRequest {

    @NotBlank(message = "任务标题不能为空")
    @Size(min = 1, max = 100, message = "任务标题长度需在 1 到 100 之间")
    private String title;

    @Size(max = 2000, message = "任务描述长度不能超过 2000")
    private String description;

    @NotNull(message = "任务状态不能为空")
    private TaskStatus status;

    @NotNull(message = "任务优先级不能为空")
    private TaskPriority priority;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dueAt;
}
