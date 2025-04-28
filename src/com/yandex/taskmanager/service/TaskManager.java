package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Task;

import java.util.ArrayList;

public interface TaskManager {
    void addNewTask(Task task);

    ArrayList<Task> getAllTasks();

    ArrayList<Epic> getAllEpics();

    ArrayList<SubTask> getAllSubTasks();

    ArrayList<Task> getHistoryList();

    void deleteAllTasks();

    Task getTaskById(int id);

    ArrayList<SubTask> getEpicSubTasks(int id);

    void deleteTaskById(int id);

    void updateTask(Task newTask);

    TaskManagerStatus getTaskManagerStatus();//возможно лишнее

    int getTaskCount();//возможно лишнее
}
