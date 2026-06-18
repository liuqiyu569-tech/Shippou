package com.taskmanagement.repository;

import com.taskmanagement.entity.TeamMember;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findAllByTeamIdOrderByJoinedAtAsc(Long teamId);

    List<TeamMember> findAllByTeamIdAndUserIdIn(Long teamId, Collection<Long> userIds);

    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    @Query("SELECT tm.teamId FROM TeamMember tm WHERE tm.userId = :userId ORDER BY tm.joinedAt DESC")
    Page<Long> findTeamIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    long countByTeamId(Long teamId);

    @Modifying
    void deleteByTeamId(Long teamId);

    @Modifying
    void deleteByTeamIdAndUserId(Long teamId, Long userId);
}
