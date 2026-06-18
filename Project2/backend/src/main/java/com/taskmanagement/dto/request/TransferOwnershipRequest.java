package com.taskmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferOwnershipRequest {

    @NotNull(message = "新 Owner 用户 ID 不能为空")
    @Positive(message = "新 Owner 用户 ID 必须为正数")
    private Long newOwnerId;
}
