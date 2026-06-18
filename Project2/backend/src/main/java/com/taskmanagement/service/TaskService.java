package com.taskmanagement.service;

import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.dto.request.TaskCreateRequest;
import com.taskmanagement.dto.request.TaskUpdateRequest;
import com.taskmanagement.dto.response.TaskOptionResponse;
import com.taskmanagement.dto.response.TaskDependencyGraphResponse;
import com.taskmanagement.dto.response.TaskDependencyListResponse;
import com.taskmanagement.dto.response.TaskOperationLogResponse;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.entity.enums.TaskLogObjectType;
import com.taskmanagement.entity.enums.TaskLogOperationType;
import com.taskmanagement.entity.enums.TaskPriority;
import com.taskmanagement.entity.enums.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * 任务服务接口，定义任务管理的核心操作。
 *
 * <p>该接口提供任务的增删改查功能，所有操作都基于当前登录用户，确保数据隔离。</p>
 */
public interface TaskService {

    /**
     * 创建当前登录用户的任务。
     *
     * @param request 创建任务请求体
     * @return 创建后的任务响应
     */
    TaskResponse createTask(@NonNull TaskCreateRequest request);

    /**
     * 获取当前登录用户的任务列表。
     *
     * @return 当前用户任务列表（按创建时间倒序）
     */
    List<TaskResponse> getCurrentUserTasks();

    /**
     * 查询当前用户的个人任务和被分配的团队任务。
     *
     * @param scope 查询范围：personal、team 或 all
     * @param status 状态筛选，可为 null
     * @param priority 优先级筛选，可为 null
     * @param dueStart 截止时间下限，可为 null
     * @param dueEnd 截止时间上限，可为 null
     * @param page 页码，从 1 开始
     * @param pageSize 每页大小
     * @return 分页任务列表
     */
    PageResult<TaskResponse> queryCurrentUserTasks(
        @NonNull String scope,
        @Nullable TaskStatus status,
        @Nullable TaskPriority priority,
        @Nullable LocalDateTime dueStart,
        @Nullable LocalDateTime dueEnd,
        @Nullable String keyword,
        int page,
        int pageSize
    );

    /**
     * 获取当前用户可访问的任务详情。
     *
     * @param id 任务 id
     * @return 任务详情
     */
    TaskResponse getTaskById(@NonNull Long id);

    /**
     * 更新当前用户拥有的任务。
     *
     * @param id 任务 id
     * @param request 更新请求体
     * @return 更新后的任务响应
     */
    TaskResponse updateTask(@NonNull Long id, @NonNull TaskUpdateRequest request);

    /**
     * 删除当前用户拥有的任务。
     *
     * @param id 任务 id
     */
    void deleteTask(@NonNull Long id);

    /**
     * 查询当前用户的个人任务下拉候选项，仅返回 id 与 title。
     *
     * @param keyword 关键字，可为 null
     * @return 匹配的个人任务列表
     */
    List<TaskOptionResponse> getPersonalTaskOptions(@Nullable String keyword);

    TaskDependencyListResponse getTaskDependencies(@NonNull Long id);

    void addTaskDependency(@NonNull Long id, @NonNull Long dependsOnTaskId);

    void deleteTaskDependency(@NonNull Long id, @NonNull Long dependsOnTaskId);

    TaskDependencyGraphResponse getDependencyGraph();

    PageResult<TaskOperationLogResponse> queryTaskLogs(
        @NonNull Long id,
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        int page,
        int pageSize
    );

    PageResult<TaskOperationLogResponse> queryCurrentUserTaskLogs(
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        @Nullable String keyword,
        @Nullable LocalDateTime startTime,
        @Nullable LocalDateTime endTime,
        int page,
        int pageSize
    );
}
