package com.taskmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TeamListResponse {

    private final Long id;
    private final String name;
    private final String role;
    private final long memberCount;
}
