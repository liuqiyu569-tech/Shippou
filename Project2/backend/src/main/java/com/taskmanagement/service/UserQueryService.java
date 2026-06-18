package com.taskmanagement.service;

import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.dto.response.UserInfoResponse;
import org.springframework.lang.Nullable;

/**
 * 用户查询服务接口，提供按关键字模糊分页搜索用户。
 */
public interface UserQueryService {

    /**
     * 根据关键字分页查询用户，按用户名排序。
     *
     * @param keyword 用户名关键字（可选）
     * @param page 页码
     * @param pageSize 每页大小
     * @return 用户信息分页结果
     */
    PageResult<UserInfoResponse> queryUsers(@Nullable String keyword, int page, int pageSize);
}
