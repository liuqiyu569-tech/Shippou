package com.taskmanagement.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskDependencyEdgeResponse {

    private Long from;
    private Long to;
}
