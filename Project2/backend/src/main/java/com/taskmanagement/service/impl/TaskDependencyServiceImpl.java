package com.taskmanagement.service.impl;

import com.taskmanagement.common.exception.BadRequestException;
import com.taskmanagement.common.exception.TaskDependencyNotFoundException;
import com.taskmanagement.common.exception.TaskNotFoundException;
import com.taskmanagement.dto.response.TaskDependencyEdgeResponse;
import com.taskmanagement.dto.response.TaskDependencyGraphResponse;
import com.taskmanagement.dto.response.TaskDependencyItemResponse;
import com.taskmanagement.dto.response.TaskDependencyListResponse;
import com.taskmanagement.dto.response.TaskDependencyNodeResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.TaskDependency;
import com.taskmanagement.entity.enums.TaskStatus;
import com.taskmanagement.repository.TaskDependencyRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.service.TaskDependencyService;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskDependencyServiceImpl implements TaskDependencyService {

    private static final String TASK_NOT_IN_SCOPE = "任务不可用";
    private static final String DEPENDENCY_EXISTS_MSG = "该任务依赖已存在";
    private static final String CYCLE_MSG = "不能添加循环依赖";
    private static final String PENDING_PREDEPS_MSG = "前置任务未完成，不能将后置任务设置为已完成";

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;

    @Override
    @Transactional(readOnly = true)
    public TaskDependencyListResponse getPersonalTaskDependencies(Long currentUserId, Long taskId) {
        Task task = findPersonalTaskOrThrow(currentUserId, taskId);
        return buildDependencyList(task.getId());
    }

    @Override
    @Transactional
    public void addPersonalTaskDependency(Long currentUserId, Long taskId, Long dependsOnTaskId) {
        Task subjectTask = findPersonalTaskOrThrow(currentUserId, taskId);
        Task prerequisiteTask = findPersonalTaskOrThrow(currentUserId, dependsOnTaskId);
        addDependency(subjectTask, prerequisiteTask);
    }

    @Override
    @Transactional
    public void deletePersonalTaskDependency(Long currentUserId, Long taskId, Long dependsOnTaskId) {
        findPersonalTaskOrThrow(currentUserId, taskId);
        findPersonalTaskOrThrow(currentUserId, dependsOnTaskId);
        deleteDependency(taskId, dependsOnTaskId);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDependencyGraphResponse getPersonalDependencyGraph(Long currentUserId) {
        List<Task> tasks = taskRepository.findAllByCreatorUserIdAndTeamIdIsNullOrderByCreatedAtDesc(currentUserId);
        return buildDependencyGraph(tasks);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDependencyListResponse getTeamTaskDependencies(Long currentUserId, Long teamId, Long taskId) {
        Task task = findTeamTaskOrThrow(teamId, taskId);
        return buildDependencyList(task.getId());
    }

    @Override
    @Transactional
    public void addTeamTaskDependency(Long currentUserId, Long teamId, Long taskId, Long dependsOnTaskId) {
        Task subjectTask = findTeamTaskOrThrow(teamId, taskId);
        Task prerequisiteTask = findTeamTaskOrThrow(teamId, dependsOnTaskId);
        addDependency(subjectTask, prerequisiteTask);
    }

    @Override
    @Transactional
    public void deleteTeamTaskDependency(Long currentUserId, Long teamId, Long taskId, Long dependsOnTaskId) {
        findTeamTaskOrThrow(teamId, taskId);
        findTeamTaskOrThrow(teamId, dependsOnTaskId);
        deleteDependency(taskId, dependsOnTaskId);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDependencyGraphResponse getTeamDependencyGraph(Long currentUserId, Long teamId) {
        List<Task> tasks = taskRepository.findAllByTeamId(teamId);
        return buildDependencyGraph(tasks);
    }

    @Override
    public void ensureTaskReadyForDone(Long taskId) {
        ensureTaskReadyForDoneWithAdditionalDependency(taskId, null);
    }

    @Override
    public void ensureTaskReadyForDoneWithAdditionalDependency(Long taskId, Long dependsOnTaskId) {
        Set<Long> visited = new HashSet<>();
        if (dependsOnTaskId != null) {
            checkDependencyCompleted(dependsOnTaskId, visited);
        }
        for (TaskDependency dependency : taskDependencyRepository.findAllByTaskId(taskId)) {
            checkDependencyCompleted(dependency.getDependsOnTaskId(), visited);
        }
    }

    @Override
    public void clearTaskDependencyLinks(Long taskId) {
        taskDependencyRepository.deleteAllInvolvingTaskIds(List.of(taskId));
    }

    private void addDependency(Task subjectTask, Task prerequisiteTask) {
        if (subjectTask.getId().equals(prerequisiteTask.getId())) {
            throw new BadRequestException("任务不能依赖自身");
        }
        if (taskDependencyRepository.existsByTaskIdAndDependsOnTaskId(
            subjectTask.getId(),
            prerequisiteTask.getId()
        )) {
            throw new BadRequestException(DEPENDENCY_EXISTS_MSG);
        }
        ensureNoCycle(prerequisiteTask.getId(), subjectTask.getId(), new HashSet<>());
        if (subjectTask.getStatus() == TaskStatus.DONE) {
            ensureTaskReadyForDoneWithAdditionalDependency(subjectTask.getId(), prerequisiteTask.getId());
        }

        TaskDependency dependency = new TaskDependency();
        dependency.setTaskId(subjectTask.getId());
        dependency.setDependsOnTaskId(prerequisiteTask.getId());
        taskDependencyRepository.save(dependency);
    }

    private void deleteDependency(Long taskId, Long dependsOnTaskId) {
        taskDependencyRepository.findByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId)
            .orElseThrow(TaskDependencyNotFoundException::new);
        taskDependencyRepository.deleteByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId);
    }

    private Task findPersonalTaskOrThrow(Long currentUserId, Long taskId) {
        return taskRepository.findByIdAndCreatorUserIdAndTeamIdIsNull(taskId, currentUserId)
            .orElseThrow(TaskNotFoundException::new);
    }

    private Task findTeamTaskOrThrow(Long teamId, Long taskId) {
        return taskRepository.findByIdAndTeamId(taskId, teamId)
            .orElseThrow(TaskNotFoundException::new);
    }

    private TaskDependencyListResponse buildDependencyList(Long taskId) {
        List<TaskDependency> prerequisites = taskDependencyRepository.findAllByTaskId(taskId);
        List<TaskDependency> successors = taskDependencyRepository.findAllByDependsOnTaskId(taskId);

        Set<Long> referencedTaskIds = Stream.concat(
            prerequisites.stream().map(TaskDependency::getDependsOnTaskId),
            successors.stream().map(TaskDependency::getTaskId)
        ).collect(Collectors.toSet());
        referencedTaskIds.add(taskId);

        Map<Long, Task> taskMap = findTasks(referencedTaskIds);

        return TaskDependencyListResponse.builder()
            .prerequisites(prerequisites.stream()
                .map(item -> toDependencyItem(taskMap.get(item.getDependsOnTaskId())))
                .toList())
            .successors(successors.stream()
                .map(item -> toDependencyItem(taskMap.get(item.getTaskId())))
                .toList())
            .build();
    }

    private TaskDependencyGraphResponse buildDependencyGraph(List<Task> tasks) {
        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        Map<Long, Task> taskMap = tasks.stream()
            .collect(Collectors.toMap(Task::getId, task -> task));

        if (taskIds.isEmpty()) {
            return TaskDependencyGraphResponse.builder().build();
        }

        List<TaskDependency> edges = taskDependencyRepository.findAllByTaskIdInOrDependsOnTaskIdIn(taskIds, taskIds);
        List<TaskDependencyEdgeResponse> normalizedEdges = edges.stream()
            .filter(edge -> taskIds.contains(edge.getTaskId()) && taskIds.contains(edge.getDependsOnTaskId()))
            .map(edge -> TaskDependencyEdgeResponse.builder()
                .from(edge.getDependsOnTaskId())
                .to(edge.getTaskId())
                .build())
            .toList();

        List<TaskDependencyNodeResponse> nodes = tasks.stream()
            .map(task -> TaskDependencyNodeResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .status(task.getStatus())
                .build())
            .toList();

        return TaskDependencyGraphResponse.builder()
            .nodes(nodes)
            .edges(normalizedEdges)
            .build();
    }

    private Map<Long, Task> findTasks(Collection<Long> taskIds) {
        return taskRepository.findAllById(taskIds).stream()
            .collect(Collectors.toMap(Task::getId, task -> task, (left, right) -> left));
    }

    private TaskDependencyItemResponse toDependencyItem(@Nullable Task task) {
        if (task == null) {
            return TaskDependencyItemResponse.builder()
                .id(null)
                .title(TASK_NOT_IN_SCOPE)
                .status(TaskStatus.TODO)
                .build();
        }
        return TaskDependencyItemResponse.builder()
            .id(task.getId())
            .title(task.getTitle())
            .status(task.getStatus())
            .build();
    }

    private void ensureNoCycle(Long fromTaskId, Long targetTaskId, Set<Long> visited) {
        if (!visited.add(fromTaskId)) {
            return;
        }
        if (fromTaskId.equals(targetTaskId)) {
            throw new BadRequestException(CYCLE_MSG);
        }
        for (TaskDependency dependency : taskDependencyRepository.findAllByTaskId(fromTaskId)) {
            ensureNoCycle(dependency.getDependsOnTaskId(), targetTaskId, visited);
        }
    }

    private void checkDependencyCompleted(Long taskId, Set<Long> visited) {
        if (!visited.add(taskId)) {
            return;
        }
        Task dependencyTask = taskRepository.findById(taskId)
            .orElseThrow(TaskNotFoundException::new);
        if (dependencyTask.getStatus() != TaskStatus.DONE) {
            throw new BadRequestException(PENDING_PREDEPS_MSG);
        }
        taskDependencyRepository.findAllByTaskId(taskId).forEach(item ->
            checkDependencyCompleted(item.getDependsOnTaskId(), visited)
        );
    }

}
