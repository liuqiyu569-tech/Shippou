package com.taskmanagement.controller;

import com.taskmanagement.common.dto.ApiResponse;
import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.dto.request.AddMemberRequest;
import com.taskmanagement.dto.request.TeamCreateRequest;
import com.taskmanagement.dto.request.TeamMemberRoleRequest;
import com.taskmanagement.dto.request.TransferOwnershipRequest;
import com.taskmanagement.dto.response.TeamDetailResponse;
import com.taskmanagement.dto.response.TeamListResponse;
import com.taskmanagement.dto.response.TeamTaskStatsResponse;
import com.taskmanagement.service.TeamService;
import com.taskmanagement.service.TeamTaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 团队管理控制器，提供团队 CRUD 与成员管理接口。
 *
 * <p>所有接口均需用户认证，权限校验在 Service 层实现。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamTaskService teamTaskService;

    /**
     * 创建团队，创建者自动成为 Owner。
     *
     * @param request 创建团队请求
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createTeam(@Valid @RequestBody TeamCreateRequest request) {
        teamService.createTeam(request);
        return ResponseEntity.ok(ApiResponse.success("创建成功"));
    }

    /**
     * 获取当前用户的团队列表（分页）。
     *
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 团队列表分页
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<TeamListResponse>>> getMyTeams(
        @RequestParam(defaultValue = "1") @Positive(message = "页码必须为正数") int page,
        @RequestParam(defaultValue = "10") @Positive(message = "每页大小必须为正数") int pageSize
    ) {
        PageResult<TeamListResponse> result = teamService.getMyTeams(page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("获取成功", result));
    }

    /**
     * 获取团队详情（含成员列表），仅团队成员可访问。
     *
     * @param teamId 团队 ID
     * @return 团队详情
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getTeamDetail(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId
    ) {
        TeamDetailResponse result = teamService.getTeamDetail(teamId);
        return ResponseEntity.ok(ApiResponse.success("获取成功", result));
    }

    @GetMapping("/{teamId}/task-stats")
    public ResponseEntity<ApiResponse<TeamTaskStatsResponse>> getTeamTaskStats(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @RequestParam(defaultValue = "3") @Positive(message = "即将到期天数必须为正数") int upcomingDays
    ) {
        TeamTaskStatsResponse result = teamTaskService.getTeamTaskStats(teamId, upcomingDays);
        return ResponseEntity.ok(ApiResponse.success("查询成功", result));
    }

    /**
     * 解散团队，仅 Owner 可操作。级联删除成员、任务和分配记录。
     *
     * @param teamId 团队 ID
     * @return 解散结果
     */
    @DeleteMapping("/{teamId}")
    public ResponseEntity<ApiResponse<Void>> dissolveTeam(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId
    ) {
        teamService.dissolveTeam(teamId);
        return ResponseEntity.ok(ApiResponse.success("解散成功"));
    }

    /**
     * 添加成员到团队，仅 Owner 可操作。
     *
     * @param teamId 团队 ID
     * @param request 添加成员请求
     * @return 添加结果
     */
    @PostMapping("/{teamId}/members")
    public ResponseEntity<ApiResponse<Void>> addMember(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @Valid @RequestBody AddMemberRequest request
    ) {
        teamService.addMember(teamId, request.getUserId());
        return ResponseEntity.ok(ApiResponse.success("添加成功"));
    }

    /**
     * 修改成员角色（MEMBER ↔ ADMIN），仅 Owner 可操作。
     *
     * @param teamId 团队 ID
     * @param userId 用户 ID
     * @param request 角色修改请求
     * @return 修改结果
     */
    @PutMapping("/{teamId}/members/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> updateMemberRole(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @PathVariable @Positive(message = "用户 ID 必须为正数") Long userId,
        @Valid @RequestBody TeamMemberRoleRequest request
    ) {
        teamService.updateMemberRole(teamId, userId, request.getRole());
        return ResponseEntity.ok(ApiResponse.success("修改成功"));
    }

    /**
     * 移除成员（仅 Owner），同时取消该成员在此团队中的任务分配。
     *
     * @param teamId 团队 ID
     * @param userId 用户 ID
     * @return 移除结果
     */
    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @PathVariable @Positive(message = "用户 ID 必须为正数") Long userId
    ) {
        teamService.removeMember(teamId, userId);
        return ResponseEntity.ok(ApiResponse.success("删除成功"));
    }

    /**
     * 当前用户主动离开团队。Owner 必须先转让所有权，独自一人时除外。
     *
     * @param teamId 团队 ID
     * @return 离开结果
     */
    @DeleteMapping("/{teamId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveTeam(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId
    ) {
        teamService.leaveTeam(teamId);
        return ResponseEntity.ok(ApiResponse.success("已成功离开团队"));
    }

    /**
     * Owner 转让团队所有权给团队内另一名成员。
     *
     * @param teamId 团队 ID
     * @param request 转让请求
     * @return 转让结果
     */
    @PostMapping("/{teamId}/transfer")
    public ResponseEntity<ApiResponse<Void>> transferOwnership(
        @PathVariable @Positive(message = "团队 ID 必须为正数") Long teamId,
        @Valid @RequestBody TransferOwnershipRequest request
    ) {
        teamService.transferOwnership(teamId, request.getNewOwnerId());
        return ResponseEntity.ok(ApiResponse.success("转让成功"));
    }
}
