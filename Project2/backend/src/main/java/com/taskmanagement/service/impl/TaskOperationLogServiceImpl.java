package com.taskmanagement.service.impl;

import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.common.exception.BadRequestException;
import com.taskmanagement.common.exception.ForbiddenException;
import com.taskmanagement.common.exception.TaskAccessDeniedException;
import com.taskmanagement.common.exception.TaskNotFoundException;
import com.taskmanagement.common.exception.TeamNotFoundException;
import com.taskmanagement.common.exception.UserNotFoundException;
import com.taskmanagement.dto.response.TaskOperationLogResponse;
import com.taskmanagement.dto.response.UserInfoResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.TaskOperationLog;
import com.taskmanagement.entity.Team;
import com.taskmanagement.entity.User;
import com.taskmanagement.entity.enums.TaskLogObjectType;
import com.taskmanagement.entity.enums.TaskLogOperationType;
import com.taskmanagement.entity.enums.TaskLogScopeType;
import com.taskmanagement.repository.TaskOperationLogRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.TeamMemberRepository;
import com.taskmanagement.repository.TeamRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CurrentUserProvider;
import com.taskmanagement.service.TaskOperationLogService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
public class TaskOperationLogServiceImpl implements TaskOperationLogService {

    private static final String OPERATION_TIME = "operationTime";
    private static final int MAX_SUMMARY_LENGTH = 500;

    private final TaskOperationLogRepository taskOperationLogRepository;
    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public void record(
        @NonNull Task task,
        @NonNull TaskLogOperationType operationType,
        @NonNull TaskLogObjectType objectType,
        @NonNull String summary
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        User operator = userRepository.findById(currentUserId).orElseThrow(UserNotFoundException::new);

        TaskOperationLog log = new TaskOperationLog();
        log.setOperatorId(operator.getId());
        log.setOperatorUsername(operator.getUsername());
        log.setOperationType(operationType);
        log.setObjectType(objectType);
        log.setScopeType(task.getTeamId() == null ? TaskLogScopeType.PERSONAL : TaskLogScopeType.TEAM);
        log.setTaskId(task.getId());
        log.setTaskTitle(task.getTitle());
        log.setSummary(limitSummary(summary));

        if (task.getTeamId() != null) {
            Team team = teamRepository.findById(task.getTeamId()).orElseThrow(TeamNotFoundException::new);
            log.setTeamId(team.getId());
            log.setTeamName(team.getName());
        }

        taskOperationLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TaskOperationLogResponse> queryPersonalTaskLogs(
        @NonNull Long taskId,
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        int page,
        int pageSize
    ) {
        validatePersonalObjectType(objectType);
        Long currentUserId = currentUserProvider.getCurrentUserId();
        assertPersonalLogAccess(taskId, currentUserId);
        Specification<TaskOperationLog> spec = baseSpec(TaskLogScopeType.PERSONAL, operationType, objectType)
            .and((root, query, cb) -> cb.equal(root.get("taskId"), taskId))
            .and((root, query, cb) -> cb.equal(root.get("operatorId"), currentUserId));
        return query(spec, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TaskOperationLogResponse> queryCurrentUserPersonalLogs(
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        @Nullable String keyword,
        @Nullable LocalDateTime startTime,
        @Nullable LocalDateTime endTime,
        int page,
        int pageSize
    ) {
        validatePersonalObjectType(objectType);
        validateTimeRange(startTime, endTime);
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Specification<TaskOperationLog> spec = baseSpec(TaskLogScopeType.PERSONAL, operationType, objectType)
            .and((root, query, cb) -> cb.equal(root.get("operatorId"), currentUserId))
            .and(keywordSpec(keyword))
            .and(timeRangeSpec(startTime, endTime));
        return query(spec, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TaskOperationLogResponse> queryTeamTaskLogs(
        @NonNull Long teamId,
        @NonNull Long taskId,
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        int page,
        int pageSize
    ) {
        assertTeamLogAccess(teamId);
        assertTeamTaskLogAccess(teamId, taskId);
        Specification<TaskOperationLog> spec = baseSpec(TaskLogScopeType.TEAM, operationType, objectType)
            .and((root, query, cb) -> cb.equal(root.get("teamId"), teamId))
            .and((root, query, cb) -> cb.equal(root.get("taskId"), taskId));
        return query(spec, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TaskOperationLogResponse> queryTeamLogs(
        @NonNull Long teamId,
        @Nullable TaskLogOperationType operationType,
        @Nullable TaskLogObjectType objectType,
        @Nullable String keyword,
        @Nullable LocalDateTime startTime,
        @Nullable LocalDateTime endTime,
        int page,
        int pageSize
    ) {
        validateTimeRange(startTime, endTime);
        assertTeamLogAccess(teamId);
        Specification<TaskOperationLog> spec = baseSpec(TaskLogScopeType.TEAM, operationType, objectType)
            .and((root, query, cb) -> cb.equal(root.get("teamId"), teamId))
            .and(keywordSpec(keyword))
            .and(timeRangeSpec(startTime, endTime));
        return query(spec, page, pageSize);
    }

    private void assertPersonalLogAccess(Long taskId, Long currentUserId) {
        if (taskRepository.findByIdAndCreatorUserIdAndTeamIdIsNull(taskId, currentUserId).isPresent()) {
            return;
        }
        if (taskRepository.existsById(taskId)) {
            throw new TaskAccessDeniedException("No permission to view this task log");
        }
        boolean hasHistoricalLog = taskOperationLogRepository.existsByTaskIdAndScopeTypeAndOperatorId(
            taskId, TaskLogScopeType.PERSONAL, currentUserId);
        if (!hasHistoricalLog) {
            throw new TaskNotFoundException();
        }
    }

    private void assertTeamLogAccess(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new TeamNotFoundException();
        }
        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new ForbiddenException("No permission to view this team task log");
        }
    }

    private void assertTeamTaskLogAccess(Long teamId, Long taskId) {
        if (taskRepository.findByIdAndTeamId(taskId, teamId).isPresent()) {
            return;
        }
        if (taskRepository.existsById(taskId)) {
            throw new ForbiddenException("No permission to view this team task log");
        }
        boolean hasHistoricalLog = taskOperationLogRepository.existsByTaskIdAndScopeTypeAndTeamId(
            taskId, TaskLogScopeType.TEAM, teamId);
        if (!hasHistoricalLog) {
            throw new TaskNotFoundException();
        }
    }

    private Specification<TaskOperationLog> baseSpec(
        TaskLogScopeType scopeType,
        TaskLogOperationType operationType,
        TaskLogObjectType objectType
    ) {
        Specification<TaskOperationLog> spec = (root, query, cb) -> cb.equal(root.get("scopeType"), scopeType);
        if (operationType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("operationType"), operationType));
        }
        if (objectType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("objectType"), objectType));
        }
        return spec;
    }

    private Specification<TaskOperationLog> keywordSpec(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Specification.where(null);
        }
        String normalized = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("taskTitle")), normalized);
    }

    private Specification<TaskOperationLog> timeRangeSpec(LocalDateTime startTime, LocalDateTime endTime) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(OPERATION_TIME), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(OPERATION_TIME), endTime));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private PageResult<TaskOperationLogResponse> query(
        Specification<TaskOperationLog> spec,
        int page,
        int pageSize
    ) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, OPERATION_TIME));
        Page<TaskOperationLog> logPage = taskOperationLogRepository.findAll(spec, pageRequest);
        List<TaskOperationLogResponse> items = logPage.getContent().stream().map(this::toResponse).toList();
        return new PageResult<>(logPage.getTotalElements(), page, pageSize, items);
    }

    private TaskOperationLogResponse toResponse(TaskOperationLog log) {
        TaskOperationLogResponse.TeamSnapshot team = log.getTeamId() == null
            ? null
            : TaskOperationLogResponse.TeamSnapshot.builder()
                .id(log.getTeamId())
                .name(log.getTeamName())
                .build();

        return TaskOperationLogResponse.builder()
            .id(log.getId())
            .operator(UserInfoResponse.builder()
                .id(log.getOperatorId())
                .username(log.getOperatorUsername())
                .build())
            .operationType(log.getOperationType())
            .objectType(log.getObjectType())
            .scopeType(log.getScopeType())
            .task(TaskOperationLogResponse.TaskSnapshot.builder()
                .id(log.getTaskId())
                .title(log.getTaskTitle())
                .build())
            .team(team)
            .summary(log.getSummary())
            .operationTime(log.getOperationTime())
            .build();
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BadRequestException("startTime cannot be after endTime");
        }
    }

    private void validatePersonalObjectType(TaskLogObjectType objectType) {
        if (TaskLogObjectType.TASK_ASSIGNEE.equals(objectType)) {
            throw new BadRequestException("Personal task logs do not support TASK_ASSIGNEE objectType");
        }
    }

    private String limitSummary(String summary) {
        String safeSummary = summary == null ? "" : summary;
        if (safeSummary.length() <= MAX_SUMMARY_LENGTH) {
            return safeSummary;
        }
        return safeSummary.substring(0, MAX_SUMMARY_LENGTH);
    }
}
