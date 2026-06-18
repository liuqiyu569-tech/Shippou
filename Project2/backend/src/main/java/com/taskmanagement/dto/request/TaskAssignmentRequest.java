package com.taskmanagement.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TaskAssignmentRequest {

    @NotEmpty(message = "被指派用户列表不能为空")
    private List<
        @NotNull(message = "用户 ID 不能为空")
        @Positive(message = "用户 ID 必须为正数") Long
    > userIds;
}
