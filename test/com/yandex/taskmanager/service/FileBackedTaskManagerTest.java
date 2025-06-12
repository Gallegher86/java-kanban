package com.yandex.taskmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;

import java.io.IOException;

import java.io.File;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> {
    private File tempFile;

    @Override
    protected FileBackedTaskManager createManager() {
        try {
            tempFile = File.createTempFile("tasks_", ".csv");
            System.out.println(tempFile.getAbsolutePath());
            return new FileBackedTaskManager(tempFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void deleteTempFile() {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile.toPath());
                System.out.println("Удален: " + tempFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Не удалось удалить временный файл: " + tempFile.getAbsolutePath());
            }
        }
    }

    @Test
    public void constructorMustThrowExceptionIfFileIsNull() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> new FileBackedTaskManager(null));
        assertTrue(ex1.getMessage().contains("null"), "Сообщение об ошибке должно содержать слово 'null'.");
    }

    @Test
    public void constructorMustThrowExceptionIfFileNotExist() {
        File missingFile;

        try {
            missingFile = File.createTempFile("NoFile_", ".csv");
            Files.deleteIfExists(missingFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        IOException ex1 = assertThrows(IOException.class, () -> new FileBackedTaskManager(missingFile));
        assertTrue(ex1.getMessage().contains("not exist"), "Сообщение об ошибке должно содержать слово 'not exist'.");
    }

    @Test
    public void loadFromAnEmptyFileWorksCorrectly() {
        try {
            FileBackedTaskManager newManager = FileBackedTaskManager.loadFromFile(tempFile);
            checkTaskCountForEmptyManager(newManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void saveAndLoadWorksCorrectly() {
        try {
            List<Task> tasksInOldManager;
            List<Task> tasksInNewManager;

            createSixTaskListForTests(manager);

            FileBackedTaskManager newManager = FileBackedTaskManager.loadFromFile(tempFile);

            tasksInOldManager = manager.getAllTasks();
            tasksInNewManager = newManager.getAllTasks();

            checkTaskCountForSixTasks(newManager);
            checkTasksUnchangedCustom(tasksInOldManager, tasksInNewManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void saveAndLoadWithDeleteByIdWorksCorrectly() {
        List<Task> tasksInOldManager;
        List<Task> tasksInNewManager;

        createSixTaskListForTests(manager);

        manager.deleteTaskById(1);
        manager.deleteTaskById(2);

        FileBackedTaskManager newManager = FileBackedTaskManager.loadFromFile(tempFile);

        tasksInOldManager = manager.getAllTasks();
        tasksInNewManager = newManager.getAllTasks();

        checkTasksUnchangedCustom(tasksInOldManager, tasksInNewManager);
        checkTaskCountCustom(newManager, 0, 1, 1, 6);
    }

    @Test
    public void saveAndLoadWithDeleteAllTasksWorksCorrectly() {
        createSixTaskListForTests(manager);

        manager.deleteAllTasks();

        FileBackedTaskManager newManager = FileBackedTaskManager.loadFromFile(tempFile);

        checkTaskCountForEmptyManager(newManager);
    }

    @Test
    public void saveAndLoadWithUpdateTaskWorksCorrectly() {
        List<Task> tasksInOldManager;
        List<Task> tasksInNewManager;

        createThreeTaskListForTests(manager);

        Task task = new Task(task1.getId(), "TASK CHANGED", "DESCRIPTION CHANGED", Status.DONE);
        Epic epic = new Epic(epic1.getId(), "EPIC CHANGED", "DESCRIPTION CHANGED");
        SubTask subTask = new SubTask(subTask1.getId(), "SUBTASK CHANGED", "DESCRIPTION CHANGED",
                Status.IN_PROGRESS, subTask1.getEpicId());

        manager.updateTask(task);
        manager.updateEpic(epic);
        manager.updateSubTask(subTask);

        FileBackedTaskManager newManager = FileBackedTaskManager.loadFromFile(tempFile);

        tasksInOldManager = manager.getAllTasks();
        tasksInNewManager = newManager.getAllTasks();

        checkTasksUnchangedCustom(tasksInOldManager, tasksInNewManager);
        checkTaskCountCustom(newManager, 1, 1, 1, 3);
    }
}
