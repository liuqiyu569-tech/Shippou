package com.taskmanagement.repository;

import com.taskmanagement.entity.TaskAssignee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, Long> {

    List<TaskAssignee> findAllByTaskId(Long taskId);

    List<TaskAssignee> findAllByUserId(Long userId);

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    @Query("SELECT ta.taskId FROM TaskAssignee ta WHERE ta.userId = :userId")
    List<Long> findTaskIdsByUserId(@Param("userId") Long userId);

    @Modifying
    void deleteByTaskId(Long taskId);

    @Modifying
    void deleteByTaskIdAndUserId(Long taskId, Long userId);

    @Modifying
    void deleteByTaskIdIn(List<Long> taskIds);
}
