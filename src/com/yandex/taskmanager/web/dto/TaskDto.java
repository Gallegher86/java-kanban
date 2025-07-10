package com.yandex.taskmanager.web.dto;

import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;

import java.time.Duration;
import java.time.LocalDateTime;

public class TaskDto {
    public int id;
    public Integer epicId;
    public String name;
    public String description;
    public Status status;
    public LocalDateTime startTime;
    public Duration duration;
    public LocalDateTime endTime;

    public static TaskDto fromTask(Task task) {
        TaskDto dto = new TaskDto();
        dto.id = task.getId();
        dto.epicId = null;
        dto.name = task.getName();
        dto.description = task.getDescription();
        dto.status = task.getStatus();
        dto.startTime = task.getStartTime();
        dto.duration = task.getDuration();
        dto.endTime = task.getEndTime();
        return dto;
    }

    public static TaskDto fromEpic(Epic epic) {
        TaskDto dto = new TaskDto();
        dto.id = epic.getId();
        dto.epicId = null;
        dto.name = epic.getName();
        dto.description = epic.getDescription();
        dto.status = epic.getStatus();
        dto.startTime = epic.getStartTime();
        dto.duration = epic.getDuration();
        dto.endTime = epic.getEndTime();
        return dto;
    }

    public static TaskDto fromSubTask(SubTask subTask) {
        TaskDto dto = new TaskDto();
        dto.id = subTask.getId();
        dto.epicId = subTask.getEpicId();
        dto.name = subTask.getName();
        dto.description = subTask.getDescription();
        dto.status = subTask.getStatus();
        dto.startTime = subTask.getStartTime();
        dto.duration = subTask.getDuration();
        dto.endTime = subTask.getEndTime();
        return dto;
    }

    public int getId() {
        return id;
    }

    public Integer getEpicId() {
        return epicId;
    }

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
}
