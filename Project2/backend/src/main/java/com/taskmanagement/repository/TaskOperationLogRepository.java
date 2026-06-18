package com.taskmanagement.repository;

import com.taskmanagement.entity.TaskOperationLog;
import com.taskmanagement.entity.enums.TaskLogScopeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskOperationLogRepository
    extends JpaRepository<TaskOperationLog, Long>, JpaSpecificationExecutor<TaskOperationLog> {

    boolean existsByTaskIdAndScopeTypeAndOperatorId(Long taskId, TaskLogScopeType scopeType, Long operatorId);

    boolean existsByTaskIdAndScopeTypeAndTeamId(Long taskId, TaskLogScopeType scopeType, Long teamId);
}
