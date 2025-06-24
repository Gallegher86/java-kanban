package com.yandex.taskmanager.model;

import java.time.Duration;
import java.time.LocalDateTime;

public class SubTask extends Task {
    private final int epicId;

    public SubTask(String name, String description, int epicId) {
        super(name, description);
        this.epicId = epicId;
    }

    public SubTask(String name, String description, int epicId, LocalDateTime startTime, Duration duration) {
        super(name, description, startTime, duration);
        this.epicId = epicId;
    }

    public SubTask(int id, String name, String description, int epicId, LocalDateTime startTime, Duration duration) {
        super(id, name, description, startTime, duration);
        this.epicId = epicId;
    }

    public SubTask(int id, String name, String description, Status status, int epicId) {
        super(id, name, description, status);
        this.epicId = epicId;
    }

    public SubTask(int id, String name, String description, Status status, int epicId, LocalDateTime startTime, Duration duration) {
        super(id, name, description, status, startTime, duration);
        this.epicId = epicId;
    }

    public int getEpicId() {
        return epicId;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                id,
                TaskType.SUBTASK,
                name,
                status,
                description,
                epicId,
                startTime != null ? startTime.format(DATE_TIME_FORMATTER) : "null",
                duration != null ? duration.toMinutes() : "null",
                getEndTime() != null ? getEndTime().format(DATE_TIME_FORMATTER) : "null");
    }
}
