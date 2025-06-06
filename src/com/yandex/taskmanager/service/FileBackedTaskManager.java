package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;

import java.util.List;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;

public class FileBackedTaskManager extends InMemoryTaskManager implements TaskManager {
    Path saveFilePath;

    public FileBackedTaskManager(Path saveFilePath) {
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
            System.out.println("I/O error while initialising Task Manager save-file at path: " + saveFilePath);
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
            System.out.println("TaskManager save file not found at path: " + saveFilePath);
        } catch (IOException ex) {
            System.out.println("I/O error while accessing Task Manager save file at path: " + saveFilePath);
        }
    }

    public static void main(String[] args) {
        FileBackedTaskManager manager = new FileBackedTaskManager(Path.of("FileBackedTaskManager.txt"));

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
    }
}

