package com.taskmanagement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmanagement.common.exception.BadRequestException;
import com.taskmanagement.common.exception.TaskAccessDeniedException;
import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.dto.response.TaskOperationLogResponse;
import com.taskmanagement.entity.TaskOperationLog;
import com.taskmanagement.entity.enums.TaskLogObjectType;
import com.taskmanagement.entity.enums.TaskLogOperationType;
import com.taskmanagement.entity.enums.TaskLogScopeType;
import com.taskmanagement.repository.TaskOperationLogRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.TeamMemberRepository;
import com.taskmanagement.repository.TeamRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CurrentUserProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskOperationLogServiceImpl permission tests")
class TaskOperationLogServiceImplTest {

    @Mock
    private TaskOperationLogRepository taskOperationLogRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private TaskOperationLogServiceImpl taskOperationLogService;

    @Test
    @DisplayName("non-owner cannot view personal task logs")
    void shouldDenyPersonalTaskLogsForNonOwner() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(2L);
        when(taskRepository.findByIdAndCreatorUserIdAndTeamIdIsNull(100L, 2L)).thenReturn(Optional.empty());
        when(taskRepository.existsById(100L)).thenReturn(true);

        assertThatThrownBy(() -> taskOperationLogService.queryPersonalTaskLogs(
            100L, null, null, 1, 10))
            .isInstanceOf(TaskAccessDeniedException.class);

        verify(taskOperationLogRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("personal log endpoints reject assignee object type")
    void shouldRejectAssigneeObjectTypeForPersonalLogs() {
        assertThatThrownBy(() -> taskOperationLogService.queryPersonalTaskLogs(
            100L, null, TaskLogObjectType.TASK_ASSIGNEE, 1, 10))
            .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> taskOperationLogService.queryCurrentUserPersonalLogs(
            null, TaskLogObjectType.TASK_ASSIGNEE, null, null, null, 1, 10))
            .isInstanceOf(BadRequestException.class);

        verify(taskOperationLogRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("owner can view historical logs after personal task deletion")
    void shouldQueryHistoricalPersonalLogsAfterTaskDeleted() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(taskRepository.findByIdAndCreatorUserIdAndTeamIdIsNull(100L, 1L)).thenReturn(Optional.empty());
        when(taskRepository.existsById(100L)).thenReturn(false);
        when(taskOperationLogRepository.existsByTaskIdAndScopeTypeAndOperatorId(
            100L, TaskLogScopeType.PERSONAL, 1L)).thenReturn(true);
        when(taskOperationLogRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(createLog())));

        PageResult<TaskOperationLogResponse> result = taskOperationLogService.queryPersonalTaskLogs(
            100L, null, null, 1, 10);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getTask().getTitle()).isEqualTo("Deleted task");
    }

    private TaskOperationLog createLog() {
        TaskOperationLog log = new TaskOperationLog();
        log.setId(1L);
        log.setOperatorId(1L);
        log.setOperatorUsername("alice");
        log.setOperationType(TaskLogOperationType.DELETE);
        log.setObjectType(TaskLogObjectType.TASK_INFO);
        log.setScopeType(TaskLogScopeType.PERSONAL);
        log.setTaskId(100L);
        log.setTaskTitle("Deleted task");
        log.setSummary("Deleted personal task [Deleted task]");
        log.setOperationTime(LocalDateTime.of(2026, 6, 1, 12, 0));
        return log;
    }
}
