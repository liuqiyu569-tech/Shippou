package com.taskmanagement.entity;

import com.taskmanagement.entity.enums.TaskLogObjectType;
import com.taskmanagement.entity.enums.TaskLogOperationType;
import com.taskmanagement.entity.enums.TaskLogScopeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "task_operation_logs")
public class TaskOperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "operator_username", nullable = false, length = 50)
    private String operatorUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private TaskLogOperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false, length = 30)
    private TaskLogObjectType objectType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private TaskLogScopeType scopeType;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "task_title", nullable = false, length = 100)
    private String taskTitle;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "team_name", length = 100)
    private String teamName;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;

    @PrePersist
    public void onCreate() {
        if (operationTime == null) {
            operationTime = LocalDateTime.now();
        }
    }
}
