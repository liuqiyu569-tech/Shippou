package com.taskmanagement.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.dto.response.TaskOperationLogResponse;
import com.taskmanagement.dto.response.UserInfoResponse;
import com.taskmanagement.entity.enums.TaskLogObjectType;
import com.taskmanagement.entity.enums.TaskLogOperationType;
import com.taskmanagement.entity.enums.TaskLogScopeType;
import com.taskmanagement.security.JwtTokenProvider;
import com.taskmanagement.service.TaskService;
import com.taskmanagement.service.TeamTaskService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskOperationLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TaskOperationLogController API tests")
class TaskOperationLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;
    @MockBean
    private TeamTaskService teamTaskService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("personal all-log endpoint returns paged task logs")
    void shouldQueryCurrentUserTaskLogs() throws Exception {
        PageResult<TaskOperationLogResponse> page = new PageResult<>(1, 1, 10, List.of(createLog()));
        when(taskService.queryCurrentUserTaskLogs(
            eq(TaskLogOperationType.CREATE),
            eq(TaskLogObjectType.TASK_INFO),
            eq("task"),
            eq(LocalDateTime.of(2026, 6, 1, 0, 0)),
            eq(LocalDateTime.of(2026, 6, 4, 23, 59)),
            eq(1),
            eq(10)
        )).thenReturn(page);

        mockMvc.perform(get("/api/v1/user/task-logs")
                .param("operationType", "CREATE")
                .param("objectType", "TASK_INFO")
                .param("keyword", "task")
                .param("startTime", "2026-06-01T00:00:00")
                .param("endTime", "2026-06-04T23:59:00")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].operationType").value("CREATE"))
            .andExpect(jsonPath("$.data.items[0].task.title").value("Task backend module"));
    }

    @Test
    @DisplayName("team task log endpoint returns paged task logs")
    void shouldQueryTeamTaskLogs() throws Exception {
        PageResult<TaskOperationLogResponse> page = new PageResult<>(1, 1, 10, List.of(createLog()));
        when(teamTaskService.queryTeamTaskLogs(
            eq(10L),
            eq(100L),
            eq(TaskLogOperationType.UPDATE),
            eq(TaskLogObjectType.TASK_INFO),
            eq(1),
            eq(10)
        )).thenReturn(page);

        mockMvc.perform(get("/api/v1/teams/{teamId}/tasks/{taskId}/logs", 10L, 100L)
                .param("operationType", "UPDATE")
                .param("objectType", "TASK_INFO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].task.id").value(100));
    }

    private TaskOperationLogResponse createLog() {
        return TaskOperationLogResponse.builder()
            .id(1L)
            .operator(UserInfoResponse.builder().id(1L).username("alice").build())
            .operationType(TaskLogOperationType.CREATE)
            .objectType(TaskLogObjectType.TASK_INFO)
            .scopeType(TaskLogScopeType.PERSONAL)
            .task(TaskOperationLogResponse.TaskSnapshot.builder()
                .id(100L)
                .title("Task backend module")
                .build())
            .summary("Created personal task [Task backend module]")
            .operationTime(LocalDateTime.of(2026, 6, 1, 10, 0))
            .build();
    }
}
