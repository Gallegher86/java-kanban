package com.yandex.taskmanager.service;

import java.io.IOException;
import java.nio.file.Path;

public class Managers {
    private Managers() {
    }

    public static TaskManager getDefaultTaskManager() {
        return new InMemoryTaskManager();
    }

    public static TaskManager getFileBackedTaskManager(Path saveFilePath) {
        try {
            return new FileBackedTaskManager(saveFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static HistoryManager getDefaultHistoryManager() {
        return new InMemoryHistoryManager();
    }
}