package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;
import com.yandex.taskmanager.model.TaskType;

import com.yandex.taskmanager.exceptions.ManagerSaveException;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.List;

import java.io.FileWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;


public class FileBackedTaskManager extends InMemoryTaskManager implements TaskManager {
    Path saveFilePath;

    public FileBackedTaskManager(Path saveFilePath) throws IOException {
        if (saveFilePath == null) {
            throw new IllegalArgumentException("Path provided to Task Manager is null.");
        }

        this.saveFilePath = saveFilePath;

        try {
            Path parent = saveFilePath.getParent();
            if (parent != null && Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            if (Files.isDirectory(saveFilePath)) {
                throw new IOException("Save file path points to a directory, not a file: " + saveFilePath);
            } else if (!Files.exists(saveFilePath)) {
                Files.createFile(saveFilePath);
            }
        } catch (IOException ex) {
            throw new IOException(("I/O error while initialising Task Manager save-file at path: "
                    + saveFilePath), ex);
        }

        try {
            restoreTasks(loadTasks(this.saveFilePath));
        } catch (IOException ex) {
            throw new IOException(("I/O error while reading from Task Manager save-file at path: "
                    + saveFilePath), ex);
        }
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

        try (FileWriter fileWriter = new FileWriter(saveFilePath.toFile(), StandardCharsets.UTF_8)) {
            String header = "id,type,name,status,description,epic";

            fileWriter.write(header);
            fileWriter.write(System.lineSeparator());

            for (Task task : managerMemory) {
                fileWriter.write(task.toString());
                fileWriter.write(System.lineSeparator());
            }
        } catch (FileNotFoundException ex) {
            throw new ManagerSaveException(("TaskManager save file not found at path: " + saveFilePath), ex);
        } catch (IOException ex) {
            throw new ManagerSaveException(("I/O error while accessing Task Manager save file at path: "
                    + saveFilePath), ex);
        }
    }

    private void restoreTasks(List<Task> tasks) {
        for (Task task : tasks) {
            if (task instanceof Epic epic) {
                insertEpic(epic);
            } else if (task instanceof SubTask subTask) {
                insertSubTask(subTask);
            } else {
                insertTask(task);
            }
        }
    }

    private List<Task> loadTasks(Path saveFilePath) throws IOException {
        List<String> tasksString;
        List<Task> tasks = new ArrayList<>();

        try {
            tasksString = readTasks(saveFilePath);
        } catch (IOException ex) {
            throw new IOException(("I/O error while accessing Task Manager save file at path: " + saveFilePath), ex);
        }

        if (!tasksString.isEmpty()) {
            for (String value : tasksString) {
                tasks.add(fromString(value));
            }
        }
        return tasks;
    }

    private List<String> readTasks(Path saveFilePath) throws IOException {
        List<String> tasksString = new ArrayList<>();

        try {
            String dataStream = Files.readString(saveFilePath);
            String[] lines = dataStream.split("\\R");

            for (int i = 1; i < lines.length; i++) {
                tasksString.add(lines[i]);
            }
        } catch (IOException ex) {
            throw new IOException(("I/O error while accessing Task Manager save file at path: " + saveFilePath), ex);
        }
        return tasksString;
    }

    private Task fromString(String value) {
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

    public static void main(String[] args) {
        /*try {
            FileBackedTaskManager manager = new FileBackedTaskManager(Paths.get("FileBackedManager.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/

        TaskManager manager = Managers.getFileBackedTaskManager(Paths.get("FileBackedManager.txt"));

        /*Task testTask1 = new Task("Task1", "Description task1");
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
        manager.addSubTask(testSubTask4);*/

        System.out.println(manager.getTasks());
        System.out.println(manager.getEpics());
        System.out.println(manager.getSubTasks());
        System.out.println(manager.getIdCounter());

        manager.deleteTaskById(3);
        System.out.println(manager.getAllTasks());
        System.out.println(manager.getIdCounter());
    }
}

