package com.yandex.taskmanager.model;

import java.util.Objects;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Task {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
    protected final int id;
    protected final String name;
    protected final String description;
    protected Status status = Status.NEW;
    protected LocalDateTime startTime;
    protected Duration duration;

    public Task(String name, String description) {
        this.id = 0;
        this.name = name;
        this.description = description;
    }

    public Task(String name, String description, LocalDateTime startTime, Duration duration) {
        this.id = 0;
        this.name = name;
        this.description = description;
        this.startTime = (startTime != null) ? startTime.withSecond(0).withNano(0) : null;
        this.duration = duration;
    }

    public Task(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Task(int id, String name, String description, LocalDateTime startTime, Duration duration) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startTime = (startTime != null) ? startTime.withSecond(0).withNano(0) : null;
        this.duration = duration;
    }

    public Task(int id, String name, String description, Status status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public Task(int id, String name, String description, Status status, LocalDateTime startTime, Duration duration) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.startTime = (startTime != null) ? startTime.withSecond(0).withNano(0) : null;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getId() {
        return id;
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

    public LocalDateTime getEndTime() {
        if (startTime == null || duration == null) {
            return null;
        }
        return startTime.plus(duration);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s",
                id,
                TaskType.TASK,
                name,
                status,
                description,
                startTime != null ? startTime.format(DATE_TIME_FORMATTER) : "null",
                duration != null ? duration.toMinutes() : "null"
        );
    }
}
