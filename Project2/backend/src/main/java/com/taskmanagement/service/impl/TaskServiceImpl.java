package com.taskmanagement.service.impl;

import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.common.exception.BadRequestException;
import com.taskmanagement.common.exception.TaskAccessDeniedException;
import com.taskmanagement.common.exception.TaskNotFoundException;
import com.taskmanagement.common.utils.TaskMapper;
import com.taskmanagement.dto.request.TaskCreateRequest;
import com.taskmanagement.dto.request.TaskUpdateRequest;
import com.taskmanagement.dto.response.TaskOptionResponse;
import com.taskmanagement.dto.response.TaskDependencyGraphResponse;
import com.taskmanagement.dto.response.TaskDependencyListResponse;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.enums.TaskLogObjectType;
import com.taskmanagement.entity.enums.TaskLogOperationType;
import com.taskmanagement.entity.enums.TaskPriority;
import com.taskmanagement.entity.enums.TaskStatus;
import com.taskmanagement.repository.TaskAssigneeRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.security.CurrentUserProvider;
import com.taskmanagement.service.TaskService;
import com.taskmanagement.service.TaskDependencyService;
import com.taskmanagement.service.TaskOperationLogService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Task service implementation for personal task isolation and team task aggregation.
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private static final String CREATED_AT = "createdAt";

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TaskDependencyService taskDependencyService;
    private final TaskOperationLogService taskOperationLogService;
    private final TaskMapper taskMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TaskResponse createTask(@NonNull TaskCreateRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Task task = taskMapper.toEntity(request, currentUserId);
        Task savedTask = taskRepository.save(task);
        taskOperationLogService.record(
            savedTask,
            TaskLogOperationType.CREATE,
            TaskLogObjectType.TASK_INFO,
            "Created personal task [" + savedTask.getTitle() + "]"
        );
        return taskMapper.toResponse(savedTask);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getCurrentUserTasks() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return taskRepository.findAllByCreatorUserIdAndTeamIdIsNullOrderByCreatedAtDesc(currentUserId).stream()
            .map(taskMapper::toResponse)
            .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<TaskResponse> queryCurrentUserTasks(
        @NonNull String scope,
        @Nullable TaskStatus status,
        @Nullable TaskPriority priority,
        @Nullable LocalDateTime dueStart,
        @Nullable LocalDateTime dueEnd,
        @Nullable String keyword,
        int page,
        int pageSize
    ) {
        validateDueRange(dueStart, dueEnd);

        Long currentUserId = currentUserProvider.getCurrentUserId();
        TaskScope taskScope = TaskScope.fromQuery(scope);
        Specification<Task> ownershipSpec = buildOwnershipSpec(taskScope, currentUserId);
        if (ownershipSpec == null) {
            return new PageResult<>(0, page, pageSize, List.of());
        }

        Specification<Task> spec = ownershipSpec.and(buildFilterSpec(status, priority, dueStart, dueEnd, keyword));
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, CREATED_AT));
        Page<Task> taskPage = taskRepository.findAll(spec, pageRequest);
        List<TaskResponse> items = taskPage.getContent().stream()
            .map(taskMapper::toResponse)
            .toList();
        return new PageResult<>(taskPage.getTotalElements(), page, pageSize, items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(@NonNull Long id) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Task task = findPersonalTaskOrThrow(id, currentUserId);
        return taskMapper.toResponse(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TaskResponse updateTask(@NonNull Long id, @NonNull TaskUpdateRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Task task = findPersonalTaskOrThrow(id, currentUserId);
        if (request.getStatus() == TaskStatus.DONE) {
            taskDependencyService.ensureTaskReadyForDone(id);
        }
        TaskSnapshot before = TaskSnapshot.from(task);
        taskMapper.applyUpdate(task, request);
        Task savedTask = taskRepository.save(task);
        taskOperationLogService.record(
            savedTask,
            TaskLogOperationType.UPDATE,
            TaskLogObjectType.TASK_INFO,
            buildTaskUpdateSummary(before, TaskSnapshot.from(savedTask))
        );
        return taskMapper.toResponse(savedTask);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteTask(@NonNull Long id) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Task task = findPersonalTaskOrThrow(id, currentUserId);
        TaskSnapshot before = TaskSnapshot.from(task);
        taskDependencyService.clearTaskDependencyLinks(id);
        taskRepository.delete(task);
        taskOperationLogService.record(
            before.toTask(),
            TaskLogOperationType.DELETE,
            TaskLogObjectType.TASK_INFO,
            "Deleted personal task [" + before.title() + "]"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDependencyListResponse getTaskDependencies(@NonNull Long id) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return taskDependencyService.getPersonalTaskDependencies(currentUserId, id);
    }

    @Override
    @Transactional
    public void addTaskDependency(@NonNull Long id, @NonNull Long dependsOnTaskId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Task task = findPersonalTaskOrThrow(id, currentUserId);
        Task dependency = findPersonalTaskOrThrow(dependsOnTaskId, currentUserId);
        taskDependencyService.addPersonalTaskDependency(currentUserId, id, dependsOnTaskId);
        taskOperationLogService.record(
            task,
            TaskLogOperationType.CREATE,
            TaskLogObjectType.TASK_DEPENDENCY,
            "Added prerequisite task [" + dependency.getTitle() + "]"
        );
    }

    @Override
    @Transactional
    public void deleteTaskDependency(@NonNull Long id, @NonNull Long dependsOnTaskId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Task task = findPersonalTaskOrThrow(id, currentUserId);
        Task dependency = findPersonalTaskOrThrow(dependsOnTaskId, currentUserId);
        taskDependencyService.deletePersonalTaskDependency(currentUserId, id, dependsOnTaskId);
        taskOperationLogService.record(
            task,
            TaskLogOperationType.DELETE,
            TaskLogObjectType.TASK_DEPENDENCY,
            "Removed prerequisite task [" + dependency.getTitle() + "]"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDependencyGraphResponse getDependencyGraph() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return taskDependencyService.getPersonalDependencyGraph(currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<com.taskmanagement.dto.response.TaskOperationLogResponse> queryTaskLogs(
        @NonNull Long id,
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        int page,
        int pageSize
    ) {
        return taskOperationLogService.queryPersonalTaskLogs(id, operationType, objectType, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<com.taskmanagement.dto.response.TaskOperationLogResponse> queryCurrentUserTaskLogs(
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        @Nullable String keyword,
        @Nullable LocalDateTime startTime,
        @Nullable LocalDateTime endTime,
        int page,
        int pageSize
    ) {
        return taskOperationLogService.queryCurrentUserPersonalLogs(
            operationType, objectType, keyword, startTime, endTime, page, pageSize);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<TaskOptionResponse> getPersonalTaskOptions(@Nullable String keyword) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        List<Task> tasks = (keyword == null || keyword.isBlank())
            ? taskRepository.findPersonalTaskOptions(currentUserId)
            : taskRepository.findPersonalTaskOptionsByKeyword(
                currentUserId, keyword.trim().toLowerCase(Locale.ROOT));
        return tasks.stream()
            .map(t -> TaskOptionResponse.builder().id(t.getId()).title(t.getTitle()).build())
            .toList();
    }

    private Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id).orElseThrow(TaskNotFoundException::new);
    }

    private Task findPersonalTaskOrThrow(Long id, Long currentUserId) {
        return taskRepository.findByIdAndCreatorUserIdAndTeamIdIsNull(id, currentUserId)
            .orElseThrow(() -> {
                Task task = findTaskOrThrow(id);
                checkOwnership(task, currentUserId, "无权访问此任务");
                return new TaskNotFoundException();
            });
    }

    private void checkOwnership(Task task, Long currentUserId, String forbiddenMessage) {
        if (task.getCreatorUserId().equals(currentUserId)) {
            return;
        }
        throw new TaskAccessDeniedException(forbiddenMessage);
    }

    private void validateDueRange(LocalDateTime dueStart, LocalDateTime dueEnd) {
        if (dueStart != null && dueEnd != null && dueStart.isAfter(dueEnd)) {
            throw new BadRequestException("开始时间不能晚于结束时间");
        }
    }

    private Specification<Task> buildOwnershipSpec(TaskScope scope, Long currentUserId) {
        Specification<Task> personalSpec = personalTaskSpec(currentUserId);
        if (scope == TaskScope.PERSONAL) {
            return personalSpec;
        }

        List<Long> assignedTaskIds = taskAssigneeRepository.findTaskIdsByUserId(currentUserId);
        Specification<Task> teamSpec = assignedTeamTaskSpec(assignedTaskIds);
        if (scope == TaskScope.TEAM) {
            return teamSpec;
        }
        if (teamSpec == null) {
            return personalSpec;
        }
        return personalSpec.or(teamSpec);
    }

    private Specification<Task> personalTaskSpec(Long currentUserId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.equal(root.get("creatorUserId"), currentUserId),
            criteriaBuilder.isNull(root.get("teamId"))
        );
    }

    private Specification<Task> assignedTeamTaskSpec(List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.isNotNull(root.get("teamId")),
            root.get("id").in(taskIds)
        );
    }

    private Specification<Task> buildFilterSpec(
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueStart,
        LocalDateTime dueEnd,
        String keyword
    ) {
        Specification<Task> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), priority));
        }
        if (dueStart != null) {
            spec = spec.and(
                (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("dueAt"), dueStart));
        }
        if (dueEnd != null) {
            spec = spec.and(
                (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("dueAt"), dueEnd));
        }
        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
            ));
        }
        return spec;
    }

    private String buildTaskUpdateSummary(TaskSnapshot before, TaskSnapshot after) {
        List<String> changes = new java.util.ArrayList<>();
        if (!Objects.equals(before.title(), after.title())) {
            changes.add("Title changed from [" + before.title() + "] to [" + after.title() + "]");
        }
        if (!Objects.equals(before.description(), after.description())) {
            changes.add("Description changed");
        }
        if (before.status() != after.status()) {
            changes.add("Status changed from " + before.status() + " to " + after.status());
        }
        if (before.priority() != after.priority()) {
            changes.add("Priority changed from " + before.priority() + " to " + after.priority());
        }
        if (!Objects.equals(before.dueAt(), after.dueAt())) {
            changes.add("Due time changed from " + formatValue(before.dueAt()) + " to " + formatValue(after.dueAt()));
        }
        if (changes.isEmpty()) {
            return "Updated task without field changes";
        }
        return String.join("; ", changes);
    }

    private String formatValue(Object value) {
        return value == null ? "none" : value.toString();
    }

    private record TaskSnapshot(
        Long id,
        Long creatorUserId,
        Long teamId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueAt
    ) {
        static TaskSnapshot from(Task task) {
            return new TaskSnapshot(
                task.getId(),
                task.getCreatorUserId(),
                task.getTeamId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueAt()
            );
        }

        Task toTask() {
            Task task = new Task();
            task.setId(id);
            task.setCreatorUserId(creatorUserId);
            task.setTeamId(teamId);
            task.setTitle(title);
            task.setDescription(description);
            task.setStatus(status);
            task.setPriority(priority);
            task.setDueAt(dueAt);
            return task;
        }
    }

    private enum TaskScope {
        PERSONAL,
        TEAM,
        ALL;

        private static TaskScope fromQuery(String value) {
            if (!StringUtils.hasText(value)) {
                return ALL;
            }
            try {
                return TaskScope.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("查询范围必须为 personal、team 或 all");
            }
        }
    }
}
