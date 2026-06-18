package com.taskmanagement.service.impl;

import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.common.exception.BadRequestException;
import com.taskmanagement.common.exception.ForbiddenException;
import com.taskmanagement.common.exception.TaskNotFoundException;
import com.taskmanagement.common.exception.TeamNotFoundException;
import com.taskmanagement.common.utils.DueStatusCalculator;
import com.taskmanagement.dto.request.TeamTaskCreateRequest;
import com.taskmanagement.dto.request.TeamTaskUpdateRequest;
import com.taskmanagement.dto.response.TaskDependencyGraphResponse;
import com.taskmanagement.dto.response.TaskDependencyListResponse;
import com.taskmanagement.dto.response.TaskOptionResponse;
import com.taskmanagement.dto.response.TeamTaskResponse;
import com.taskmanagement.dto.response.TeamTaskResponse.AssigneeInfo;
import com.taskmanagement.dto.response.TeamTaskStatsResponse;
import com.taskmanagement.dto.response.UserInfoResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.TaskAssignee;
import com.taskmanagement.entity.TeamMember;
import com.taskmanagement.entity.User;
import com.taskmanagement.entity.enums.DueStatus;
import com.taskmanagement.entity.enums.TaskLogObjectType;
import com.taskmanagement.entity.enums.TaskLogOperationType;
import com.taskmanagement.entity.enums.TaskPriority;
import com.taskmanagement.entity.enums.TaskStatus;
import com.taskmanagement.entity.enums.TeamRole;
import com.taskmanagement.repository.TaskAssigneeRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.TeamMemberRepository;
import com.taskmanagement.repository.TeamRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CurrentUserProvider;
import com.taskmanagement.service.TaskDependencyService;
import com.taskmanagement.service.TaskOperationLogService;
import com.taskmanagement.service.TeamTaskService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TeamTaskServiceImpl implements TeamTaskService {

    private static final String CREATED_AT = "createdAt";
    private static final String ERR_ONLY_OWNER_ADMIN = "仅 Owner/Admin 可执行此操作";
    private static final String ERR_MEMBER_NOT_IN_TEAM = "仅团队成员可访问该团队";

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TaskDependencyService taskDependencyService;
    private final TaskOperationLogService taskOperationLogService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public TeamTaskResponse createTeamTask(@NonNull Long teamId, @NonNull TeamTaskCreateRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        TeamRole role = requireTeamMemberRole(teamId, currentUserId);
        requireManagerRole(role, ERR_ONLY_OWNER_ADMIN);

        Task task = new Task();
        task.setCreatorUserId(currentUserId);
        task.setTeamId(teamId);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueAt(request.getDueAt());

        Task savedTask = taskRepository.save(task);
        taskOperationLogService.record(
            savedTask,
            TaskLogOperationType.CREATE,
            TaskLogObjectType.TASK_INFO,
            "Created team task [" + savedTask.getTitle() + "]"
        );
        return toResponse(savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TeamTaskResponse> getTeamTasks(
        @NonNull Long teamId,
        @Nullable TaskStatus status,
        @Nullable TaskPriority priority,
        @Nullable Long assigneeId,
        @Nullable LocalDateTime dueStart,
        @Nullable LocalDateTime dueEnd,
        @Nullable String keyword,
        int page,
        int pageSize
    ) {
        validateDueRange(dueStart, dueEnd);
        requireTeamMemberRole(teamId, currentUserProvider.getCurrentUserId());

        List<Long> assignedTaskIds = null;
        if (assigneeId != null) {
            if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, assigneeId)) {
                throw new BadRequestException("指派用户不是团队成员");
            }
            assignedTaskIds = taskAssigneeRepository.findTaskIdsByUserId(assigneeId);
            if (assignedTaskIds.isEmpty()) {
                return new PageResult<>(0, page, pageSize, List.of());
            }
        }

        Specification<Task> spec = buildTeamTaskSpec(teamId, status, priority, dueStart, dueEnd, assignedTaskIds, keyword);
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, CREATED_AT));
        Page<Task> taskPage = taskRepository.findAll(spec, pageRequest);
        List<TeamTaskResponse> items = taskPage.getContent().stream().map(this::toResponse).toList();
        return new PageResult<>(taskPage.getTotalElements(), page, pageSize, items);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamTaskStatsResponse getTeamTaskStats(@NonNull Long teamId, int upcomingDays) {
        requireTeamMemberRole(teamId, currentUserProvider.getCurrentUserId());
        List<Task> tasks = taskRepository.findAllByTeamId(teamId);

        Map<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
        Map<TaskPriority, Long> byPriority = new EnumMap<>(TaskPriority.class);
        for (TaskStatus status : TaskStatus.values()) {
            byStatus.put(status, 0L);
        }
        for (TaskPriority priority : TaskPriority.values()) {
            byPriority.put(priority, 0L);
        }

        long overdueCount = 0;
        long upcomingDueCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Task task : tasks) {
            byStatus.compute(task.getStatus(), (key, value) -> value == null ? 1L : value + 1L);
            byPriority.compute(task.getPriority(), (key, value) -> value == null ? 1L : value + 1L);
            DueStatus dueStatus = DueStatusCalculator.calculate(task, upcomingDays, now);
            if (dueStatus == DueStatus.OVERDUE) {
                overdueCount++;
            } else if (dueStatus == DueStatus.UPCOMING_DUE) {
                upcomingDueCount++;
            }
        }

        return TeamTaskStatsResponse.builder()
            .total(tasks.size())
            .byStatus(byStatus)
            .byPriority(byPriority)
            .overdueCount(overdueCount)
            .upcomingDueCount(upcomingDueCount)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TeamTaskResponse getTeamTask(@NonNull Long teamId, @NonNull Long taskId) {
        requireTeamMemberRole(teamId, currentUserProvider.getCurrentUserId());
        return toResponse(findTeamTaskOrThrow(teamId, taskId));
    }

    @Override
    @Transactional
    public TeamTaskResponse updateTeamTask(@NonNull Long teamId, @NonNull Long taskId, @NonNull TeamTaskUpdateRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        TeamRole role = requireTeamMemberRole(teamId, currentUserId);
        Task task = findTeamTaskOrThrow(teamId, taskId);

        if (hasManagerRole(role)) {
            if (request.getStatus() == TaskStatus.DONE) {
                taskDependencyService.ensureTaskReadyForDone(taskId);
            }
            TaskSnapshot before = TaskSnapshot.from(task);
            applyManagerUpdate(task, request);
            Task savedTask = taskRepository.save(task);
            taskOperationLogService.record(
                savedTask,
                TaskLogOperationType.UPDATE,
                TaskLogObjectType.TASK_INFO,
                buildTaskUpdateSummary(before, TaskSnapshot.from(savedTask))
            );
            return toResponse(savedTask);
        }

        if (!taskAssigneeRepository.existsByTaskIdAndUserId(taskId, currentUserId)) {
            throw new ForbiddenException("仅被指派成员可更新该任务");
        }

        boolean hasNonStatusField = request.getTitle() != null
            || request.getDescription() != null
            || request.getPriority() != null
            || request.getDueAt() != null;
        if (hasNonStatusField) {
            throw new ForbiddenException("普通成员仅可更新状态");
        }
        if (request.getStatus() == null) {
            throw new BadRequestException("必须提供状态");
        }
        if (request.getStatus() == TaskStatus.DONE) {
            taskDependencyService.ensureTaskReadyForDone(taskId);
        }

        TaskSnapshot before = TaskSnapshot.from(task);
        task.setStatus(request.getStatus());
        Task savedTask = taskRepository.save(task);
        taskOperationLogService.record(
            savedTask,
            TaskLogOperationType.UPDATE,
            TaskLogObjectType.TASK_INFO,
            buildTaskUpdateSummary(before, TaskSnapshot.from(savedTask))
        );
        return toResponse(savedTask);
    }

    @Override
    @Transactional
    public void deleteTeamTask(@NonNull Long teamId, @NonNull Long taskId) {
        TeamRole role = requireTeamMemberRole(teamId, currentUserProvider.getCurrentUserId());
        requireManagerRole(role, ERR_ONLY_OWNER_ADMIN);
        Task task = findTeamTaskOrThrow(teamId, taskId);
        TaskSnapshot before = TaskSnapshot.from(task);
        taskDependencyService.clearTaskDependencyLinks(taskId);
        taskAssigneeRepository.deleteByTaskId(taskId);
        taskRepository.delete(task);
        taskOperationLogService.record(
            before.toTask(),
            TaskLogOperationType.DELETE,
            TaskLogObjectType.TASK_INFO,
            "Deleted team task [" + before.title() + "]"
        );
    }

    @Override
    @Transactional
    public void assignTask(@NonNull Long teamId, @NonNull Long taskId, @NonNull List<Long> userIds) {
        TeamRole role = requireTeamMemberRole(teamId, currentUserProvider.getCurrentUserId());
        requireManagerRole(role, ERR_ONLY_OWNER_ADMIN);
        Task task = findTeamTaskOrThrow(teamId, taskId);

        Set<Long> distinctUserIds = new LinkedHashSet<>(userIds);
        validateAssignees(teamId, distinctUserIds);
        List<Long> oldAssigneeIds = taskAssigneeRepository.findAllByTaskId(taskId).stream()
            .map(TaskAssignee::getUserId)
            .toList();

        taskAssigneeRepository.deleteAll(taskAssigneeRepository.findAllByTaskId(taskId));
        taskAssigneeRepository.flush();
        List<TaskAssignee> assignments = distinctUserIds.stream()
            .map(userId -> createAssignment(taskId, userId))
            .toList();
        taskAssigneeRepository.saveAll(assignments);
        taskOperationLogService.record(
            task,
            TaskLogOperationType.UPDATE,
            TaskLogObjectType.TASK_ASSIGNEE,
            "Assignees changed from [" + joinUsernames(oldAssigneeIds) + "] to [" + joinUsernames(distinctUserIds) + "]"
        );
    }

    @Override
    @Transactional
    public void unassignTask(@NonNull Long teamId, @NonNull Long taskId, @NonNull Long userId) {
        TeamRole role = requireTeamMemberRole(teamId, currentUserProvider.getCurrentUserId());
        requireManagerRole(role, ERR_ONLY_OWNER_ADMIN);
        Task task = findTeamTaskOrThrow(teamId, taskId);
        boolean existed = taskAssigneeRepository.existsByTaskIdAndUserId(taskId, userId);
        taskAssigneeRepository.deleteByTaskIdAndUserId(taskId, userId);
        if (existed) {
            taskOperationLogService.record(
                task,
                TaskLogOperationType.DELETE,
                TaskLogObjectType.TASK_ASSIGNEE,
                "Removed assignee [" + joinUsernames(List.of(userId)) + "]"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDependencyListResponse getTeamTaskDependencies(@NonNull Long teamId, @NonNull Long taskId) {
        requireTeamMemberRole(teamId, currentUserProvider.getCurrentUserId());
        return taskDependencyService.getTeamTaskDependencies(currentUserProvider.getCurrentUserId(), teamId, taskId);
    }

    @Override
    @Transactional
    public void addTeamTaskDependency(@NonNull Long teamId, @NonNull Long taskId, @NonNull Long dependsOnTaskId) {
        TeamRole role = requireTeamMemberRole(teamId, currentUserProvider.getCurrentUserId());
        requireManagerRole(role, ERR_ONLY_OWNER_ADMIN);
        Task task = findTeamTaskOrThrow(teamId, taskId);
        Task dependency = findTeamTaskOrThrow(teamId, dependsOnTaskId);
        taskDependencyService.addTeamTaskDependency(currentUserProvider.getCurrentUserId(), teamId, taskId, dependsOnTaskId);
        taskOperationLogService.record(
            task,
            TaskLogOperationType.CREATE,
            TaskLogObjectType.TASK_DEPENDENCY,
            "Added prerequisite task [" + dependency.getTitle() + "]"
        );
    }

    @Override
    @Transactional
    public void deleteTeamTaskDependency(@NonNull Long teamId, @NonNull Long taskId, @NonNull Long dependsOnTaskId) {
        TeamRole role = requireTeamMemberRole(teamId, currentUserProvider.getCurrentUserId());
        requireManagerRole(role, ERR_ONLY_OWNER_ADMIN);
        Task task = findTeamTaskOrThrow(teamId, taskId);
        Task dependency = findTeamTaskOrThrow(teamId, dependsOnTaskId);
        taskDependencyService.deleteTeamTaskDependency(currentUserProvider.getCurrentUserId(), teamId, taskId, dependsOnTaskId);
        taskOperationLogService.record(
            task,
            TaskLogOperationType.DELETE,
            TaskLogObjectType.TASK_DEPENDENCY,
            "Removed prerequisite task [" + dependency.getTitle() + "]"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDependencyGraphResponse getTeamDependencyGraph(@NonNull Long teamId) {
        requireTeamMemberRole(teamId, currentUserProvider.getCurrentUserId());
        return taskDependencyService.getTeamDependencyGraph(currentUserProvider.getCurrentUserId(), teamId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskOptionResponse> getTeamTaskOptions(@NonNull Long teamId, @Nullable String keyword) {
        requireTeamMemberRole(teamId, currentUserProvider.getCurrentUserId());
        List<Task> tasks = (keyword == null || keyword.isBlank())
            ? taskRepository.findTeamTaskOptions(teamId)
            : taskRepository.findTeamTaskOptionsByKeyword(
                teamId, keyword.trim().toLowerCase(Locale.ROOT));
        return tasks.stream()
            .map(t -> TaskOptionResponse.builder().id(t.getId()).title(t.getTitle()).build())
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<com.taskmanagement.dto.response.TaskOperationLogResponse> queryTeamTaskLogs(
        @NonNull Long teamId,
        @NonNull Long taskId,
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        int page,
        int pageSize
    ) {
        return taskOperationLogService.queryTeamTaskLogs(teamId, taskId, operationType, objectType, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<com.taskmanagement.dto.response.TaskOperationLogResponse> queryTeamLogs(
        @NonNull Long teamId,
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        @Nullable String keyword,
        @Nullable LocalDateTime startTime,
        @Nullable LocalDateTime endTime,
        int page,
        int pageSize
    ) {
        return taskOperationLogService.queryTeamLogs(
            teamId, operationType, objectType, keyword, startTime, endTime, page, pageSize);
    }

    private TeamRole requireTeamMemberRole(Long teamId, Long userId) {
        if (!teamRepository.existsById(teamId)) {
            throw new TeamNotFoundException();
        }
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .map(TeamMember::getRole)
            .orElseThrow(() -> new ForbiddenException(ERR_MEMBER_NOT_IN_TEAM));
    }

    private Task findTeamTaskOrThrow(Long teamId, Long taskId) {
        return taskRepository.findByIdAndTeamId(taskId, teamId).orElseThrow(TaskNotFoundException::new);
    }

    private void validateDueRange(LocalDateTime dueStart, LocalDateTime dueEnd) {
        if (dueStart != null && dueEnd != null && dueStart.isAfter(dueEnd)) {
            throw new BadRequestException("开始时间不能晚于结束时间");
        }
    }

    private Specification<Task> buildTeamTaskSpec(
        Long teamId,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueStart,
        LocalDateTime dueEnd,
        List<Long> assignedTaskIds,
        String keyword
    ) {
        Specification<Task> spec = (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("teamId"), teamId);
        if (status != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), priority));
        }
        if (dueStart != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("dueAt"), dueStart));
        }
        if (dueEnd != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("dueAt"), dueEnd));
        }
        if (assignedTaskIds != null) {
            spec = spec.and((root, query, criteriaBuilder) -> root.get("id").in(assignedTaskIds));
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

    private void applyManagerUpdate(Task task, TeamTaskUpdateRequest request) {
        boolean changed = false;
        if (request.getTitle() != null) {
            if (!StringUtils.hasText(request.getTitle())) {
                throw new BadRequestException("任务标题不能为空");
            }
            task.setTitle(request.getTitle());
            changed = true;
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
            changed = true;
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
            changed = true;
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
            changed = true;
        }
        if (request.getDueAt() != null) {
            task.setDueAt(request.getDueAt());
            changed = true;
        }
        if (!changed) {
            throw new BadRequestException("至少需要提供一个字段");
        }
    }

    private void validateAssignees(Long teamId, Set<Long> userIds) {
        if (userIds.isEmpty()) {
            throw new BadRequestException("指派用户列表不能为空");
        }
        Set<Long> teamUserIds = teamMemberRepository.findAllByTeamIdAndUserIdIn(teamId, userIds).stream()
            .map(TeamMember::getUserId)
            .collect(java.util.stream.Collectors.toSet());
        List<Long> invalidUserIds = userIds.stream()
            .filter(userId -> !teamUserIds.contains(userId))
            .toList();
        if (!invalidUserIds.isEmpty()) {
            throw new BadRequestException("被指派用户必须是团队成员");
        }
    }

    private TaskAssignee createAssignment(Long taskId, Long userId) {
        TaskAssignee assignment = new TaskAssignee();
        assignment.setTaskId(taskId);
        assignment.setUserId(userId);
        return assignment;
    }

    private void requireManagerRole(TeamRole role, String message) {
        if (!hasManagerRole(role)) {
            throw new ForbiddenException(message);
        }
    }

    private boolean hasManagerRole(TeamRole role) {
        return role == TeamRole.OWNER || role == TeamRole.ADMIN;
    }

    private TeamTaskResponse toResponse(Task task) {
        Map<Long, String> usernames = loadUsernames(collectRelatedUserIds(task));
        UserInfoResponse creator = UserInfoResponse.builder()
            .id(task.getCreatorUserId())
            .username(usernames.getOrDefault(task.getCreatorUserId(), "unknown"))
            .build();
        List<AssigneeInfo> assignees = taskAssigneeRepository.findAllByTaskId(task.getId()).stream()
            .map(assignment -> AssigneeInfo.builder()
                .userId(assignment.getUserId())
                .username(usernames.getOrDefault(assignment.getUserId(), "unknown"))
                .build())
            .toList();

        return TeamTaskResponse.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .status(task.getStatus())
            .priority(task.getPriority())
            .dueAt(task.getDueAt())
            .dueStatus(DueStatusCalculator.calculate(task))
            .teamId(task.getTeamId())
            .creator(creator)
            .assignees(assignees)
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .build();
    }

    private List<Long> collectRelatedUserIds(Task task) {
        List<Long> userIds = new ArrayList<>();
        userIds.add(task.getCreatorUserId());
        taskAssigneeRepository.findAllByTaskId(task.getId()).stream()
            .map(TaskAssignee::getUserId)
            .forEach(userIds::add);
        return userIds;
    }

    private Map<Long, String> loadUsernames(List<Long> userIds) {
        Map<Long, String> usernames = new HashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            usernames.put(user.getId(), user.getUsername());
        }
        return usernames;
    }

    private String buildTaskUpdateSummary(TaskSnapshot before, TaskSnapshot after) {
        List<String> changes = new ArrayList<>();
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

    private String joinUsernames(Iterable<Long> userIds) {
        List<Long> ids = new ArrayList<>();
        userIds.forEach(ids::add);
        if (ids.isEmpty()) {
            return "none";
        }
        Map<Long, String> usernames = loadUsernames(ids);
        return ids.stream()
            .map(id -> usernames.getOrDefault(id, "user#" + id))
            .collect(java.util.stream.Collectors.joining(", "));
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
}
