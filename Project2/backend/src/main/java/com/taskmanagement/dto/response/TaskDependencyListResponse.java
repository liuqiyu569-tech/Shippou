package com.taskmanagement.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskDependencyListResponse {

    @Builder.Default
    private List<TaskDependencyItemResponse> prerequisites = new ArrayList<>();

    @Builder.Default
    private List<TaskDependencyItemResponse> successors = new ArrayList<>();
}
