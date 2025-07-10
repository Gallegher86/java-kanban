package com.yandex.taskmanager.web.dto;

import com.yandex.taskmanager.model.Status;

import java.time.Duration;
import java.time.LocalDateTime;

public class TaskDto {
    public String name;
    public String description;
    public Status status;
    public LocalDateTime startTime;
    public Duration duration;
    public int epicId;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public int getEpicId() {
        return epicId;
    }
}
