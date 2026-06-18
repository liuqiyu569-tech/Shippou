package com.taskmanagement.controller;

import com.taskmanagement.common.dto.ApiResponse;
import com.taskmanagement.dto.request.TaskCreateRequest;
import com.taskmanagement.dto.request.TaskUpdateRequest;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务管理控制器，提供任务的增删改查接口。
 *
 * <p>所有接口均需要用户认证（通过 JWT Token），且用户只能操作自己的任务。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * 创建任务。
     *
     * @param request 创建任务请求
     * @return 创建后的任务
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", response));
    }

    /**
     * 获取当前用户任务列表。
     *
     * @return 当前用户任务集合
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getCurrentUserTasks() {
        List<TaskResponse> response = taskService.getCurrentUserTasks();
        return ResponseEntity.ok(ApiResponse.success("获取成功", response));
    }

    /**
     * 获取任务详情。
     *
     * @param id 任务 id
     * @return 任务详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(@PathVariable @Positive(message = "任务 id 必须为正数") Long id) {
        TaskResponse response = taskService.getTaskById(id);
        return ResponseEntity.ok(ApiResponse.success("获取成功", response));
    }

    /**
     * 更新任务。
     *
     * @param id 任务 id
     * @param request 更新任务请求
     * @return 更新后的任务
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
        @PathVariable @Positive(message = "任务 id 必须为正数") Long id,
        @Valid @RequestBody TaskUpdateRequest request
    ) {
        TaskResponse response = taskService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", response));
    }

    /**
     * 删除任务。
     *
     * @param id 任务 id
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable @Positive(message = "任务 id 必须为正数") Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功"));
    }
}
