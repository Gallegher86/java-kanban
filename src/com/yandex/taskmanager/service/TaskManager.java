package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Task;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public interface TaskManager {
    void addTask(Task task);

    void addEpic(Epic epic);

    void addSubTask(SubTask subTask);

    List<Task> getAllTasks();

    TreeSet<Task> getPrioritizedTasks();

    List<Task> getTasks();

    List<Epic> getEpics();

    List<SubTask> getSubTasks();

    List<Task> getHistory();

    void clearHistory();

    void deleteAllTasks();

    Optional<Task> getTaskById(int id);

    List<SubTask> getEpicSubTasks(int id);

    void deleteTaskById(int id);

    void updateTask(Task newTask);

    void updateEpic(Epic epic);

    void updateSubTask(SubTask subTask);

    int getIdCounter();
}
