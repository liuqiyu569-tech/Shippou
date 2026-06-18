package com.taskmanagement.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskDependencyGraphResponse {

    @Builder.Default
    private List<TaskDependencyNodeResponse> nodes = new ArrayList<>();

    @Builder.Default
    private List<TaskDependencyEdgeResponse> edges = new ArrayList<>();
}
