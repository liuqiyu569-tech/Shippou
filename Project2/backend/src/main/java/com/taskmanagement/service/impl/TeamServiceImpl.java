package com.taskmanagement.service.impl;

import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.common.exception.BadRequestException;
import com.taskmanagement.common.exception.ForbiddenException;
import com.taskmanagement.common.exception.MemberAlreadyExistsException;
import com.taskmanagement.common.exception.TeamNotFoundException;
import com.taskmanagement.common.exception.UserNotFoundException;
import com.taskmanagement.dto.request.TeamCreateRequest;
import com.taskmanagement.dto.response.TeamDetailResponse;
import com.taskmanagement.dto.response.TeamDetailResponse.MemberInfo;
import com.taskmanagement.dto.response.TeamListResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.Team;
import com.taskmanagement.entity.TeamMember;
import com.taskmanagement.entity.User;
import com.taskmanagement.entity.enums.TaskLogObjectType;
import com.taskmanagement.entity.enums.TaskLogOperationType;
import com.taskmanagement.entity.enums.TeamRole;
import com.taskmanagement.repository.TaskAssigneeRepository;
import com.taskmanagement.repository.TaskDependencyRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.TeamMemberRepository;
import com.taskmanagement.repository.TeamRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CurrentUserProvider;
import com.taskmanagement.service.TaskOperationLogService;
import com.taskmanagement.service.TeamService;
import java.util.ArrayList;
import org.springframework.lang.NonNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TaskOperationLogService taskOperationLogService;

    @Override
    @Transactional
    public void createTeam(@NonNull TeamCreateRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();

        Team team = new Team();
        team.setName(request.getName());
        team.setCreatedBy(currentUserId);
        team = teamRepository.save(team);

        TeamMember owner = new TeamMember();
        owner.setTeamId(team.getId());
        owner.setUserId(currentUserId);
        owner.setRole(TeamRole.OWNER);
        teamMemberRepository.save(owner);

        Set<Long> memberIds = request.getMemberIds() != null
            ? request.getMemberIds().stream()
                .filter(id -> !id.equals(currentUserId))
                .collect(Collectors.toSet())
            : Collections.emptySet();

        for (Long memberId : memberIds) {
            if (!userRepository.existsById(memberId)) {
                continue;
            }
            TeamMember member = new TeamMember();
            member.setTeamId(team.getId());
            member.setUserId(memberId);
            member.setRole(TeamRole.MEMBER);
            teamMemberRepository.save(member);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TeamListResponse> getMyTeams(int page, int pageSize) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize);
        Page<Long> teamIdPage = teamMemberRepository.findTeamIdsByUserId(currentUserId, pageRequest);

        if (teamIdPage.isEmpty()) {
            return new PageResult<>(teamIdPage.getTotalElements(), page, pageSize, Collections.emptyList());
        }

        List<Team> teams = teamRepository.findAllById(teamIdPage.getContent());
        Map<Long, Team> teamMap = teams.stream().collect(Collectors.toMap(Team::getId, t -> t));

        List<TeamListResponse> items = new ArrayList<>();
        for (Long teamId : teamIdPage.getContent()) {
            Team team = teamMap.get(teamId);
            if (team == null) {
                continue;
            }
            TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId).orElse(null);
            String role = member != null ? member.getRole().name() : TeamRole.MEMBER.name();
            long memberCount = teamMemberRepository.countByTeamId(teamId);
            items.add(TeamListResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .role(role)
                .memberCount(memberCount)
                .build());
        }

        return new PageResult<>(teamIdPage.getTotalElements(), page, pageSize, items);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamDetailResponse getTeamDetail(@NonNull Long teamId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Team team = teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);

        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("非团队成员无法查看团队详情"));

        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId);
        List<Long> userIds = members.stream().map(TeamMember::getUserId).toList();
        Map<Long, String> usernameMap = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, User::getUsername));

        List<MemberInfo> memberInfos = members.stream()
            .map(m -> MemberInfo.builder()
                .userId(m.getUserId())
                .username(usernameMap.getOrDefault(m.getUserId(), "unknown"))
                .role(m.getRole().name())
                .joinedAt(m.getJoinedAt())
                .build())
            .toList();

        return TeamDetailResponse.builder()
            .id(team.getId())
            .name(team.getName())
            .myRole(currentMember.getRole().name())
            .members(memberInfos)
            .createdAt(team.getCreatedAt())
            .build();
    }

    @Override
    @Transactional
    public void dissolveTeam(@NonNull Long teamId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);

        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("非团队成员无法操作"));

        if (currentMember.getRole() != TeamRole.OWNER) {
            throw new ForbiddenException("仅 Owner 可解散团队");
        }

        List<Task> teamTasks = taskRepository.findAllByTeamId(teamId);
        if (!teamTasks.isEmpty()) {
            List<Long> taskIds = teamTasks.stream().map(Task::getId).toList();
            taskDependencyRepository.deleteAllInvolvingTaskIds(taskIds);
            taskAssigneeRepository.deleteByTaskIdIn(taskIds);
            taskRepository.deleteAll(teamTasks);
        }

        teamMemberRepository.deleteByTeamId(teamId);
        teamRepository.deleteById(teamId);
    }

    @Override
    @Transactional
    public void addMember(@NonNull Long teamId, @NonNull Long userId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("非团队成员无法操作"));

        if (currentMember.getRole() != TeamRole.OWNER) {
            throw new ForbiddenException("仅 Owner 可添加成员");
        }

        teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }

        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new MemberAlreadyExistsException();
        }

        TeamMember newMember = new TeamMember();
        newMember.setTeamId(teamId);
        newMember.setUserId(userId);
        newMember.setRole(TeamRole.MEMBER);
        teamMemberRepository.save(newMember);
    }

    @Override
    @Transactional
    public void updateMemberRole(@NonNull Long teamId, @NonNull Long userId, @NonNull TeamRole newRole) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("非团队成员无法操作"));

        if (currentMember.getRole() != TeamRole.OWNER) {
            throw new ForbiddenException("仅 Owner 可修改成员角色");
        }

        TeamMember targetMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .orElseThrow(() -> new UserNotFoundException());

        if (targetMember.getRole() == TeamRole.OWNER) {
            throw new BadRequestException("不可修改 Owner 的角色");
        }

        if (newRole == TeamRole.OWNER) {
            throw new BadRequestException("不可通过此接口将成员设为 Owner");
        }

        targetMember.setRole(newRole);
        teamMemberRepository.save(targetMember);
    }

    @Override
    @Transactional
    public void removeMember(@NonNull Long teamId, @NonNull Long userId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("非团队成员无法操作"));

        if (currentMember.getRole() != TeamRole.OWNER) {
            throw new ForbiddenException("仅 Owner 可移除成员");
        }

        if (currentUserId.equals(userId)) {
            throw new BadRequestException("Owner 不可移除自身，请使用解散团队功能");
        }

        TeamMember targetMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .orElseThrow(() -> new UserNotFoundException());

        String removedUsername = userRepository.findById(userId)
            .map(User::getUsername)
            .orElse("user#" + userId);
        List<Long> assignedTaskIds = taskAssigneeRepository.findTaskIdsByUserId(userId);
        List<Task> teamTasks = taskRepository.findAllByTeamId(teamId);
        for (Task task : teamTasks) {
            taskAssigneeRepository.deleteByTaskIdAndUserId(task.getId(), userId);
        }
        for (Task task : teamTasks) {
            if (!assignedTaskIds.contains(task.getId())) {
                continue;
            }
            taskOperationLogService.record(
                task,
                TaskLogOperationType.DELETE,
                TaskLogObjectType.TASK_ASSIGNEE,
                "Removed assignee [" + removedUsername + "] because the member was removed from the team"
            );
        }

        teamMemberRepository.delete(targetMember);
    }

    @Override
    @Transactional
    public void leaveTeam(@NonNull Long teamId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);

        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("当前用户不是该团队成员"));

        if (currentMember.getRole() == TeamRole.OWNER) {
            long memberCount = teamMemberRepository.countByTeamId(teamId);
            if (memberCount > 1) {
                throw new BadRequestException("当前用户是 Owner，不能直接离开，请先转让所有权");
            }
        }

        String leavingUsername = userRepository.findById(currentUserId)
            .map(User::getUsername)
            .orElse("user#" + currentUserId);
        List<Long> assignedTaskIds = taskAssigneeRepository.findTaskIdsByUserId(currentUserId);
        List<Task> teamTasks = taskRepository.findAllByTeamId(teamId);
        for (Task task : teamTasks) {
            taskAssigneeRepository.deleteByTaskIdAndUserId(task.getId(), currentUserId);
        }
        for (Task task : teamTasks) {
            if (!assignedTaskIds.contains(task.getId())) {
                continue;
            }
            taskOperationLogService.record(
                task,
                TaskLogOperationType.DELETE,
                TaskLogObjectType.TASK_ASSIGNEE,
                "Removed assignee [" + leavingUsername + "] because the member left the team"
            );
        }

        teamMemberRepository.delete(currentMember);
    }

    @Override
    @Transactional
    public void transferOwnership(@NonNull Long teamId, @NonNull Long newOwnerId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);

        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("当前用户不是该团队的 Owner"));

        if (currentMember.getRole() != TeamRole.OWNER) {
            throw new ForbiddenException("当前用户不是该团队的 Owner");
        }

        if (currentUserId.equals(newOwnerId)) {
            throw new BadRequestException("不可将所有权转让给自己");
        }

        TeamMember newOwner = teamMemberRepository.findByTeamIdAndUserId(teamId, newOwnerId)
            .orElseThrow(() -> new BadRequestException("新 Owner 不是该团队成员"));

        currentMember.setRole(TeamRole.MEMBER);
        newOwner.setRole(TeamRole.OWNER);
        teamMemberRepository.save(currentMember);
        teamMemberRepository.save(newOwner);
    }

}
