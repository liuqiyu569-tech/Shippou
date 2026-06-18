package com.taskmanagement.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.common.exception.ForbiddenException;
import com.taskmanagement.dto.response.TeamTaskResponse;
import com.taskmanagement.dto.response.UserInfoResponse;
import com.taskmanagement.entity.enums.TaskPriority;
import com.taskmanagement.entity.enums.TaskStatus;
import com.taskmanagement.security.JwtTokenProvider;
import com.taskmanagement.service.TeamTaskService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TeamTaskController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TeamTaskController API tests")
class TeamTaskControllerTest {

    private static final Long TEAM_ID = 10L;
    private static final Long TASK_ID = 101L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamTaskService teamTaskService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Owner/Admin can create a team task")
    void shouldCreateTeamTask() throws Exception {
        TeamTaskResponse response = createTeamTaskResponse();
        when(teamTaskService.createTeamTask(eq(TEAM_ID), org.mockito.ArgumentMatchers.any()))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/teams/{teamId}/tasks", TEAM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Implement permission checks",
                      "description": "Finish backend module",
                      "status": "TODO",
                      "priority": "HIGH",
                      "dueAt": "2026-05-05T23:59:59"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(TASK_ID))
            .andExpect(jsonPath("$.data.teamId").value(TEAM_ID));

        verify(teamTaskService).createTeamTask(eq(TEAM_ID), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Team task list forwards filters to service")
    void shouldListTeamTasksWithFilters() throws Exception {
        PageResult<TeamTaskResponse> page = new PageResult<>(1, 2, 5, List.of(createTeamTaskResponse()));
        when(teamTaskService.getTeamTasks(
            eq(TEAM_ID),
            eq(TaskStatus.TODO),
            eq(TaskPriority.HIGH),
            eq(2L),
            eq(LocalDateTime.of(2026, 5, 1, 0, 0)),
            eq(LocalDateTime.of(2026, 5, 5, 23, 59, 59)),
            eq("permission"),
            eq(2),
            eq(5)
        )).thenReturn(page);

        mockMvc.perform(get("/api/v1/teams/{teamId}/tasks", TEAM_ID)
                .param("status", "TODO")
                .param("priority", "HIGH")
                .param("assigneeId", "2")
                .param("dueStart", "2026-05-01T00:00:00")
                .param("dueEnd", "2026-05-05T23:59:59")
                .param("keyword", "permission")
                .param("page", "2")
                .param("pageSize", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(TASK_ID));
    }

    @Test
    @DisplayName("Empty assignment payload is rejected before service call")
    void shouldRejectEmptyAssignmentPayload() throws Exception {
        mockMvc.perform(put("/api/v1/teams/{teamId}/tasks/{taskId}/assignees", TEAM_ID, TASK_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userIds\":[]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));

        verify(teamTaskService, never()).assignTask(eq(TEAM_ID), eq(TASK_ID), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Service-layer permission errors are returned as 403")
    void shouldReturnForbiddenWhenServiceRejectsUpdate() throws Exception {
        when(teamTaskService.updateTeamTask(eq(TEAM_ID), eq(TASK_ID), org.mockito.ArgumentMatchers.any()))
            .thenThrow(new ForbiddenException("Only assigned members can update status"));

        mockMvc.perform(put("/api/v1/teams/{teamId}/tasks/{taskId}", TEAM_ID, TASK_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("Only assigned members can update status"));
    }

    private TeamTaskResponse createTeamTaskResponse() {
        return TeamTaskResponse.builder()
            .id(TASK_ID)
            .title("Implement permission checks")
            .description("Finish backend module")
            .status(TaskStatus.TODO)
            .priority(TaskPriority.HIGH)
            .dueAt(LocalDateTime.of(2026, 5, 5, 23, 59, 59))
            .teamId(TEAM_ID)
            .creator(UserInfoResponse.builder().id(1L).username("shijunkai").build())
            .assignees(List.of(TeamTaskResponse.AssigneeInfo.builder().userId(2L).username("alice").build()))
            .createdAt(LocalDateTime.of(2026, 4, 30, 20, 0))
            .updatedAt(LocalDateTime.of(2026, 4, 30, 20, 0))
            .build();
    }
}
