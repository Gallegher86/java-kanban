package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class InMemoryTaskManager implements TaskManager {
    private int taskCount = 0;
    private final HashMap<Integer, Task> tasks = new HashMap<>();
    private final HashMap<Integer, Epic> epics = new HashMap<>();
    private final HashMap<Integer, SubTask> subTasks = new HashMap<>();
    private final HistoryManager historyManager = Managers.getDefaultHistoryManager();

    @Override
    public void addTask(Task task) {
        if (task instanceof Epic || task instanceof SubTask) {
            throw new IllegalArgumentException("Для добавления Epic/SubTask используются отдельные методы.");
        }

        if (isTaskOkToAdd(task)) {
            setTaskId(task);
            addToTasks(task);
        }
    }

    @Override
    public void addEpic(Epic epic) {
        if (isTaskOkToAdd(epic)) {
            if (!epic.getSubTaskIdList().isEmpty()) {
                throw new IllegalArgumentException("В переданном эпике заполнен subTaskIdList, эпик не является новым.");
            } else {
                setTaskId(epic);
                addToEpics(epic);
            }
        }
    }

    @Override
    public void addSubTask(SubTask subTask) {
        if (isTaskOkToAdd(subTask)) {
            Epic epic = epics.get(subTask.getEpicId());

            if (epic == null) {
                throw new IllegalArgumentException("EpicID: " + subTask.getEpicId() + " подзадачи нет в TaskManager.");
            } else {
                setTaskId(subTask);
                addToSubTasks(subTask);
                epic.addSubTaskId(subTask.getId());
                resetEpicStatus(subTask.getEpicId());
            }
        }
    }

    @Override
    public List<Task> getAllTasks() {
        List<Task> allTasks = new ArrayList<>();
        allTasks.addAll(tasks.values());
        allTasks.addAll(epics.values());
        allTasks.addAll(subTasks.values());
        return allTasks;
    }

    @Override
    public List<Task> getTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public List<Epic> getEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public List<SubTask> getSubTasks() {
        return new ArrayList<>(subTasks.values());
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    @Override
    public void clearHistory() {
        historyManager.clearHistory();
    }

    @Override
    public void deleteAllTasks() {
        tasks.clear();
        epics.clear();
        subTasks.clear();
        historyManager.clearHistory();
        taskCount = 0;
    }

    @Override
    public Optional<Task> getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.addHistory(task);
            return Optional.of(task);
        }

        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.addHistory(epic);
            return Optional.of(epic);
        }

        SubTask subTask = subTasks.get(id);
        if (subTask != null) {
            historyManager.addHistory(subTask);
            return Optional.of(subTask);
        }

        return Optional.empty();
    }

    @Override
    public List<SubTask> getEpicSubTasks(int id) {
        if (epics.containsKey(id)) {
            Epic epic = epics.get(id);

            List<Integer> subTaskIds = epic.getSubTaskIdList();
            List<SubTask> subTasks = new ArrayList<>();

            for (int subTaskId : subTaskIds) {
                SubTask subTask = this.subTasks.get(subTaskId);
                subTasks.add(subTask);
            }
            return subTasks;
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public void deleteTaskById(int id) {
        if (tasks.containsKey(id)) {
            tasks.remove(id);
        } else if (epics.containsKey(id)) {
            deleteEpicById(id);
        } else if (subTasks.containsKey(id)) {
            deleteSubTaskById(id);
        } else {
            throw new IllegalArgumentException("Задача с ID: " + id + " не найдена в списке TaskManager.");
        }
    }

    @Override
    public void updateTask(Task task) {
        if (task instanceof Epic || task instanceof SubTask) {
            throw new IllegalArgumentException("Для обновления Epic/SubTask используются отдельные методы.");
        }

        if (isTaskOkToUpdate(task)) {
            addToTasks(task);
        }
    }

    @Override
    public void updateEpic(Epic epic) {
        if (isTaskOkToUpdate(epic)) {
            if (!epic.getSubTaskIdList().isEmpty()) {
                throw new IllegalArgumentException("В переданном эпике заполнен subTaskIdList, возможен конфликт с менеджером.");
            }
            int epicId = epic.getId();

            epic.setSubTaskIdList(epics.get(epicId).getSubTaskIdList());
            addToEpics(epic);
            resetEpicStatus(epicId);
        }
    }

    @Override
    public void updateSubTask(SubTask subTask) {
        if (isTaskOkToUpdate(subTask)) {
            int subTaskId = subTask.getId();
            int epicId = subTask.getEpicId();
            Epic epic = epics.get(epicId);

            if (epic == null) {
                throw new IllegalArgumentException("EpicId: " + epicId + ", переданной подзадачи с ID: "
                        + subTaskId + " нет в списке TaskManager.");
            }

            List<Integer> checkIds = epic.getSubTaskIdList();
            if (checkIds.contains(subTaskId)) {
                addToSubTasks(subTask);
                resetEpicStatus(epicId);
            } else {
                throw new IllegalArgumentException("В списке эпика: " + epicId + ", нет переданной подзадачи " +
                        "с ID: " + subTaskId + ".");
            }
        }
    }

    private boolean isTaskOkToAdd(Task task) {
        checkTaskNotNull(task);

        if (task.getId() == 0 && task.getStatus() == Status.NEW) {
            return true;
        } else {
            throw new IllegalArgumentException("Задача с ID: " + task.getId() + " и статусом: " + task.getStatus() +
                    ", не является новой.");
        }
    }

    private boolean isTaskOkToUpdate(Task task) {
        checkTaskNotNull(task);

        if (!isTaskInManager(task.getId())) {
            throw new IllegalArgumentException("Задача с ID: " + task.getId() + " не найдена в списке TaskManager.");
        } else {
            return true;
        }
    }

    private boolean isTaskInManager(int id) {
        return (tasks.containsKey(id) || epics.containsKey(id) || subTasks.containsKey(id));
    }

    private void checkTaskNotNull(Task task) {
        if (task == null) {
            throw new NullPointerException("Переданная задача является null.");
        }
    }

    private void setTaskId(Task task) {
        taskCount++;
        task.setId(taskCount);
    }

    private void addToTasks(Task task) {
        tasks.put(task.getId(), task);
    }

    private void addToEpics(Epic epic) {
        epics.put(epic.getId(), epic);
    }

    private void addToSubTasks(SubTask subTask) {
        subTasks.put(subTask.getId(), subTask);
    }

    private void deleteEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            for (int subTaskId : epic.getSubTaskIdList()) {
                subTasks.remove(subTaskId);
            }
        }
        epics.remove(id);
    }

    private void deleteSubTaskById(int id) {
        SubTask subTask = subTasks.get(id);
        if (subTask != null) {
            int epicId = subTask.getEpicId();

            Epic subTaskEpic = epics.get(epicId);
            subTaskEpic.removeSubTaskId(id);
            resetEpicStatus(epicId);
        }
        subTasks.remove(id);
    }

    private void resetEpicStatus(int id) {
        Epic epic = epics.get(id);
        List<Integer> subTaskIds = epic.getSubTaskIdList();

        if (subTaskIds.isEmpty()) {
            epic.setStatus(Status.NEW);
        } else {
            boolean isNew = true;
            boolean isDone = true;

            for (int subTaskId : subTaskIds) {
                SubTask subTask = subTasks.get(subTaskId);
                Status status = subTask.getStatus();

                if (status != Status.NEW) {
                    isNew = false;
                }
                if (status != Status.DONE) {
                    isDone = false;
                }
            }

            if (isNew) {
                epic.setStatus(Status.NEW);
            } else if (isDone) {
                epic.setStatus(Status.DONE);
            } else {
                epic.setStatus(Status.IN_PROGRESS);
            }
        }
    }

    @Override
    public int getTaskCount() {
        return taskCount;
    }
}
