package com.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamCreateRequest {

    @NotBlank(message = "团队名称不能为空")
    @Size(max = 100, message = "团队名称最多100个字符")
    private String name;

    private List<Long> memberIds;
}
