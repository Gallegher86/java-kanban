package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Task;
import java.util.List;

public interface HistoryManager {
    void addHistory(Task task);

    List<Task> getHistory();

    void clearHistory();

    void removeHistory(int id);
}
