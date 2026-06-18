package com.taskmanagement.dto.response;

import com.taskmanagement.entity.enums.TaskPriority;
import com.taskmanagement.entity.enums.TaskStatus;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamTaskStatsResponse {

    private long total;
    private Map<TaskStatus, Long> byStatus;
    private Map<TaskPriority, Long> byPriority;
    private long overdueCount;
    private long upcomingDueCount;
}
