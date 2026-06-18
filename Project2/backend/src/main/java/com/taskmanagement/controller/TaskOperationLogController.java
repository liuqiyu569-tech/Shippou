package com.taskmanagement.controller;

import com.taskmanagement.common.dto.ApiResponse;
import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.dto.response.TaskOperationLogResponse;
import com.taskmanagement.entity.enums.TaskLogObjectType;
import com.taskmanagement.entity.enums.TaskLogOperationType;
import com.taskmanagement.service.TaskService;
import com.taskmanagement.service.TeamTaskService;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class TaskOperationLogController {

    private final TaskService taskService;
    private final TeamTaskService teamTaskService;

    @GetMapping("/api/v1/user/tasks/{taskId:[0-9]+}/logs")
    public ResponseEntity<ApiResponse<PageResult<TaskOperationLogResponse>>> queryPersonalTaskLogs(
        @PathVariable @Positive Long taskId,
        @RequestParam(required = false) TaskLogOperationType operationType,
        @RequestParam(required = false) TaskLogObjectType objectType,
        @RequestParam(defaultValue = "1") @Positive int page,
        @RequestParam(defaultValue = "10") @Positive int pageSize
    ) {
        PageResult<TaskOperationLogResponse> result = taskService.queryTaskLogs(
            taskId, operationType, objectType, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("查询成功", result));
    }

    @GetMapping("/api/v1/user/task-logs")
    public ResponseEntity<ApiResponse<PageResult<TaskOperationLogResponse>>> queryCurrentUserTaskLogs(
        @RequestParam(required = false) TaskLogOperationType operationType,
        @RequestParam(required = false) TaskLogObjectType objectType,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
        @RequestParam(defaultValue = "1") @Positive int page,
        @RequestParam(defaultValue = "10") @Positive int pageSize
    ) {
        PageResult<TaskOperationLogResponse> result = taskService.queryCurrentUserTaskLogs(
            operationType, objectType, keyword, startTime, endTime, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("查询成功", result));
    }

    @GetMapping("/api/v1/teams/{teamId:[0-9]+}/tasks/{taskId:[0-9]+}/logs")
    public ResponseEntity<ApiResponse<PageResult<TaskOperationLogResponse>>> queryTeamTaskLogs(
        @PathVariable @Positive Long teamId,
        @PathVariable @Positive Long taskId,
        @RequestParam(required = false) TaskLogOperationType operationType,
        @RequestParam(required = false) TaskLogObjectType objectType,
        @RequestParam(defaultValue = "1") @Positive int page,
        @RequestParam(defaultValue = "10") @Positive int pageSize
    ) {
        PageResult<TaskOperationLogResponse> result = teamTaskService.queryTeamTaskLogs(
            teamId, taskId, operationType, objectType, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("查询成功", result));
    }

    @GetMapping("/api/v1/teams/{teamId:[0-9]+}/task-logs")
    public ResponseEntity<ApiResponse<PageResult<TaskOperationLogResponse>>> queryTeamLogs(
        @PathVariable @Positive Long teamId,
        @RequestParam(required = false) TaskLogOperationType operationType,
        @RequestParam(required = false) TaskLogObjectType objectType,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
        @RequestParam(defaultValue = "1") @Positive int page,
        @RequestParam(defaultValue = "10") @Positive int pageSize
    ) {
        PageResult<TaskOperationLogResponse> result = teamTaskService.queryTeamLogs(
            teamId, operationType, objectType, keyword, startTime, endTime, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("查询成功", result));
    }
}
