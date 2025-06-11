package com.yandex.taskmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;
import com.yandex.taskmanager.model.TaskType;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> {
    private Path tempFilePath;

    @Override
    protected FileBackedTaskManager createManager() {
        try {
            tempFilePath = Files.createTempFile("tasks_", ".csv");
            System.out.println(tempFilePath.toAbsolutePath());
            return new FileBackedTaskManager(tempFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void deleteTempFilePath() {
        if (tempFilePath != null) {
            try {
                Files.deleteIfExists(tempFilePath);
                System.out.println("Удален: " + tempFilePath);
            } catch (IOException e) {
                System.err.println("Не удалось удалить временный файл: " + tempFilePath);
            }
        }
    }

    @Test
    public void constructorMustThrowExceptionIfPathIsNull() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                new FileBackedTaskManager(null));
        assertTrue(ex1.getMessage().contains("null"),
                "Сообщение об ошибке должно содержать слово 'null'.");
    }

    @Test
    public void constructorMustThrowExceptionIfPathIsDirectory() throws IOException {
        Path dir = Files.createTempDirectory("testDir");

        IOException ex1 = assertThrows(IOException.class, () ->
                new FileBackedTaskManager(dir));
        assertTrue(ex1.getMessage().contains("directory"),
                "Сообщение об ошибке должно содержать слово 'directory'.");

        Files.deleteIfExists(dir);
    }

    @Test
    public void constructorMustCreateNewFileIfItsNotExistsOnProvidedPath() throws IOException {
        Path pathWithNoFile = Files.createTempFile("NoFile_", ".csv");
        Files.deleteIfExists(pathWithNoFile);

        manager = new FileBackedTaskManager(pathWithNoFile);
        assertTrue(Files.exists(pathWithNoFile), "Менеджер должен создать файл сохранения.");

        Files.deleteIfExists(pathWithNoFile);
    }

    @Test
    public void loadFromAnEmptyFileWorksProperly() {
        try {
            task1 = new Task("Task1", "Description task1");

            manager.addTask(task1);
            List<Task> savedTask = loadFromFile(tempFilePath);
            List<Task> inMemoryTask = manager.getAllTasks();
            checkTasksUnchangedCustom(savedTask, inMemoryTask);

            manager.deleteTaskById(1);
            savedTask = loadFromFile(tempFilePath);
            inMemoryTask = manager.getAllTasks();

            assertTrue(savedTask.isEmpty(), "Задача должна быть удалена из файла сохранений.");
            assertTrue(inMemoryTask.isEmpty(), "Задача должна быть удалена из памяти менеджера.");

            FileBackedTaskManager newManager = FileBackedTaskManager.loadFromFile(tempFilePath.toFile());
            savedTask = loadFromFile(tempFilePath);
            inMemoryTask = newManager.getAllTasks();

            assertTrue(savedTask.isEmpty(), "В файле сохранений не должно быть задач.");
            assertTrue(inMemoryTask.isEmpty(), "В новом менеджере не должно быть задач.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void loadFromFileWithDataWorksProperly() {
        try {
            createSixTaskListForTests(manager);

            List<Task> savedTask = loadFromFile(tempFilePath);
            List<Task> inMemoryTask = manager.getAllTasks();
            checkTaskCountForSixTasks(manager);
            checkTasksUnchangedCustom(savedTask, inMemoryTask);

            FileBackedTaskManager newManager = FileBackedTaskManager.loadFromFile(tempFilePath.toFile());
            savedTask = loadFromFile(tempFilePath);
            inMemoryTask = newManager.getAllTasks();
            checkTaskCountForSixTasks(newManager);
            checkTasksUnchangedCustom(savedTask, inMemoryTask);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void loadFromFileCountsIdCounterCorrectly() {
        createSixTaskListForTests(manager);

        manager.deleteTaskById(1);
        manager.deleteTaskById(2);

        FileBackedTaskManager newManager = FileBackedTaskManager.loadFromFile(tempFilePath.toFile());
        checkTaskCountCustom(newManager, 0, 1, 1, 6);
    }

    @Test
    public void saveWorksProperlyOnAddTasks() {
        try {
            List<Task> savedTask = loadFromFile(tempFilePath);
            List<Task> inMemoryTask = manager.getAllTasks();

            assertTrue(savedTask.isEmpty(), "Файл сохранения должен быть пустым.");
            assertTrue(inMemoryTask.isEmpty(), "В памяти менеджера не должно быть задач.");
            checkTaskCountForEmptyManager(manager);

            createSixTaskListForTests(manager);
            savedTask = loadFromFile(tempFilePath);
            inMemoryTask = manager.getAllTasks();
            checkTaskCountForSixTasks(manager);
            checkTasksUnchangedCustom(savedTask, inMemoryTask);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void saveWorksProperlyOnDeleteAllTasks() {
        try {
            createSixTaskListForTests(manager);

            manager.deleteAllTasks();
            List<Task> savedTask = loadFromFile(tempFilePath);
            List<Task> inMemoryTask = manager.getAllTasks();
            checkTaskCountForEmptyManager(manager);
            checkTasksUnchangedCustom(savedTask, inMemoryTask);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void saveWorksProperlyOnDeleteRTaskById() {
        try {
            createSixTaskListForTests(manager);

            manager.deleteTaskById(task1.getId());
            List<Task> savedTask = loadFromFile(tempFilePath);
            List<Task> inMemoryTask = manager.getAllTasks();
            checkTasksUnchangedCustom(savedTask, inMemoryTask);

            manager.deleteTaskById(subTask3.getId());
            savedTask = loadFromFile(tempFilePath);
            inMemoryTask = manager.getAllTasks();
            checkTasksUnchangedCustom(savedTask, inMemoryTask);

            manager.deleteTaskById(epic1.getId());
            savedTask = loadFromFile(tempFilePath);
            inMemoryTask = manager.getAllTasks();
            checkTasksUnchangedCustom(savedTask, inMemoryTask);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void saveWorksProperlyOnUpdateTask() {
        try {
            createThreeTaskListForTests(manager);

            Task task = new Task(task1.getId(), "TASK CHANGED", "DESCRIPTION CHANGED", Status.DONE);
            Epic epic = new Epic(epic1.getId(), "EPIC CHANGED", "DESCRIPTION CHANGED");
            SubTask subTask = new SubTask(subTask1.getId(), "SUBTASK CHANGED", "DESCRIPTION CHANGED",
                    Status.IN_PROGRESS, subTask1.getEpicId());

            manager.updateTask(task);
            List<String> savedData = readFromFile(tempFilePath);
            checkTask(savedData.getFirst(), manager.getTaskById(task1.getId()).orElseThrow());

            manager.updateEpic(epic);
            savedData = readFromFile(tempFilePath);
            checkTask(savedData.get(1), manager.getTaskById(epic1.getId()).orElseThrow());

            manager.updateSubTask(subTask);
            savedData = readFromFile(tempFilePath);
            checkTask(savedData.get(2), manager.getTaskById(subTask1.getId()).orElseThrow());
            checkTask(savedData.get(1), manager.getTaskById(epic1.getId()).orElseThrow());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ.
    private static List<Task> loadFromFile(Path path) throws IOException {
        List<String> tasksString;
        List<Task> tasks = new ArrayList<>();

        try {
            tasksString = readFromFile(path);
        } catch (IOException ex) {
            throw new IOException(("I/O error while accessing Task Manager save file at path: " + path), ex);
        }

        for (String value : tasksString) {
            tasks.add(fromString(value));
        }
        return tasks;
    }

    private static List<String> readFromFile(Path file) throws IOException {
        List<String> tasksString = new ArrayList<>();
        String dataStream;

        try {
            dataStream = Files.readString(file);
        } catch (IOException ex) {
            throw new IOException(("I/O error while accessing Task Manager save file at path: " + file), ex);
        }

        if (dataStream.isBlank()) {
            return tasksString;
        }

        String[] lines = dataStream.split("\\R");

        for (int i = 1; i < lines.length; i++) {
            tasksString.add(lines[i]);
        }
        return tasksString;
    }

    private static Task fromString(String value) {
        String[] taskFields = value.split(",");

        int id = Integer.parseInt(taskFields[0]);
        TaskType type = TaskType.valueOf(taskFields[1]);
        String name = taskFields[2];
        Status status = Status.valueOf(taskFields[3]);
        String description = taskFields[4];

        return switch (type) {
            case TASK -> new Task(id, name, description, status);
            case EPIC -> new Epic(id, name, description);
            case SUBTASK -> {
                int epicId = Integer.parseInt(taskFields[5]);
                yield new SubTask(id, name, description, status, epicId);
            }
        };
    }

    private void checkTask(String value, Task task) {
        String[] taskFields = value.split(",");

        int id = Integer.parseInt(taskFields[0]);
        TaskType type = TaskType.valueOf(taskFields[1]);
        String name = taskFields[2];
        Status status = Status.valueOf(taskFields[3]);
        String description = taskFields[4];

        assertEquals(id, task.getId(), "id задачи не совпадает.");
        assertEquals(name, task.getName(), "Имя задачи не совпадает.");
        assertEquals(status, task.getStatus(), "Статус задачи не совпадает.");
        assertEquals(description, task.getDescription(), "Описание задачи не совпадает.");

        switch (type) {
            case TASK -> assertEquals(Task.class, task.getClass(), "Ожидалась обычная задача.");
            case EPIC -> assertInstanceOf(Epic.class, task, "Ожидался эпик.");
            case SUBTASK -> {
                assertInstanceOf(SubTask.class, task, "Ожидалась подзадача.");
                int epicId = Integer.parseInt(taskFields[5]);
                SubTask subTask = (SubTask) task;
                assertEquals(epicId, subTask.getEpicId(), "epicId задачи не совпадает.");
            }
        }
    }
}
