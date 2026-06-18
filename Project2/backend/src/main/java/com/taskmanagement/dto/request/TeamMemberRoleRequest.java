package com.taskmanagement.dto.request;

import com.taskmanagement.entity.enums.TeamRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamMemberRoleRequest {

    @NotNull(message = "角色不能为空")
    private TeamRole role;
}
