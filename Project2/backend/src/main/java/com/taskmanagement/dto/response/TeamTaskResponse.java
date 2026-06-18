package com.taskmanagement.dto.response;

import com.taskmanagement.entity.enums.DueStatus;
import com.taskmanagement.entity.enums.TaskPriority;
import com.taskmanagement.entity.enums.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TeamTaskResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final TaskStatus status;
    private final TaskPriority priority;
    private final LocalDateTime dueAt;
    private final DueStatus dueStatus;
    private final Long teamId;
    private final UserInfoResponse creator;
    private final List<AssigneeInfo> assignees;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AssigneeInfo {

        private final Long userId;
        private final String username;
    }
}
