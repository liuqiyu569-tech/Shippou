package com.taskmanagement.common.utils;

import com.taskmanagement.dto.request.TaskCreateRequest;
import com.taskmanagement.dto.request.TaskUpdateRequest;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.entity.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toEntity(TaskCreateRequest request, Long userId) {
        Task task = new Task();
        task.setCreatorUserId(userId);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueAt(request.getDueAt());
        return task;
    }

    public void applyUpdate(Task task, TaskUpdateRequest request) {
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueAt(request.getDueAt());
    }

    public TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .status(task.getStatus())
            .priority(task.getPriority())
            .dueAt(task.getDueAt())
            .dueStatus(DueStatusCalculator.calculate(task))
            .teamId(task.getTeamId())
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .build();
    }
}
