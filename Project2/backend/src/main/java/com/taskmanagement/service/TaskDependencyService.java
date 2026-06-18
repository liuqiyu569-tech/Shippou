package com.taskmanagement.service;

import com.taskmanagement.dto.response.TaskDependencyGraphResponse;
import com.taskmanagement.dto.response.TaskDependencyListResponse;

public interface TaskDependencyService {

    TaskDependencyListResponse getPersonalTaskDependencies(Long currentUserId, Long taskId);

    void addPersonalTaskDependency(Long currentUserId, Long taskId, Long dependsOnTaskId);

    void deletePersonalTaskDependency(Long currentUserId, Long taskId, Long dependsOnTaskId);

    TaskDependencyGraphResponse getPersonalDependencyGraph(Long currentUserId);

    TaskDependencyListResponse getTeamTaskDependencies(Long currentUserId, Long teamId, Long taskId);

    void addTeamTaskDependency(Long currentUserId, Long teamId, Long taskId, Long dependsOnTaskId);

    void deleteTeamTaskDependency(Long currentUserId, Long teamId, Long taskId, Long dependsOnTaskId);

    TaskDependencyGraphResponse getTeamDependencyGraph(Long currentUserId, Long teamId);

    void ensureTaskReadyForDone(Long taskId);

    void ensureTaskReadyForDoneWithAdditionalDependency(Long taskId, Long dependsOnTaskId);

    void clearTaskDependencyLinks(Long taskId);
}
