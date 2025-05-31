package com.yandex.taskmanager.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ManagersTest {

    @Test
    public void managerCreatesDefaultTaskManagerNotNull() {
        TaskManager taskManager = Managers.getDefaultTaskManager();

        assertNotNull(taskManager,
                "getDefaultTaskManager() должен возвращать TaskManager без параметров.");
    }

    @Test
    public void managerCreatesDefaultHistoryManagerNotNull() {
        HistoryManager historyManager = Managers.getDefaultHistoryManager();

        assertNotNull(historyManager,
                "getDefaultHistoryManager() должен возвращать HistoryManager без параметров.");
    }
}