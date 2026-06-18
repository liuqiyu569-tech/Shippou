package com.taskmanagement.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TeamDetailResponse {

    private final Long id;
    private final String name;
    private final String myRole;
    private final List<MemberInfo> members;
    private final LocalDateTime createdAt;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MemberInfo {
        private final Long userId;
        private final String username;
        private final String role;
        private final LocalDateTime joinedAt;
    }
}
