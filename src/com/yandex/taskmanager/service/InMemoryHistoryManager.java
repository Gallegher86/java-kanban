package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Task;

import java.util.List;
import java.util.ArrayList;

public class InMemoryHistoryManager implements HistoryManager {
    private final List<Task> historyList = new ArrayList<>();
    private final int historyListMaxSize = 10;

    public List<Task> getHistory() {
        return new ArrayList<>(historyList);
    }

    public void addHistory(Task task) {
        if (historyList.size() == historyListMaxSize) {
            historyList.removeFirst();
        }
        historyList.add(task);
    }

    public void clearHistory() {
        historyList.clear();
    }
}
