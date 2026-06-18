package com.taskmanagement.common.utils;

import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.enums.DueStatus;
import com.taskmanagement.entity.enums.TaskStatus;
import java.time.LocalDateTime;

public final class DueStatusCalculator {

    public static final int DEFAULT_UPCOMING_DAYS = 3;

    private DueStatusCalculator() {
    }

    public static DueStatus calculate(Task task) {
        return calculate(task, DEFAULT_UPCOMING_DAYS, LocalDateTime.now());
    }

    public static DueStatus calculate(Task task, int upcomingDays, LocalDateTime now) {
        if (task.getStatus() == TaskStatus.DONE) {
            return DueStatus.DONE;
        }
        if (task.getDueAt() == null) {
            return DueStatus.NONE;
        }
        if (task.getDueAt().isBefore(now)) {
            return DueStatus.OVERDUE;
        }
        if (!task.getDueAt().isAfter(now.plusDays(upcomingDays))) {
            return DueStatus.UPCOMING_DUE;
        }
        return DueStatus.NORMAL;
    }
}
