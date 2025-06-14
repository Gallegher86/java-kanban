package com.yandex.taskmanager.service;

import java.io.File;

import java.io.IOException;

public class Managers {
    private Managers() {
    }

    public static TaskManager getDefaultTaskManager() {
        return new InMemoryTaskManager();
    }

    public static TaskManager getFileBackedTaskManager(File saveFile) {
        try {
            return new FileBackedTaskManager(saveFile);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static HistoryManager getDefaultHistoryManager() {
        return new InMemoryHistoryManager();
    }
}