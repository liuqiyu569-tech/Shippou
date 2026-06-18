package com.taskmanagement.controller;

import com.taskmanagement.common.dto.ApiResponse;
import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.dto.request.TaskAssignmentRequest;
import com.taskmanagement.dto.request.TaskDependencyRequest;
import com.taskmanagement.dto.request.TeamTaskCreateRequest;
import com.taskmanagement.dto.request.TeamTaskUpdateRequest;
import com.taskmanagement.dto.response.TaskDependencyGraphResponse;
import com.taskmanagement.dto.response.TaskDependencyListResponse;
import com.taskmanagement.dto.response.TaskOptionResponse;
import com.taskmanagement.dto.response.TeamTaskResponse;
import com.taskmanagement.entity.enums.TaskPriority;
import com.taskmanagement.entity.enums.TaskStatus;
import com.taskmanagement.service.TeamTaskService;
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
@RequestMapping("/api/v1/teams/{teamId}/tasks")
@RequiredArgsConstructor
public class TeamTaskController {

    private final TeamTaskService teamTaskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TeamTaskResponse>> createTeamTask(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @Valid @RequestBody TeamTaskCreateRequest request
    ) {
        TeamTaskResponse response = teamTaskService.createTeamTask(teamId, request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<TeamTaskResponse>>> getTeamTasks(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) TaskPriority priority,
        @RequestParam(required = false) @Positive(message = "指派用户 ID 必须为正数") Long assigneeId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueStart,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueEnd,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") @Positive(message = "页码必须为正数") int page,
        @RequestParam(defaultValue = "10") @Positive(message = "每页大小必须为正数") int pageSize
    ) {
        PageResult<TeamTaskResponse> result = teamTaskService.getTeamTasks(
            teamId, status, priority, assigneeId, dueStart, dueEnd, keyword, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("查询成功", result));
    }

    @GetMapping("/{taskId:[0-9]+}")
    public ResponseEntity<ApiResponse<TeamTaskResponse>> getTeamTask(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long taskId
    ) {
        TeamTaskResponse response = teamTaskService.getTeamTask(teamId, taskId);
        return ResponseEntity.ok(ApiResponse.success("获取成功", response));
    }

    @PutMapping("/{taskId:[0-9]+}")
    public ResponseEntity<ApiResponse<TeamTaskResponse>> updateTeamTask(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long taskId,
        @Valid @RequestBody TeamTaskUpdateRequest request
    ) {
        TeamTaskResponse response = teamTaskService.updateTeamTask(teamId, taskId, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", response));
    }

    @DeleteMapping("/{taskId:[0-9]+}")
    public ResponseEntity<ApiResponse<Void>> deleteTeamTask(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long taskId
    ) {
        teamTaskService.deleteTeamTask(teamId, taskId);
        return ResponseEntity.ok(ApiResponse.success("删除成功"));
    }

    @GetMapping("/{taskId:[0-9]+}/dependencies")
    public ResponseEntity<ApiResponse<TaskDependencyListResponse>> getTeamTaskDependencies(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long taskId
    ) {
        return ResponseEntity.ok(ApiResponse.success("获取成功", teamTaskService.getTeamTaskDependencies(teamId, taskId)));
    }

    @PostMapping("/{taskId:[0-9]+}/dependencies")
    public ResponseEntity<ApiResponse<Void>> addTeamTaskDependency(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long taskId,
        @Valid @RequestBody TaskDependencyRequest request
    ) {
        teamTaskService.addTeamTaskDependency(teamId, taskId, request.getDependsOnTaskId());
        return ResponseEntity.ok(ApiResponse.success("依赖关系已添加"));
    }

    @DeleteMapping("/{taskId:[0-9]+}/dependencies/{dependsOnTaskId:[0-9]+}")
    public ResponseEntity<ApiResponse<Void>> deleteTeamTaskDependency(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long taskId,
        @PathVariable @Positive(message = "前置依赖任务 ID 必须为正数") Long dependsOnTaskId
    ) {
        teamTaskService.deleteTeamTaskDependency(teamId, taskId, dependsOnTaskId);
        return ResponseEntity.ok(ApiResponse.success("依赖关系已删除"));
    }

    @GetMapping("/dependency-graph")
    public ResponseEntity<ApiResponse<TaskDependencyGraphResponse>> getTeamDependencyGraph(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId
    ) {
        return ResponseEntity.ok(ApiResponse.success("获取成功", teamTaskService.getTeamDependencyGraph(teamId)));
    }

    @PutMapping("/{taskId:[0-9]+}/assignees")
    public ResponseEntity<ApiResponse<Void>> assignTask(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long taskId,
        @Valid @RequestBody TaskAssignmentRequest request
    ) {
        teamTaskService.assignTask(teamId, taskId, request.getUserIds());
        return ResponseEntity.ok(ApiResponse.success("分配成功"));
    }

    @DeleteMapping("/{taskId:[0-9]+}/assignees/{userId:[0-9]+}")
    public ResponseEntity<ApiResponse<Void>> unassignTask(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @PathVariable @Positive(message = "任务 ID 必须为正数") Long taskId,
        @PathVariable @Positive(message = "用户 ID 必须为正数") Long userId
    ) {
        teamTaskService.unassignTask(teamId, taskId, userId);
        return ResponseEntity.ok(ApiResponse.success("取消分配成功"));
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<TaskOptionResponse>>> getTeamTaskOptions(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @RequestParam(required = false) String keyword
    ) {
        List<TaskOptionResponse> result = teamTaskService.getTeamTaskOptions(teamId, keyword);
        return ResponseEntity.ok(ApiResponse.success("获取成功", result));
    }
}
