CREATE TABLE task_operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT NOT NULL,
    operator_username VARCHAR(50) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    object_type VARCHAR(30) NOT NULL,
    scope_type VARCHAR(20) NOT NULL,
    task_id BIGINT NOT NULL,
    task_title VARCHAR(100) NOT NULL,
    team_id BIGINT NULL,
    team_name VARCHAR(100) NULL,
    summary VARCHAR(500) NOT NULL,
    operation_time DATETIME NOT NULL,
    INDEX idx_task_operation_logs_task_time (task_id, operation_time),
    INDEX idx_task_operation_logs_operator_time (operator_id, operation_time),
    INDEX idx_task_operation_logs_team_time (team_id, operation_time),
    INDEX idx_task_operation_logs_scope_time (scope_type, operation_time),
    INDEX idx_task_operation_logs_type_time (operation_type, object_type, operation_time)
);
