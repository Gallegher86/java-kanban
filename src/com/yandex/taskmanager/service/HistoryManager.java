package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Task;
import java.util.ArrayList;

public interface HistoryManager {
    void addHistory(Task task);

    ArrayList<Task> getHistory();
}
