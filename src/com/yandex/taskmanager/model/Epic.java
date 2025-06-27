package com.yandex.taskmanager.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class Epic extends Task {
    private final List<Integer> subTaskIds;
    private LocalDateTime endTime;

    public Epic(String name, String description) {
        super(name, description);
        this.subTaskIds = new ArrayList<>();
    }

    public Epic(int id, String name, String description) {
        super(id, name, description);
        this.subTaskIds = new ArrayList<>();
    }

    public Epic(Epic other, List<Integer> subTaskIds) {
        super(other.id, other.name, other.description, other.status);
        this.subTaskIds = new ArrayList<>(subTaskIds);
    }

    public List<Integer> getSubTaskIdList() {
        return new ArrayList<>(subTaskIds);
    }

    public void addSubTaskId(Integer subTaskId) {
        subTaskIds.add(subTaskId);
    }

    public void removeSubTaskId(Integer subTaskId) {
        subTaskIds.remove(subTaskId);
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setEpicTime(LocalDateTime startTime, Duration duration, LocalDateTime endTime) {
        this.startTime = startTime;
        this.duration = duration;
        this.endTime = endTime;
    }

    @Override
    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                id,
                TaskType.EPIC,
                name,
                status,
                description,
                "-",
                startTime != null ? startTime.format(DATE_TIME_FORMATTER) : "null",
                duration != null ? duration.toMinutes() : "null");
    }
}

