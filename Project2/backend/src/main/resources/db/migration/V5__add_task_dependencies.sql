-- task_dependencies 表（任务依赖关系）
-- 语义：一条记录 (task_id, depends_on_task_id) 表示 task_id 依赖 depends_on_task_id，
-- 即 depends_on_task_id 是 task_id 的前置任务。
CREATE TABLE task_dependencies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    depends_on_task_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_dependencies_task_id FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT fk_task_dependencies_depends_on FOREIGN KEY (depends_on_task_id) REFERENCES tasks(id),
    CONSTRAINT uk_task_dependencies_task_dep UNIQUE (task_id, depends_on_task_id),
    INDEX idx_task_dependencies_task_id (task_id),
    INDEX idx_task_dependencies_depends_on (depends_on_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
