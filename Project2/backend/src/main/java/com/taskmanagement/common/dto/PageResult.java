package com.taskmanagement.common.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageResult<T> {

    private final long total;
    private final int page;
    private final int pageSize;
    private final List<T> items;
}
