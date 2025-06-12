package com.yandex.taskmanager.service;

import com.yandex.taskmanager.exceptions.ManagerLoadException;
import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;
import com.yandex.taskmanager.model.TaskType;

import com.yandex.taskmanager.exceptions.ManagerSaveException;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.ArrayList;

import java.io.File;
import java.io.FileWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;


public class FileBackedTaskManager extends InMemoryTaskManager implements TaskManager {
    private final Path saveFile;
    private static final String HEADER = "id,type,name,status,description,epic";

    public FileBackedTaskManager(Path saveFile) throws IOException {
        if (saveFile == null) {
            throw new IllegalArgumentException("Path provided to Task Manager is null.");
        }

        if (Files.exists(saveFile) && Files.isDirectory(saveFile)) {
            throw new IOException("Save file path points to a directory, not a file: " + saveFile);
        }

        if (!Files.exists(saveFile)) {
            try {
                createSaveFile(saveFile);
            } catch (IOException ex) {
                throw new IOException(("I/O error while creating Task Manager save file at path: "
                        + saveFile), ex);
            }
        }

        this.saveFile = saveFile;
    }

    @Override
    public void addTask(Task task) {
        super.addTask(task);
        save();
    }

    @Override
    public void addEpic(Epic epic) {
        super.addEpic(epic);
        save();
    }

    @Override
    public void addSubTask(SubTask subTask) {
        super.addSubTask(subTask);
        save();
    }

    @Override
    public void deleteAllTasks() {
        super.deleteAllTasks();
        save();
    }

    @Override
    public void deleteTaskById(int id) {
        super.deleteTaskById(id);
        save();
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void updateSubTask(SubTask subTask) {
        super.updateSubTask(subTask);
        save();
    }

    private void save() {
        List<Task> managerMemory = getAllTasks();

        try (FileWriter fileWriter = new FileWriter(saveFile.toFile(), StandardCharsets.UTF_8)) {
            fileWriter.write(HEADER);
            fileWriter.write(System.lineSeparator());

            for (Task task : managerMemory) {
                fileWriter.write(task.toString());
                fileWriter.write(System.lineSeparator());
            }
        } catch (FileNotFoundException ex) {
            throw new ManagerSaveException(("TaskManager save file not found at path: " + saveFile), ex);
        } catch (IOException ex) {
            throw new ManagerSaveException(("I/O error while accessing Task Manager save file at path: "
                    + saveFile), ex);
        }
    }

    public static FileBackedTaskManager loadFromFile(File file) {
        final Path loadPath = file.toPath();
        final FileBackedTaskManager taskManager;
        List<String> tasksString;
        int idCounter = 0;

        try {
            taskManager = new FileBackedTaskManager(loadPath);
            tasksString = readFromFile(loadPath);
        } catch (IOException ex) {
            throw new ManagerLoadException(("I/O error while accessing Task Manager load file at path: "
                    + loadPath), ex);
        }

        for (String value : tasksString) {
            Task task = fromString(value);
            taskManager.restoreTasks(task);
            if (task.getId() > idCounter) {
                idCounter = task.getId();
            }
        }

        taskManager.idCounter = idCounter;

        return taskManager;
    }

    private void restoreTasks(Task task) {
        if (task instanceof Epic epic) {
            epics.put(epic.getId(), epic);
        } else if (task instanceof SubTask subTask) {
            Epic epic = epics.get(subTask.getEpicId());

            subTasks.put(subTask.getId(), subTask);
            addSubTaskIdToEpic(epic, subTask.getId());
            updateEpicStatus(epic.getId());
        } else {
            tasks.put(task.getId(), task);
        }
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

        String[] lines = dataStream.split(System.lineSeparator());

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

    private static void createSaveFile(Path file) throws IOException {
        Path parent = file.getParent();

        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException ex) {
                throw new IOException(("I/O error while creating directory for Task Manager save file at path: "
                        + file), ex);
            }
        }

        try {
            Files.createFile(file);
        } catch (IOException ex) {
            throw new IOException(("I/O error while creating Task Manager save file at path: "
                    + file), ex);
        }
    }

    public static void main(String[] args) {
        Path path = Paths.get("FileBackedTaskManager.txt");

        TaskManager manager = Managers.getFileBackedTaskManager(path);

        Task testTask1 = new Task("Task1", "Description task1");
        Task testTask2 = new Task("Task2", "Description task2");
        Epic testEpic1 = new Epic("Epic1", "Description epic1");
        Epic testEpic2 = new Epic("Epic2", "Description epic2");

        SubTask testSubTask1 = new SubTask("SubTask1", "Description subTask1", 3);
        SubTask testSubTask2 = new SubTask("SubTask2", "Description subTask2", 3);
        SubTask testSubTask3 = new SubTask("SubTask3", "Description subTask3", 3);
        SubTask testSubTask4 = new SubTask("SubTask4", "Description subTask4", 4);

        manager.addTask(testTask1);
        manager.addTask(testTask2);
        manager.addEpic(testEpic1);
        manager.addEpic(testEpic2);
        manager.addSubTask(testSubTask1);
        manager.addSubTask(testSubTask2);
        manager.addSubTask(testSubTask3);
        manager.addSubTask(testSubTask4);

        System.out.println(manager.getAllTasks());

        manager.deleteTaskById(1);
        manager.deleteTaskById(8);

        TaskManager newManager = FileBackedTaskManager.loadFromFile(path.toFile());

        System.out.println(newManager.getAllTasks());

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

