package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Task;

import java.util.ArrayList;

public class InMemoryHistoryManager implements HistoryManager {
    private final ArrayList<Task> historyList = new ArrayList<>();

    public ArrayList<Task> getHistory() {
        return historyList;
    }

    public void addHistory(Task task) {
        if (historyList.size() == 10) {
            historyList.remove(0);
        }
        historyList.add(task);
    }
}
