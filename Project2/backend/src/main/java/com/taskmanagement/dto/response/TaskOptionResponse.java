package com.taskmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 任务下拉候选项响应体，仅包含 id 与 title，用于添加依赖时的下拉菜单。
 */
@Getter
@Builder
@AllArgsConstructor
public class TaskOptionResponse {

    private final Long id;
    private final String title;
}
