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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

import java.io.File;
import java.io.FileWriter;

import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FileBackedTaskManager extends InMemoryTaskManager implements TaskManager {
    private final File saveFile;
    private static final String HEADER = "id,type,name,status,description,epic,startTime,duration,endTime";

    public FileBackedTaskManager(File saveFile) throws IOException {
        if (saveFile == null) {
            throw new IllegalArgumentException("File provided to Task Manager is null.");
        }

        if (saveFile.exists()) {
            this.saveFile = saveFile;
        } else {
            throw new IOException("File provided to Task Manager does not exist: "
                    + saveFile);
        }
    }

    @Override
    public Task addTask(Task task) {
        Task newTask = super.addTask(task);
        save();
        return newTask;
    }

    @Override
    public Epic addEpic(Epic epic) {
        Epic newEpic = super.addEpic(epic);
        save();
        return newEpic;
    }

    @Override
    public SubTask addSubTask(SubTask subTask) {
        SubTask newSubTask = super.addSubTask(subTask);
        save();
        return newSubTask;
    }

    @Override
    public void deleteAllTasks() {
        super.deleteAllTasks();
        save();
    }

    @Override
    public void deleteAnyTaskById(int id) {
        super.deleteAnyTaskById(id);
        save();
    }

    @Override
    public void deleteTask(int id) {
        super.deleteTask(id);
        save();
    }

    @Override
    public void deleteEpic(int id) {
        super.deleteEpic(id);
        save();
    }

    @Override
    public void deleteSubTask(int id) {
        super.deleteSubTask(id);
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

        try (FileWriter fileWriter = new FileWriter(saveFile, StandardCharsets.UTF_8)) {
            fileWriter.write(HEADER);
            fileWriter.write(System.lineSeparator());

            for (Task task : managerMemory) {
                fileWriter.write(task.toString());
                fileWriter.write(System.lineSeparator());
            }
        } catch (FileNotFoundException ex) {
            throw new ManagerSaveException(("TaskManager save file not found at path: " + saveFile.toPath()), ex);
        } catch (IOException ex) {
            throw new ManagerSaveException(("I/O error while accessing Task Manager save file at path: "
                    + saveFile.toPath()), ex);
        }
    }

    public static FileBackedTaskManager loadFromFile(File file) {
        final FileBackedTaskManager taskManager;
        List<String> tasksString;
        int idCounter = 0;

        try {
            taskManager = new FileBackedTaskManager(file);
            tasksString = readFromFile(file);
        } catch (IOException ex) {
            throw new ManagerLoadException(("I/O error while accessing Task Manager load file at path: "
                    + file.toPath()), ex);
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
            subTasks.put(subTask.getId(), subTask);

            if (subTask.getEndTime() != null) {
                prioritizedTasks.add(subTask);
                markCalendarInterval(subTask);
            }

            Epic epic = epics.get(subTask.getEpicId());
            epic.addSubTaskId(subTask.getId());
            updateEpicStatus(epic.getId());
            setEpicTime(epic.getId());
        } else {
            tasks.put(task.getId(), task);

            if (task.getEndTime() != null) {
                prioritizedTasks.add(task);
                markCalendarInterval(task);
            }
        }
    }

    private static List<String> readFromFile(File file) throws IOException {
        List<String> tasksString = new ArrayList<>();
        String dataStream;

        try {
            dataStream = Files.readString(file.toPath());
        } catch (IOException ex) {
            throw new IOException(("I/O error while accessing Task Manager save file at path: " + file.toPath()), ex);
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
        LocalDateTime startTime = !"null".equals(taskFields[6])
                ? LocalDateTime.parse(taskFields[6], Task.DATE_TIME_FORMATTER) : null;
        Duration duration = !"null".equals(taskFields[7])
                ? Duration.ofMinutes(Long.parseLong(taskFields[7])) : null;

        return switch (type) {
            case TASK -> new Task(id, name, description, status, startTime, duration);
            case EPIC -> new Epic(id, name, description);
            case SUBTASK -> {
                int epicId = Integer.parseInt(taskFields[5]);
                yield new SubTask(id, name, description, status, epicId, startTime, duration);
            }
        };
    }

    public static void main(String[] args) {
        File file;
        LocalDateTime now = LocalDateTime.now();

        try {
            file = File.createTempFile("FileBackedTaskManager_", ".txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        TaskManager manager = Managers.getFileBackedTaskManager(file);

        Task testTask1 = new Task("Task1", "Description task1", now, Duration.ofMinutes(15));
        Task testTask2 = new Task("Task2", "Description task2");
        Epic testEpic1 = new Epic("Epic1", "Description epic1");
        Epic testEpic2 = new Epic("Epic2", "Description epic2");

        SubTask testSubTask1 = new SubTask("SubTask1", "Description subTask1", 3);
        SubTask testSubTask2 = new SubTask("SubTask2", "Description subTask2", 3, now.plusMinutes(30), Duration.ofMinutes(10));
        SubTask testSubTask3 = new SubTask("SubTask3", "Description subTask3", 3, now.plusMinutes(49), Duration.ofMinutes(15));
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

        TaskManager newManager = FileBackedTaskManager.loadFromFile(file);

        System.out.println(newManager.getAllTasks());

        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

