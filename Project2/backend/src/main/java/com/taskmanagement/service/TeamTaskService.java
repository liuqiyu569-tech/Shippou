package com.taskmanagement.service;

import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.dto.request.TeamTaskCreateRequest;
import com.taskmanagement.dto.request.TeamTaskUpdateRequest;
import com.taskmanagement.dto.response.TaskDependencyGraphResponse;
import com.taskmanagement.dto.response.TaskDependencyListResponse;
import com.taskmanagement.dto.response.TaskOptionResponse;
import com.taskmanagement.dto.response.TaskOperationLogResponse;
import com.taskmanagement.dto.response.TeamTaskResponse;
import com.taskmanagement.dto.response.TeamTaskStatsResponse;
import com.taskmanagement.entity.enums.TaskLogObjectType;
import com.taskmanagement.entity.enums.TaskLogOperationType;
import com.taskmanagement.entity.enums.TaskPriority;
import com.taskmanagement.entity.enums.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Service contract for team task management and assignment operations.
 */
public interface TeamTaskService {

    /**
     * Creates a task in the target team. Only Owner and Admin members may create team tasks.
     *
     * @param teamId target team id
     * @param request task creation request
     * @return created task data
     */
    TeamTaskResponse createTeamTask(@NonNull Long teamId, @NonNull TeamTaskCreateRequest request);

    /**
     * Lists tasks in a team with optional filters. Every team member may browse team tasks.
     *
     * @param teamId target team id
     * @param status optional status filter
     * @param priority optional priority filter
     * @param assigneeId optional assignee filter
     * @param dueStart optional due-time lower bound
     * @param dueEnd optional due-time upper bound
     * @param page one-based page number
     * @param pageSize page size
     * @return filtered task page
     */
    PageResult<TeamTaskResponse> getTeamTasks(
        @NonNull Long teamId,
        @Nullable TaskStatus status,
        @Nullable TaskPriority priority,
        @Nullable Long assigneeId,
        @Nullable LocalDateTime dueStart,
        @Nullable LocalDateTime dueEnd,
        @Nullable String keyword,
        int page,
        int pageSize
    );

    TeamTaskStatsResponse getTeamTaskStats(@NonNull Long teamId, int upcomingDays);

    /**
     * Gets a team task detail after checking team membership.
     *
     * @param teamId target team id
     * @param taskId target task id
     * @return team task detail
     */
    TeamTaskResponse getTeamTask(@NonNull Long teamId, @NonNull Long taskId);

    /**
     * Updates a team task. Owner/Admin may update editable fields; Member may only update the status
     * of a task assigned to themselves.
     *
     * @param teamId target team id
     * @param taskId target task id
     * @param request update request
     * @return updated task data
     */
    TeamTaskResponse updateTeamTask(@NonNull Long teamId, @NonNull Long taskId, @NonNull TeamTaskUpdateRequest request);

    /**
     * Deletes a team task and its assignment records. Only Owner and Admin members may delete tasks.
     *
     * @param teamId target team id
     * @param taskId target task id
     */
    void deleteTeamTask(@NonNull Long teamId, @NonNull Long taskId);

    /**
     * Replaces the assignee list of a team task. Every assignee must be a member of the team.
     *
     * @param teamId target team id
     * @param taskId target task id
     * @param userIds assignee user ids
     */
    void assignTask(@NonNull Long teamId, @NonNull Long taskId, @NonNull List<Long> userIds);

    /**
     * Removes one assignee from a team task.
     *
     * @param teamId target team id
     * @param taskId target task id
     * @param userId assignee user id
     */
    void unassignTask(@NonNull Long teamId, @NonNull Long taskId, @NonNull Long userId);

    /**
     * 查询指定团队任务下的前置和后继任务列表。
     *
     * @param teamId 团队 ID
     * @param taskId 任务 ID
     * @return 依赖关系列表
     */
    TaskDependencyListResponse getTeamTaskDependencies(@NonNull Long teamId, @NonNull Long taskId);

    void addTeamTaskDependency(@NonNull Long teamId, @NonNull Long taskId, @NonNull Long dependsOnTaskId);

    void deleteTeamTaskDependency(@NonNull Long teamId, @NonNull Long taskId, @NonNull Long dependsOnTaskId);

    /**
     * 查询团队任务依赖图。
     *
     * @param teamId 团队 ID
     * @return DAG 数据
     */
    TaskDependencyGraphResponse getTeamDependencyGraph(@NonNull Long teamId);

    /**
     * 查询指定团队下可选任务（id + title）。
     *
     * @param teamId 团队 ID
     * @param keyword 关键词，null 表示无过滤
     * @return 团队任务列表
     */
    List<TaskOptionResponse> getTeamTaskOptions(@NonNull Long teamId, @Nullable String keyword);

    PageResult<TaskOperationLogResponse> queryTeamTaskLogs(
        @NonNull Long teamId,
        @NonNull Long taskId,
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        int page,
        int pageSize
    );

    PageResult<TaskOperationLogResponse> queryTeamLogs(
        @NonNull Long teamId,
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        @Nullable String keyword,
        @Nullable LocalDateTime startTime,
        @Nullable LocalDateTime endTime,
        int page,
        int pageSize
    );
}
