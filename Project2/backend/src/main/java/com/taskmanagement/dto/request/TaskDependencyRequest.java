package com.taskmanagement.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskDependencyRequest {

    @JsonProperty("dependsOnTaskId")
    @JsonAlias("dependTaskId")
    @NotNull(message = "前置依赖任务不能为空")
    private Long dependsOnTaskId;
}
