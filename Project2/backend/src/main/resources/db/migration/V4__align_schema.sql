-- Align schema with database design document

-- 1) teams: add created_by
ALTER TABLE teams
    ADD COLUMN created_by BIGINT NULL AFTER name;

-- Backfill created_by from OWNER membership when possible
UPDATE teams t
JOIN team_members tm ON tm.team_id = t.id AND tm.role = 'OWNER'
SET t.created_by = tm.user_id
WHERE t.created_by IS NULL;

-- If some legacy teams still cannot infer owner, use earliest member as fallback
UPDATE teams t
JOIN (
    SELECT team_id, MIN(user_id) AS fallback_user_id
    FROM team_members
    GROUP BY team_id
) x ON x.team_id = t.id
SET t.created_by = x.fallback_user_id
WHERE t.created_by IS NULL;

ALTER TABLE teams
    MODIFY COLUMN created_by BIGINT NOT NULL,
    ADD CONSTRAINT fk_teams_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD INDEX idx_teams_created_by (created_by);

-- 2) team_members: created_at/updated_at -> joined_at
ALTER TABLE team_members
    ADD COLUMN joined_at DATETIME NULL AFTER role;

UPDATE team_members
SET joined_at = created_at
WHERE joined_at IS NULL;

UPDATE team_members
SET joined_at = CURRENT_TIMESTAMP
WHERE joined_at IS NULL;

ALTER TABLE team_members
    MODIFY COLUMN joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    DROP COLUMN created_at,
    DROP COLUMN updated_at;

-- 3) tasks: user_id -> creator_user_id
ALTER TABLE tasks
    DROP FOREIGN KEY fk_tasks_user_id;

ALTER TABLE tasks
    CHANGE COLUMN user_id creator_user_id BIGINT NOT NULL;

ALTER TABLE tasks
    DROP INDEX idx_tasks_user_id,
    ADD CONSTRAINT fk_tasks_creator_user_id FOREIGN KEY (creator_user_id) REFERENCES users(id),
    ADD INDEX idx_tasks_creator_user_id (creator_user_id);

-- 4) task_assignees: add id + assigned_at
ALTER TABLE task_assignees
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD COLUMN assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER user_id,
    ADD PRIMARY KEY (id),
    ADD CONSTRAINT uk_task_assignees_task_user UNIQUE (task_id, user_id);
