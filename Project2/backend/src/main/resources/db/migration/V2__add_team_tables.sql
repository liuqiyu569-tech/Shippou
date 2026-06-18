-- teams 表
CREATE TABLE teams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- team_members 表（团队-用户关系）
CREATE TABLE team_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_team_members_team_id FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_team_members_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_team_members_team_user UNIQUE (team_id, user_id),
    INDEX idx_team_members_team_id (team_id),
    INDEX idx_team_members_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- tasks 表增加 team_id 字段
ALTER TABLE tasks ADD COLUMN team_id BIGINT NULL AFTER user_id;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_team_id FOREIGN KEY (team_id) REFERENCES teams(id);
ALTER TABLE tasks ADD INDEX idx_tasks_team_id (team_id);

-- task_assignees 表（任务-被分配者关系）
CREATE TABLE task_assignees (
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, user_id),
    CONSTRAINT fk_task_assignees_task_id FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT fk_task_assignees_user_id FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
