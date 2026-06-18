package com.taskmanagement.controller;

import com.taskmanagement.common.dto.ApiResponse;
import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.dto.response.UserInfoResponse;
import com.taskmanagement.service.UserQueryService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户查询控制器，提供按关键字模糊分页搜索用户。
 *
 * <p>登录后即可访问，用于添加成员时的用户搜索下拉。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserQueryController {

    private final UserQueryService userQueryService;

    /**
     * 根据关键字分页查询用户，按用户名排序。
     *
     * @param keyword 用户名关键字（可选）
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 用户信息分页
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<UserInfoResponse>>> queryUsers(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") @Positive(message = "页码必须为正数") int page,
        @RequestParam(defaultValue = "10") @Positive(message = "每页大小必须为正数") int pageSize
    ) {
        PageResult<UserInfoResponse> result = userQueryService.queryUsers(keyword, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("查询成功", result));
    }
}
