package com.yandex.taskmanager.web.dto;

import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;
import com.yandex.taskmanager.model.TaskType;

import java.time.Duration;
import java.time.LocalDateTime;

public class TaskDto {
    public TaskType taskType;
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
        dto.taskType = TaskType.TASK;
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
        dto.taskType = TaskType.EPIC;
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
        dto.taskType = TaskType.SUBTASK;
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

    public static Task toNewTask(TaskDto taskDto) {
        String name = taskDto.getName();
        String description = taskDto.getDescription();
        LocalDateTime startTime = taskDto.getStartTime();
        Duration duration = taskDto.getDuration();
        return new Task(name, description, startTime, duration);
    }

    public static Epic toNewEpic(TaskDto taskDto) {
        String name = taskDto.getName();
        String description = taskDto.getDescription();
        return new Epic(name, description);
    }

    public static SubTask toNewSubTask(TaskDto taskDto) {
        if (taskDto.getEpicId() == null) {
            throw new IllegalArgumentException("'epicId' is required to create SubTask.");
        }

        String name = taskDto.getName();
        String description = taskDto.getDescription();
        int epicId = taskDto.getEpicId();
        LocalDateTime startTime = taskDto.getStartTime();
        Duration duration = taskDto.getDuration();
        return new SubTask(name, description, epicId, startTime, duration);
    }

    public static Task toTask(TaskDto taskDto) {
        int id = taskDto.getId();
        String name = taskDto.getName();
        String description = taskDto.getDescription();
        Status status = taskDto.getStatus();
        LocalDateTime startTime = taskDto.getStartTime();
        Duration duration = taskDto.getDuration();
        return new Task(id, name, description, status, startTime, duration);
    }

    public static SubTask toSubTask(TaskDto taskDto) {
        if (taskDto.getEpicId() == null) {
            throw new IllegalArgumentException("'epicId' is required to create SubTask.");
        }

        int id = taskDto.getId();
        String name = taskDto.getName();
        String description = taskDto.getDescription();
        int epicId = taskDto.getEpicId();
        Status status = taskDto.getStatus();
        LocalDateTime startTime = taskDto.getStartTime();
        Duration duration = taskDto.getDuration();
        return new SubTask(id, name, description, status, epicId, startTime, duration);
    }

    public static Epic toEpic(TaskDto taskDto) {
        int id = taskDto.getId();
        String name = taskDto.getName();
        String description = taskDto.getDescription();
        Status status = taskDto.getStatus();
        LocalDateTime startTime = taskDto.getStartTime();
        Duration duration = taskDto.getDuration();
        LocalDateTime endTime = taskDto.getEndTime();

        Epic epic = new Epic(id, name, description);
        epic.setStatus(status);
        epic.setEpicTime(startTime, duration, endTime);
        return epic;
    }

    public static Task toTaskWithId(TaskDto taskDto, int id) {
        String name = taskDto.getName();
        String description = taskDto.getDescription();
        Status status = taskDto.getStatus();
        LocalDateTime startTime = taskDto.getStartTime();
        Duration duration = taskDto.getDuration();
        return new Task(id, name, description, status, startTime, duration);
    }

    public static SubTask toSubTaskWithId(TaskDto taskDto, int id) {
        if (taskDto.getEpicId() == null) {
            throw new IllegalArgumentException("'epicId' is required to create SubTask.");
        }

        String name = taskDto.getName();
        String description = taskDto.getDescription();
        int epicId = taskDto.getEpicId();
        Status status = taskDto.getStatus();
        LocalDateTime startTime = taskDto.getStartTime();
        Duration duration = taskDto.getDuration();
        return new SubTask(id, name, description, status, epicId, startTime, duration);
    }

    public TaskType getTaskType() {
        return taskType;
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

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
