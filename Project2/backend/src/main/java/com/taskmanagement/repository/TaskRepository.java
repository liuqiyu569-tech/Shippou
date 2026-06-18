package com.taskmanagement.repository;

import com.taskmanagement.entity.Task;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    List<Task> findAllByCreatorUserIdOrderByCreatedAtDesc(Long creatorUserId);

    List<Task> findAllByCreatorUserIdAndTeamIdIsNullOrderByCreatedAtDesc(Long creatorUserId);

    Optional<Task> findByIdAndCreatorUserId(Long id, Long creatorUserId);

    Optional<Task> findByIdAndCreatorUserIdAndTeamIdIsNull(Long id, Long creatorUserId);

    boolean existsByIdAndCreatorUserId(Long id, Long creatorUserId);

    List<Task> findAllByTeamId(Long teamId);

    Optional<Task> findByIdAndTeamId(Long id, Long teamId);

    /**
     * 查询当前用户的所有个人任务（无关键字），按 title 升序。
     *
     * @param userId 当前用户 ID
     * @return 该用户的所有个人任务
     */
    @Query("SELECT t FROM Task t WHERE t.creatorUserId = :userId AND t.teamId IS NULL "
        + "ORDER BY t.title ASC")
    List<Task> findPersonalTaskOptions(@Param("userId") Long userId);

    /**
     * 按关键字模糊查询当前用户的个人任务，按 title 升序。
     *
     * @param userId 当前用户 ID
     * @param keyword 已转小写的关键字
     * @return 匹配的个人任务列表
     */
    @Query("SELECT t FROM Task t WHERE t.creatorUserId = :userId AND t.teamId IS NULL "
        + "AND LOWER(t.title) LIKE CONCAT('%', :keyword, '%') "
        + "ORDER BY t.title ASC")
    List<Task> findPersonalTaskOptionsByKeyword(
        @Param("userId") Long userId,
        @Param("keyword") String keyword
    );

    /**
     * 查询指定团队的所有任务（无关键字），按 title 升序。
     *
     * @param teamId 团队 ID
     * @return 该团队的所有任务
     */
    @Query("SELECT t FROM Task t WHERE t.teamId = :teamId ORDER BY t.title ASC")
    List<Task> findTeamTaskOptions(@Param("teamId") Long teamId);

    /**
     * 按关键字模糊查询指定团队的任务，按 title 升序。
     *
     * @param teamId 团队 ID
     * @param keyword 已转小写的关键字
     * @return 匹配的团队任务列表
     */
    @Query("SELECT t FROM Task t WHERE t.teamId = :teamId "
        + "AND LOWER(t.title) LIKE CONCAT('%', :keyword, '%') "
        + "ORDER BY t.title ASC")
    List<Task> findTeamTaskOptionsByKeyword(
        @Param("teamId") Long teamId,
        @Param("keyword") String keyword
    );
}
