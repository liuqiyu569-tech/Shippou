package com.taskmanagement.service.impl;

import com.taskmanagement.common.dto.PageResult;
import com.taskmanagement.dto.response.UserInfoResponse;
import com.taskmanagement.entity.User;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.service.UserQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserInfoResponse> queryUsers(@Nullable String keyword, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize);
        Page<User> userPage = (keyword != null && !keyword.isBlank())
            ? userRepository.findByUsernameContainingIgnoreCase(keyword, pageRequest)
            : userRepository.findAll(pageRequest);

        List<UserInfoResponse> items = userPage.getContent().stream()
            .map(u -> UserInfoResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .build())
            .toList();

        return new PageResult<>(userPage.getTotalElements(), page, pageSize, items);
    }
}
