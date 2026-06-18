package com.taskmanagement.controller;

import com.taskmanagement.common.dto.ApiResponse;
import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.dto.request.TaskCreateRequest;
import com.taskmanagement.dto.request.TaskDependencyRequest;
import com.taskmanagement.dto.request.TaskUpdateRequest;
import com.taskmanagement.dto.response.TaskDependencyGraphResponse;
import com.taskmanagement.dto.response.TaskDependencyListResponse;
import com.taskmanagement.dto.response.TaskOptionResponse;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.entity.enums.TaskPriority;
import com.taskmanagement.entity.enums.TaskStatus;
import com.taskmanagement.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/user/tasks")
@RequiredArgsConstructor
public class UserTaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<TaskResponse>>> queryCurrentUserTasks(
        @RequestParam(defaultValue = "all") String scope,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) TaskPriority priority,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueStart,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueEnd,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") @Positive(message = "页码必须为正数") int page,
        @RequestParam(defaultValue = "10") @Positive(message = "每页大小必须为正数") int pageSize
    ) {
        PageResult<TaskResponse> result = taskService.queryCurrentUserTasks(
            scope, status, priority, dueStart, dueEnd, keyword, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("查询成功", result));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long id
    ) {
        TaskResponse response = taskService.getTaskById(id);
        return ResponseEntity.ok(ApiResponse.success("获取成功", response));
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long id,
        @Valid @RequestBody TaskUpdateRequest request
    ) {
        TaskResponse response = taskService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", response));
    }

    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long id
    ) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功"));
    }

    @GetMapping("/{id:[0-9]+}/dependencies")
    public ResponseEntity<ApiResponse<TaskDependencyListResponse>> getTaskDependencies(
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success("获取成功", taskService.getTaskDependencies(id)));
    }

    @PostMapping("/{id:[0-9]+}/dependencies")
    public ResponseEntity<ApiResponse<Void>> addTaskDependency(
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long id,
        @Valid @RequestBody TaskDependencyRequest request
    ) {
        taskService.addTaskDependency(id, request.getDependsOnTaskId());
        return ResponseEntity.ok(ApiResponse.success("依赖关系已添加"));
    }

    @DeleteMapping("/{id:[0-9]+}/dependencies/{dependsOnTaskId:[0-9]+}")
    public ResponseEntity<ApiResponse<Void>> deleteTaskDependency(
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long id,
        @PathVariable @Positive(message = "前置依赖任务 ID 必须为正数") Long dependsOnTaskId
    ) {
        taskService.deleteTaskDependency(id, dependsOnTaskId);
        return ResponseEntity.ok(ApiResponse.success("依赖关系已删除"));
    }

    @GetMapping("/dependency-graph")
    public ResponseEntity<ApiResponse<TaskDependencyGraphResponse>> getDependencyGraph() {
        return ResponseEntity.ok(ApiResponse.success("获取成功", taskService.getDependencyGraph()));
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<TaskOptionResponse>>> getPersonalTaskOptions(
        @RequestParam(required = false) String keyword
    ) {
        List<TaskOptionResponse> result = taskService.getPersonalTaskOptions(keyword);
        return ResponseEntity.ok(ApiResponse.success("获取成功", result));
    }
}
