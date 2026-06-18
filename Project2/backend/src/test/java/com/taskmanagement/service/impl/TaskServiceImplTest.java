package com.taskmanagement.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmanagement.common.utils.TaskMapper;
import com.taskmanagement.dto.request.TaskCreateRequest;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.enums.TaskLogObjectType;
import com.taskmanagement.entity.enums.TaskLogOperationType;
import com.taskmanagement.entity.enums.TaskPriority;
import com.taskmanagement.entity.enums.TaskStatus;
import com.taskmanagement.repository.TaskAssigneeRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.security.CurrentUserProvider;
import com.taskmanagement.service.TaskDependencyService;
import com.taskmanagement.service.TaskOperationLogService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskServiceImpl log tests")
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskAssigneeRepository taskAssigneeRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private TaskDependencyService taskDependencyService;
    @Mock
    private TaskOperationLogService taskOperationLogService;
    @Spy
    private TaskMapper taskMapper = new TaskMapper();

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    @DisplayName("creating personal task writes a CREATE TASK_INFO log")
    void shouldWriteLogAfterCreatingPersonalTask() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(100L);
            task.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
            task.setUpdatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
            return task;
        });

        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Task backend module");
        request.setDescription("task operation logs");
        request.setStatus(TaskStatus.TODO);
        request.setPriority(TaskPriority.HIGH);
        request.setDueAt(LocalDateTime.of(2026, 6, 4, 23, 59));

        taskService.createTask(request);

        verify(taskOperationLogService).record(
            any(Task.class),
            eq(TaskLogOperationType.CREATE),
            eq(TaskLogObjectType.TASK_INFO),
            eq("Created personal task [Task backend module]")
        );
    }
}
