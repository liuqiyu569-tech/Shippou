package com.taskmanagement.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.taskmanagement.common.exception.BadRequestException;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.TaskDependency;
import com.taskmanagement.entity.enums.TaskStatus;
import com.taskmanagement.repository.TaskDependencyRepository;
import com.taskmanagement.repository.TaskRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskDependencyServiceImplTest {

    private static final Long OWNER_ID = 1L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskDependencyRepository taskDependencyRepository;

    @InjectMocks
    private TaskDependencyServiceImpl taskDependencyService;

    @Test
    void shouldRejectAddingCyclicDependency() {
        Task subjectTask = buildPersonalTask(101L);
        Task prerequisite = buildPersonalTask(102L);

        when(taskRepository.findByIdAndCreatorUserIdAndTeamIdIsNull(101L, OWNER_ID))
            .thenReturn(Optional.of(subjectTask));
        when(taskRepository.findByIdAndCreatorUserIdAndTeamIdIsNull(102L, OWNER_ID))
            .thenReturn(Optional.of(prerequisite));
        when(taskDependencyRepository.existsByTaskIdAndDependsOnTaskId(101L, 102L)).thenReturn(false);
        TaskDependency reverseDependency = new TaskDependency();
        reverseDependency.setTaskId(102L);
        reverseDependency.setDependsOnTaskId(101L);
        when(taskDependencyRepository.findAllByTaskId(102L)).thenReturn(List.of(reverseDependency));

        assertThatThrownBy(() -> taskDependencyService.addPersonalTaskDependency(OWNER_ID, 101L, 102L))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRejectDoneTaskWhenPrerequisiteIncompleteRecursive() {
        Task task2 = buildTask(102L, TaskStatus.DONE);
        Task task3 = buildTask(103L, TaskStatus.TODO);

        when(taskRepository.findById(102L)).thenReturn(Optional.of(task2));
        when(taskRepository.findById(103L)).thenReturn(Optional.of(task3));
        when(taskDependencyRepository.findAllByTaskId(101L)).thenReturn(List.of(dependency(101L, 102L)));
        when(taskDependencyRepository.findAllByTaskId(102L)).thenReturn(List.of(dependency(102L, 103L)));

        assertThatThrownBy(() -> taskDependencyService.ensureTaskReadyForDone(101L))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("前置任务未完成，不能将后置任务设置为已完成");
    }

    @Test
    void shouldRejectSettingTaskDoneWhenPrerequisiteIncomplete() {
        Task dependencyTask = buildTask(102L, TaskStatus.TODO);

        when(taskRepository.findById(102L)).thenReturn(Optional.of(dependencyTask));
        when(taskDependencyRepository.findAllByTaskId(101L)).thenReturn(List.of(dependency(101L, 102L)));

        assertThatThrownBy(() -> taskDependencyService.ensureTaskReadyForDone(101L))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("前置任务未完成，不能将后置任务设置为已完成");
    }

    private Task buildPersonalTask(Long id) {
        return buildTask(id, TaskStatus.TODO);
    }

    private Task buildTask(Long id, TaskStatus status) {
        Task task = new Task();
        task.setId(id);
        task.setCreatorUserId(OWNER_ID);
        task.setTeamId(null);
        task.setStatus(status);
        return task;
    }

    private TaskDependency dependency(Long taskId, Long dependsOnTaskId) {
        TaskDependency dependency = new TaskDependency();
        dependency.setTaskId(taskId);
        dependency.setDependsOnTaskId(dependsOnTaskId);
        return dependency;
    }
}
