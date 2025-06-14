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
    protected int idCounter = 0;
    protected final HashMap<Integer, Task> tasks = new HashMap<>();
    protected final HashMap<Integer, Epic> epics = new HashMap<>();
    protected final HashMap<Integer, SubTask> subTasks = new HashMap<>();
    private final HistoryManager historyManager = Managers.getDefaultHistoryManager();

    @Override
    public void addTask(Task task) {
        if (task instanceof Epic || task instanceof SubTask) {
            throw new IllegalArgumentException("Epics and SubTasks must be added using their own methods.");
        }

        if (isTaskOkToAdd(task)) {
            addToTasks(task);
        }
    }

    @Override
    public void addEpic(Epic epic) {
        if (isTaskOkToAdd(epic)) {
            if (!epic.getSubTaskIdList().isEmpty()) {
                throw new IllegalArgumentException("Cannot add Epic with non-empty subTaskIdList. Only new Epics " +
                        "are allowed.");
            } else {
                addToEpics(epic);
            }
        }
    }

    @Override
    public void addSubTask(SubTask subTask) {
        if (isTaskOkToAdd(subTask)) {
            Epic epic = epics.get(subTask.getEpicId());

            if (epic == null) {
                throw new IllegalArgumentException("Cannot add SubTask. Epic with ID: " + subTask.getEpicId()
                        + " not found in TaskManager.");
            } else {
                addToSubTasks(subTask);
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
        idCounter = 0;
    }

    @Override
    public Optional<Task> getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
            return Optional.of(task);
        }

        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
            return Optional.of(epic);
        }

        SubTask subTask = subTasks.get(id);
        if (subTask != null) {
            historyManager.add(subTask);
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
            historyManager.remove(id);
            tasks.remove(id);
        } else if (epics.containsKey(id)) {
            deleteEpicById(id);
        } else if (subTasks.containsKey(id)) {
            deleteSubTaskById(id);
        } else {
            throw new IllegalArgumentException("Task with Id: " + id + " not found in TaskManager.");
        }
    }

    @Override
    public void updateTask(Task task) {
        if (task instanceof Epic || task instanceof SubTask) {
            throw new IllegalArgumentException("Epics and SubTasks must be updated using their own methods.");
        }

        if (isTaskOkToUpdate(task)) {
            tasks.put(task.getId(), new Task(task));
        }
    }

    @Override
    public void updateEpic(Epic epic) {
        if (isTaskOkToUpdate(epic)) {
            if (!epic.getSubTaskIdList().isEmpty()) {
                throw new IllegalArgumentException("Cannot update Epic: subTaskIdList must be empty to avoid " +
                        "conflicts with TaskManager.");
            }
            int epicId = epic.getId();
            Epic oldEpic = epics.get(epicId);

            Epic updatedEpic = new Epic(epic, oldEpic.getSubTaskIdList());
            epics.put(epicId, updatedEpic);
            updateEpicStatus(epicId);
        }
    }

    @Override
    public void updateSubTask(SubTask subTask) {
        if (isTaskOkToUpdate(subTask)) {
            int subTaskId = subTask.getId();
            int epicId = subTask.getEpicId();
            Epic epic = epics.get(epicId);

            if (epic == null) {
                throw new IllegalArgumentException("Cannot update SubTask. EpicId: " + epicId + "of SubTask with Id: "
                        + subTaskId + " not found in TaskManager.");
            }

            List<Integer> checkIds = epic.getSubTaskIdList();
            if (checkIds.contains(subTaskId)) {
                subTasks.put(subTaskId, new SubTask(subTask));
                updateEpicStatus(epicId);
            } else {
                throw new IllegalArgumentException("Cannot update SubTask. In Epic with EpicId: " + epicId +
                        " there is no SubTask with Id: " + subTaskId + ".");
            }
        }
    }

    private boolean isTaskOkToAdd(Task task) {
        checkTaskNotNull(task);

        if (task.getId() == 0 && task.getStatus() == Status.NEW) {
            return true;
        } else {
            throw new IllegalArgumentException("Cannot add Task. Task (Id: " + task.getId() + ", Status: " +
                    task.getStatus() + ") is not new. New Task must have id = 0 and status = NEW.");
        }
    }

    private boolean isTaskOkToUpdate(Task task) {
        checkTaskNotNull(task);

        if (!isTaskInManager(task.getId())) {
            throw new IllegalArgumentException("Cannot update Task. Task with Id: " + task.getId() +
                    " not found in TaskManager.");
        } else {
            return true;
        }
    }

    private boolean isTaskInManager(int id) {
        return (tasks.containsKey(id) || epics.containsKey(id) || subTasks.containsKey(id));
    }

    private void checkTaskNotNull(Task task) {
        if (task == null) {
            throw new NullPointerException("Task provided to Task Manager is null.");
        }
    }

    private void addToTasks(Task task) {
        idCounter++;
        Task newTask = new Task(idCounter, task.getName(), task.getDescription());
        tasks.put(newTask.getId(), newTask);
    }

    private void addToEpics(Epic epic) {
        idCounter++;
        Epic newEpic = new Epic(idCounter, epic.getName(), epic.getDescription());
        epics.put(newEpic.getId(), newEpic);
    }

    private void addToSubTasks(SubTask subTask) {
        idCounter++;
        SubTask newSubTask = new SubTask(idCounter, subTask.getName(), subTask.getDescription(), subTask.getEpicId());
        subTasks.put(newSubTask.getId(), newSubTask);

        Epic epic = epics.get(newSubTask.getEpicId());
        addSubTaskIdToEpic(epic, newSubTask.getId());
        updateEpicStatus(epic.getId());
    }

    private void deleteEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            for (int subTaskId : epic.getSubTaskIdList()) {
                historyManager.remove(subTaskId);
                subTasks.remove(subTaskId);
            }
        }
        historyManager.remove(id);
        epics.remove(id);
    }

    private void deleteSubTaskById(int id) {
        SubTask subTask = subTasks.get(id);
        if (subTask != null) {
            Epic epic = epics.get(subTask.getEpicId());
            removeSubTaskIdFromEpic(epic, subTask.getId());
            updateEpicStatus(subTask.getEpicId());
        }
        historyManager.remove(id);
        subTasks.remove(id);
    }

    protected void updateEpicStatus(int id) {
        Epic epic = epics.get(id);
        List<Integer> subTaskIds = epic.getSubTaskIdList();

        if (subTaskIds.isEmpty()) {
            setEpicStatus(epic, Status.NEW);
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
                setEpicStatus(epic, Status.NEW);
            } else if (isDone) {
                setEpicStatus(epic, Status.DONE);
            } else {
                setEpicStatus(epic, Status.IN_PROGRESS);
            }
        }
    }

    private void addSubTaskIdToEpic(Epic epic, int subTaskId) {
        List<Integer> subTaskIds = epic.getSubTaskIdList();

        subTaskIds.add(subTaskId);
        Epic updatedEpic = new Epic(epic, subTaskIds);
        epics.put(updatedEpic.getId(), updatedEpic);
    }

    private void removeSubTaskIdFromEpic(Epic epic, int subTaskId) {
        List<Integer> subTaskIds = epic.getSubTaskIdList();

        subTaskIds.remove(Integer.valueOf(subTaskId));
        Epic updatedEpic = new Epic(epic, subTaskIds);
        epics.put(updatedEpic.getId(), updatedEpic);
    }

    private void setEpicStatus(Epic epic, Status status) {
        Epic updatedEpic = new Epic(epic, status);
        epics.put(updatedEpic.getId(), updatedEpic);
    }

    @Override
    public int getIdCounter() {
        return idCounter;
    }
}
