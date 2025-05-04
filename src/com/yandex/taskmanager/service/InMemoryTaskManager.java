package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.*;
import static com.yandex.taskmanager.service.TaskManagerStatus.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class InMemoryTaskManager implements TaskManager {
    private int taskCount = 0;
    private TaskManagerStatus taskManagerStatus = DEFAULT;
    private final HashMap<Integer, Task> taskList = new HashMap<>();
    private final HashMap<Integer, Epic> epicList = new HashMap<>();
    private final HashMap<Integer, SubTask> subTaskList = new HashMap<>();
    private final HistoryManager historyManager = Managers.getDefaultHistoryManager();

    @Override
    public void addNewTask(Task task) {
        if (task != null && task.getId() == 0 && task.getStatus() == Status.NEW) {
            if (task instanceof SubTask subTask) {
                addSubTask(subTask);
            } else if (task instanceof Epic epic) {
                addEpic(epic);
            } else {
                addToTaskList(task);
            }
        } else if (task != null) {
            taskManagerStatus = NOT_NEW_TASK;
        } else {
            taskManagerStatus = NULL;
        }
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(taskList.values());
    }

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epicList.values());
    }

    @Override
    public List<SubTask> getAllSubTasks() {
       return new ArrayList<>(subTaskList.values());
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
        taskList.clear();
        epicList.clear();
        subTaskList.clear();
        historyManager.clearHistory();
        taskCount = 0;
    }

    @Override
    public Optional<Task> getTaskById(int id) {
        if (taskList.containsKey(id)) {
            historyManager.addHistory(taskList.get(id));
            return Optional.of(taskList.get(id));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public List<SubTask> getEpicSubTasks(int id) {
        if (epicList.containsKey(id)) {
            Epic epic = epicList.get(id);
            List<Integer> subTaskIds = epic.getSubTaskIdList();
            List<SubTask> subTasks = new ArrayList<>();

            for (int subTaskId : subTaskIds) {
                SubTask subTask = subTaskList.get(subTaskId);
                subTasks.add(subTask);
            }
            return subTasks;
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public void deleteTaskById(int id) {
        if (epicList.containsKey(id)) {
            Epic epic = epicList.get(id);

            for (int subTaskId : epic.getSubTaskIdList()) {
                subTaskList.remove(subTaskId);
                taskList.remove(subTaskId);
            }
            epicList.remove(id);
            taskList.remove(id);

        } else if (subTaskList.containsKey(id)) {
            SubTask subTask = subTaskList.get(id);
            int epicId = subTask.getEpicId();

            Epic epic = epicList.get(epicId);
            epic.removeSubTaskId(id);
            resetEpicStatus(epicId);
            subTaskList.remove(id);
            taskList.remove(id);

        } else if (taskList.containsKey(id)) {
            taskList.remove(id);

        } else {
            taskManagerStatus = WRONG_ID;
        }
    }

    @Override
    public void updateTask(Task newTask) {
        if (newTask == null) {
            taskManagerStatus = NULL;
            return;
        }

        Task existingTask = taskList.get(newTask.getId());

        if (existingTask != null && existingTask.getClass() == newTask.getClass()) {
            if (newTask instanceof SubTask newSubTask) {
                updateSubTask(newSubTask);
            } else if (newTask instanceof Epic newEpic) {
                updateEpic(newEpic);
            } else {
                updateGenericTask(newTask);
            }
        } else {
            if (existingTask != null) {
                taskManagerStatus = WRONG_CLASS;
            } else {
                taskManagerStatus = WRONG_ID;
            }
        }
    }

    private void addToTaskList(Task task) {
        taskCount++;
        task.setId(taskCount);
        taskList.put(task.getId(), task);
    }

    private void addEpic(Epic epic) {
        if (!epic.getSubTaskIdList().isEmpty()) {
            taskManagerStatus = NOT_NEW_TASK;
        } else {
            addToTaskList(epic);
            epicList.put(epic.getId(), epic);
        }
    }

    private void addSubTask(SubTask subTask) {
        Epic epic = epicList.get(subTask.getEpicId());

        if (epic != null) {
            addToTaskList(subTask);
            subTaskList.put(subTask.getId(), subTask);
            epic.addSubTaskId(subTask.getId());
            resetEpicStatus(subTask.getEpicId());
        } else {
            taskManagerStatus = WRONG_EPIC_ID;
        }
    }

    private void updateEpic(Epic newEpic) {
        newEpic.setSubTaskIdList(epicList.get(newEpic.getId()).getSubTaskIdList());
        epicList.put(newEpic.getId(), newEpic);
        resetEpicStatus(newEpic.getId());
        taskList.put(newEpic.getId(), newEpic);
    }

    private void updateSubTask(SubTask newSubTask) {
        Epic epic = epicList.get(newSubTask.getEpicId());

        if (epic == null) {
            taskManagerStatus = WRONG_EPIC_ID;
            return;
        }
        List<Integer> checkIds = epic.getSubTaskIdList();

        if (checkIds.contains(newSubTask.getId())) {
            subTaskList.put(newSubTask.getId(), newSubTask);
            taskList.put(newSubTask.getId(), newSubTask);
            resetEpicStatus(newSubTask.getEpicId());
        } else {
            taskManagerStatus = WRONG_EPIC_ID;
        }
    }

    private void updateGenericTask(Task newTask) {
        taskList.put(newTask.getId(), newTask);
    }

    private void resetEpicStatus(int id) {
        Epic epic = epicList.get(id);
        List<Integer> subTaskIds = epic.getSubTaskIdList();

        if (subTaskIds.isEmpty()) {
            epic.setStatus(Status.NEW);
        } else {
            boolean isNew = true;
            boolean isDone = true;

            for (int subTaskId : subTaskIds) {
                SubTask subTask = subTaskList.get(subTaskId);
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
    public TaskManagerStatus getTaskManagerStatus() {
        TaskManagerStatus currentStatus = taskManagerStatus;
        taskManagerStatus = DEFAULT;
        return currentStatus;
    }

    @Override
    public int getTaskCount() {
        return taskCount;
    }
}
