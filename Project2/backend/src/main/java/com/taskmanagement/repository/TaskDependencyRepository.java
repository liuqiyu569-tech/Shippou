package com.taskmanagement.repository;

import com.taskmanagement.entity.TaskDependency;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    /**
     * 删除所有以指定任务集合为后继或前置的依赖关系。
     *
     * <p>用于团队解散和任务删除时清理 task_dependencies。</p>
     *
     * @param taskIds 任务 id 集合
     */
    @Modifying
    @Query("DELETE FROM TaskDependency td "
        + "WHERE td.taskId IN :taskIds OR td.dependsOnTaskId IN :taskIds")
    void deleteAllInvolvingTaskIds(@Param("taskIds") Collection<Long> taskIds);

    boolean existsByTaskIdAndDependsOnTaskId(Long taskId, Long dependsOnTaskId);

    Optional<TaskDependency> findByTaskIdAndDependsOnTaskId(Long taskId, Long dependsOnTaskId);

    void deleteByTaskIdAndDependsOnTaskId(Long taskId, Long dependsOnTaskId);

    List<TaskDependency> findAllByTaskId(Long taskId);

    List<TaskDependency> findAllByDependsOnTaskId(Long dependsOnTaskId);

    List<TaskDependency> findAllByTaskIdInOrDependsOnTaskIdIn(
        Collection<Long> taskIds,
        Collection<Long> dependsOnTaskIds
    );
}
