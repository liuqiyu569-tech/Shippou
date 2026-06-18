package com.taskmanagement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmanagement.common.exception.BadRequestException;
import com.taskmanagement.common.exception.ForbiddenException;
import com.taskmanagement.common.exception.MemberAlreadyExistsException;
import com.taskmanagement.common.exception.UserNotFoundException;
import com.taskmanagement.dto.request.TeamCreateRequest;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.Team;
import com.taskmanagement.entity.TeamMember;
import com.taskmanagement.entity.enums.TeamRole;
import com.taskmanagement.repository.TaskAssigneeRepository;
import com.taskmanagement.repository.TaskDependencyRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.TeamMemberRepository;
import com.taskmanagement.repository.TeamRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CurrentUserProvider;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamServiceImpl 单元测试")
class TeamServiceImplTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long TEAM_ID = 10L;

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskAssigneeRepository taskAssigneeRepository;
    @Mock
    private TaskDependencyRepository taskDependencyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private TeamServiceImpl teamService;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CURRENT_USER_ID);
    }

    @Nested
    @DisplayName("createTeam")
    class CreateTeamTests {

        @Test
        @DisplayName("正常创建团队，创建者成为 Owner")
        void shouldCreateTeamAndSetCreatorAsOwner() {
            TeamCreateRequest request = new TeamCreateRequest();
            request.setName("测试团队");
            request.setMemberIds(Collections.emptyList());

            Team savedTeam = new Team();
            savedTeam.setId(TEAM_ID);
            savedTeam.setName("测试团队");
            when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);

            teamService.createTeam(request);

            verify(teamRepository).save(any(Team.class));
            verify(teamMemberRepository).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("创建团队时附带成员，排除当前用户并去重")
        void shouldAddMembersExcludingCurrentUser() {
            TeamCreateRequest request = new TeamCreateRequest();
            request.setName("测试团队");
            request.setMemberIds(List.of(CURRENT_USER_ID, OTHER_USER_ID, OTHER_USER_ID));

            Team savedTeam = new Team();
            savedTeam.setId(TEAM_ID);
            when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);
            when(userRepository.existsById(OTHER_USER_ID)).thenReturn(true);

            teamService.createTeam(request);

            // Owner 创建 + 成员加入，共2次 save 调用（1 次 Owner + 1 次 Member）
            verify(teamMemberRepository, org.mockito.Mockito.times(2)).save(any(TeamMember.class));
        }
    }

    @Nested
    @DisplayName("addMember")
    class AddMemberTests {

        @Test
        @DisplayName("Owner 可以添加成员")
        void shouldAllowOwnerToAddMember() {
            TeamMember ownerMember = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(ownerMember));
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(userRepository.existsById(OTHER_USER_ID)).thenReturn(true);
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID)).thenReturn(false);

            teamService.addMember(TEAM_ID, OTHER_USER_ID);

            verify(teamMemberRepository).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("非团队成员无法添加成员")
        void shouldDenyNonMember() {
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.addMember(TEAM_ID, OTHER_USER_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("非团队成员");

            verify(teamMemberRepository, never()).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("非 Owner 不能添加成员")
        void shouldDenyNonOwner() {
            TeamMember member = createMember(CURRENT_USER_ID, TeamRole.MEMBER);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(member));

            assertThatThrownBy(() -> teamService.addMember(TEAM_ID, OTHER_USER_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("仅 Owner");

            verify(teamMemberRepository, never()).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("添加不存在的用户应抛出 UserNotFoundException")
        void shouldThrowWhenUserNotFound() {
            TeamMember ownerMember = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(ownerMember));
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(userRepository.existsById(OTHER_USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> teamService.addMember(TEAM_ID, OTHER_USER_ID))
                .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("重复添加成员应抛出 MemberAlreadyExistsException")
        void shouldThrowWhenMemberAlreadyExists() {
            TeamMember ownerMember = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(ownerMember));
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(userRepository.existsById(OTHER_USER_ID)).thenReturn(true);
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> teamService.addMember(TEAM_ID, OTHER_USER_ID))
                .isInstanceOf(MemberAlreadyExistsException.class);
        }
    }

    @Nested
    @DisplayName("dissolveTeam")
    class DissolveTeamTests {

        @Test
        @DisplayName("Owner 可以解散团队并级联删除")
        void shouldAllowOwnerToDissolve() {
            TeamMember ownerMember = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(ownerMember));
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(taskRepository.findAllByTeamId(TEAM_ID)).thenReturn(Collections.emptyList());

            teamService.dissolveTeam(TEAM_ID);

            verify(teamMemberRepository).deleteByTeamId(TEAM_ID);
            verify(teamRepository).deleteById(TEAM_ID);
        }

        @Test
        @DisplayName("Owner 解散团队时按顺序级联删除依赖、分配和任务")
        void shouldCascadeDeleteTasksAndAssignees() {
            TeamMember ownerMember = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(ownerMember));
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));

            Task task1 = new Task();
            task1.setId(101L);
            Task task2 = new Task();
            task2.setId(102L);
            when(taskRepository.findAllByTeamId(TEAM_ID)).thenReturn(List.of(task1, task2));

            teamService.dissolveTeam(TEAM_ID);

            List<Long> taskIds = List.of(101L, 102L);
            org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
                taskDependencyRepository, taskAssigneeRepository, taskRepository,
                teamMemberRepository, teamRepository);
            inOrder.verify(taskDependencyRepository).deleteAllInvolvingTaskIds(taskIds);
            inOrder.verify(taskAssigneeRepository).deleteByTaskIdIn(taskIds);
            inOrder.verify(taskRepository).deleteAll(List.of(task1, task2));
            inOrder.verify(teamMemberRepository).deleteByTeamId(TEAM_ID);
            inOrder.verify(teamRepository).deleteById(TEAM_ID);
        }

        @Test
        @DisplayName("非 Owner 不能解散团队")
        void shouldDenyNonOwner() {
            TeamMember member = createMember(CURRENT_USER_ID, TeamRole.MEMBER);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(member));

            assertThatThrownBy(() -> teamService.dissolveTeam(TEAM_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("仅 Owner");
        }
    }

    @Nested
    @DisplayName("updateMemberRole")
    class UpdateMemberRoleTests {

        @Test
        @DisplayName("Owner 可将 MEMBER 设为 ADMIN")
        void shouldPromoteMemberToAdmin() {
            TeamMember ownerMember = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            TeamMember targetMember = createMember(OTHER_USER_ID, TeamRole.MEMBER);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(ownerMember));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID))
                .thenReturn(Optional.of(targetMember));

            teamService.updateMemberRole(TEAM_ID, OTHER_USER_ID, TeamRole.ADMIN);

            assertThat(targetMember.getRole()).isEqualTo(TeamRole.ADMIN);
            verify(teamMemberRepository).save(targetMember);
        }

        @Test
        @DisplayName("不可修改 Owner 的角色")
        void shouldDenyModifyingOwnerRole() {
            TeamMember ownerMember = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            TeamMember targetOwner = createMember(OTHER_USER_ID, TeamRole.OWNER);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(ownerMember));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID))
                .thenReturn(Optional.of(targetOwner));

            assertThatThrownBy(() -> teamService.updateMemberRole(TEAM_ID, OTHER_USER_ID, TeamRole.ADMIN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Owner");
        }
    }

    @Nested
    @DisplayName("removeMember")
    class RemoveMemberTests {

        @Test
        @DisplayName("Owner 可移除成员")
        void shouldAllowOwnerToRemoveMember() {
            TeamMember ownerMember = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            TeamMember targetMember = createMember(OTHER_USER_ID, TeamRole.MEMBER);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(ownerMember));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID))
                .thenReturn(Optional.of(targetMember));
            when(taskRepository.findAllByTeamId(TEAM_ID)).thenReturn(Collections.emptyList());

            teamService.removeMember(TEAM_ID, OTHER_USER_ID);

            verify(teamMemberRepository).delete(targetMember);
        }

        @Test
        @DisplayName("Owner 不可移除自身")
        void shouldDenyRemovingSelf() {
            TeamMember ownerMember = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(ownerMember));

            assertThatThrownBy(() -> teamService.removeMember(TEAM_ID, CURRENT_USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("自身");
        }

        @Test
        @DisplayName("移除成员时取消其任务分配")
        void shouldCancelAssignmentsWhenRemovingMember() {
            TeamMember ownerMember = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            TeamMember targetMember = createMember(OTHER_USER_ID, TeamRole.MEMBER);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(ownerMember));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID))
                .thenReturn(Optional.of(targetMember));

            Task task = new Task();
            task.setId(201L);
            when(taskRepository.findAllByTeamId(TEAM_ID)).thenReturn(List.of(task));

            teamService.removeMember(TEAM_ID, OTHER_USER_ID);

            verify(taskAssigneeRepository).deleteByTaskIdAndUserId(201L, OTHER_USER_ID);
            verify(teamMemberRepository).delete(targetMember);
        }
    }

    private TeamMember createMember(Long userId, TeamRole role) {
        TeamMember member = new TeamMember();
        member.setId(userId);
        member.setTeamId(TEAM_ID);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }

    @Nested
    @DisplayName("leaveTeam")
    class LeaveTeamTests {

        @Test
        @DisplayName("非团队成员尝试离开抛 ForbiddenException")
        void shouldDenyNonMember() {
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.leaveTeam(TEAM_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("不是该团队成员");

            verify(teamMemberRepository, never()).delete(any(TeamMember.class));
        }

        @Test
        @DisplayName("Owner 团队还有其他成员时不能直接离开")
        void shouldDenyOwnerLeaveWhenOthersExist() {
            TeamMember ownerMember = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(ownerMember));
            when(teamMemberRepository.countByTeamId(TEAM_ID)).thenReturn(3L);

            assertThatThrownBy(() -> teamService.leaveTeam(TEAM_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("转让所有权");

            verify(teamMemberRepository, never()).delete(any(TeamMember.class));
        }

        @Test
        @DisplayName("Owner 独自一人时允许离开（前端应直接调解散）")
        void shouldAllowOwnerLeaveWhenAlone() {
            TeamMember ownerMember = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(ownerMember));
            when(teamMemberRepository.countByTeamId(TEAM_ID)).thenReturn(1L);
            when(taskRepository.findAllByTeamId(TEAM_ID)).thenReturn(Collections.emptyList());

            teamService.leaveTeam(TEAM_ID);

            verify(teamMemberRepository).delete(ownerMember);
        }

        @Test
        @DisplayName("Member 离开时取消其任务分配并删除成员记录")
        void shouldCancelAssignmentsWhenMemberLeaves() {
            TeamMember member = createMember(CURRENT_USER_ID, TeamRole.MEMBER);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(member));

            Task task = new Task();
            task.setId(201L);
            when(taskRepository.findAllByTeamId(TEAM_ID)).thenReturn(List.of(task));

            teamService.leaveTeam(TEAM_ID);

            verify(taskAssigneeRepository).deleteByTaskIdAndUserId(201L, CURRENT_USER_ID);
            verify(teamMemberRepository).delete(member);
        }

        @Test
        @DisplayName("团队不存在抛 TeamNotFoundException")
        void shouldThrowWhenTeamMissing() {
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.leaveTeam(TEAM_ID))
                .isInstanceOf(com.taskmanagement.common.exception.TeamNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("transferOwnership")
    class TransferOwnershipTests {

        @Test
        @DisplayName("Owner 可将所有权转让给团队内成员，原 Owner 降为 MEMBER")
        void shouldTransferOwnershipAndDemoteOldOwner() {
            TeamMember currentOwner = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            TeamMember newOwner = createMember(OTHER_USER_ID, TeamRole.MEMBER);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(currentOwner));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID))
                .thenReturn(Optional.of(newOwner));

            teamService.transferOwnership(TEAM_ID, OTHER_USER_ID);

            assertThat(currentOwner.getRole()).isEqualTo(TeamRole.MEMBER);
            assertThat(newOwner.getRole()).isEqualTo(TeamRole.OWNER);
            verify(teamMemberRepository).save(currentOwner);
            verify(teamMemberRepository).save(newOwner);
        }

        @Test
        @DisplayName("ADMIN 也可被转让为 Owner，原 Owner 降为 MEMBER")
        void shouldTransferOwnershipFromAdmin() {
            TeamMember currentOwner = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            TeamMember admin = createMember(OTHER_USER_ID, TeamRole.ADMIN);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(currentOwner));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID))
                .thenReturn(Optional.of(admin));

            teamService.transferOwnership(TEAM_ID, OTHER_USER_ID);

            assertThat(currentOwner.getRole()).isEqualTo(TeamRole.MEMBER);
            assertThat(admin.getRole()).isEqualTo(TeamRole.OWNER);
        }

        @Test
        @DisplayName("非 Owner 不能转让所有权")
        void shouldDenyNonOwner() {
            TeamMember currentMember = createMember(CURRENT_USER_ID, TeamRole.ADMIN);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(currentMember));

            assertThatThrownBy(() -> teamService.transferOwnership(TEAM_ID, OTHER_USER_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Owner");

            verify(teamMemberRepository, never()).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("非团队成员不能转让所有权")
        void shouldDenyNonMember() {
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.transferOwnership(TEAM_ID, OTHER_USER_ID))
                .isInstanceOf(ForbiddenException.class);

            verify(teamMemberRepository, never()).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("不能将所有权转让给自己")
        void shouldDenySelfTransfer() {
            TeamMember currentOwner = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(currentOwner));

            assertThatThrownBy(() -> teamService.transferOwnership(TEAM_ID, CURRENT_USER_ID))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("新 Owner 不在团队内抛 BadRequestException")
        void shouldDenyTransferToNonMember() {
            TeamMember currentOwner = createMember(CURRENT_USER_ID, TeamRole.OWNER);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team()));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(currentOwner));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.transferOwnership(TEAM_ID, OTHER_USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("不是该团队成员");
        }

        @Test
        @DisplayName("团队不存在抛 TeamNotFoundException")
        void shouldThrowWhenTeamMissing() {
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.transferOwnership(TEAM_ID, OTHER_USER_ID))
                .isInstanceOf(com.taskmanagement.common.exception.TeamNotFoundException.class);
        }
    }
}
