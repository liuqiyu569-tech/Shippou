package com.taskmanagement.service;

import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.dto.request.TeamCreateRequest;
import com.taskmanagement.dto.response.TeamDetailResponse;
import com.taskmanagement.dto.response.TeamListResponse;
import com.taskmanagement.entity.enums.TeamRole;
import org.springframework.lang.NonNull;

/**
 * 团队管理服务接口，定义团队 CRUD 与成员管理操作。
 */
public interface TeamService {

    /**
     * 创建团队，创建者自动成为 Owner。
     *
     * @param request 创建团队请求
     */
    void createTeam(@NonNull TeamCreateRequest request);

    /**
     * 获取当前用户加入的团队列表（分页），按加入时间降序。
     *
     * @param page 页码
     * @param pageSize 每页大小
     * @return 团队列表分页结果
     */
    PageResult<TeamListResponse> getMyTeams(int page, int pageSize);

    /**
     * 获取团队详情（含成员列表），仅团队成员可访问。
     *
     * @param teamId 团队 ID
     * @return 团队详情
     */
    TeamDetailResponse getTeamDetail(@NonNull Long teamId);

    /**
     * 解散团队（仅 Owner），级联删除成员、任务和分配记录。
     *
     * @param teamId 团队 ID
     */
    void dissolveTeam(@NonNull Long teamId);

    /**
     * 添加成员到团队（仅 Owner），默认角色为 MEMBER。
     *
     * @param teamId 团队 ID
     * @param userId 被添加的用户 ID
     */
    void addMember(@NonNull Long teamId, @NonNull Long userId);

    /**
     * 修改成员角色（MEMBER ↔ ADMIN），仅 Owner 可操作。
     *
     * @param teamId 团队 ID
     * @param userId 用户 ID
     * @param role 新角色（只能为 ADMIN 或 MEMBER）
     */
    void updateMemberRole(@NonNull Long teamId, @NonNull Long userId, @NonNull TeamRole role);

    /**
     * 移除成员（仅 Owner），同时取消该成员在此团队中的所有任务分配。
     *
     * @param teamId 团队 ID
     * @param userId 用户 ID
     */
    void removeMember(@NonNull Long teamId, @NonNull Long userId);

    /**
     * 当前用户主动离开团队。Owner 仅当独自一人时允许离开（前端应改调解散接口），
     * 否则必须先转让所有权。离开后同步取消该用户在此团队所有任务上的分配。
     *
     * @param teamId 团队 ID
     */
    void leaveTeam(@NonNull Long teamId);

    /**
     * Owner 将团队所有权转让给团队内另一名成员。转让后，原 Owner 角色变为 MEMBER，
     * 被转让者角色变为 OWNER。仅当前 Owner 可操作。
     *
     * @param teamId 团队 ID
     * @param newOwnerId 接受所有权的用户 ID
     */
    void transferOwnership(@NonNull Long teamId, @NonNull Long newOwnerId);
}
