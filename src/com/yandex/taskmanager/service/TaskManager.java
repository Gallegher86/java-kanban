package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Task;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public interface TaskManager {
    Task addTask(Task task);

    Epic addEpic(Epic epic);

    SubTask addSubTask(SubTask subTask);

    List<Task> getAllTasks();

    TreeSet<Task> getPrioritizedTasks();

    List<Task> getTasks();

    List<Epic> getEpics();

    List<SubTask> getSubTasks();

    List<Task> getHistory();

    void clearHistory();

    void deleteAllTasks();

    Optional<Task> findAnyTaskById(int id);

    Task getTaskById(int id);

    Epic getEpicById(int id);

    SubTask getSubTaskById(int id);

    List<SubTask> getEpicSubTasks(int id);

    void deleteAnyTaskById(int id);

    void deleteTask(int id);

    void deleteEpic(int id);

    void deleteSubTask(int id);

    void updateTask(Task newTask);

    void updateEpic(Epic epic);

    void updateSubTask(SubTask subTask);

    int getIdCounter();
}
