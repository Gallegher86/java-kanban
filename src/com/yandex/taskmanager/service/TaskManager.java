package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskManager {
    void addNewTask(Task task);

    List<Task> getAllTasks();

    List<Epic> getAllEpics();

    List<SubTask> getAllSubTasks();

    List<Task> getHistory();

    void clearHistory();

    void deleteAllTasks();

    Optional<Task> getTaskById(int id);

    List<SubTask> getEpicSubTasks(int id);

    void deleteTaskById(int id);

    void updateTask(Task newTask);

    TaskManagerStatus getTaskManagerStatus();

    int getTaskCount();
}
