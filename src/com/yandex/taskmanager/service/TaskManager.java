package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.*;
import static com.yandex.taskmanager.service.TaskManagerStatus.*;
import java.util.ArrayList;
import java.util.HashMap;

public class TaskManager {
    static int taskCount = 0;
    private TaskManagerStatus taskManagerStatus = DEFAULT;
    private final HashMap<Integer, Task> taskList = new HashMap<>();
    private final HashMap<Integer, Epic> epicList = new HashMap<>();
    private final HashMap<Integer, SubTask> subTaskList = new HashMap<>();

    public void addNewTask(Task task) {
        if (task != null && task.getId() == 0) {
            if (task instanceof SubTask subTask) {
                Epic epic = epicList.get(subTask.getEpicId());

                if (epic != null) {
                    addToTaskList(task);
                    subTaskList.put(subTask.getId(), subTask);
                    epic.addSubTaskId(subTask.getId());
                    resetEpicStatus(subTask.getEpicId());
                } else {
                    taskManagerStatus = WRONG_EPIC_ID;
                }
            } else if (task instanceof Epic epic) {
                addToTaskList(task);
                epicList.put(epic.getId(), epic);
            } else {
                addToTaskList(task);
            }
        } else if (task != null) {
            taskManagerStatus = WRONG_ID;
        } else {
            taskManagerStatus = NULL;
        }
    }

    private void addToTaskList(Task task) {
        taskCount++;
        task.setId(taskCount);
        taskList.put(task.getId(), task);
    }

    public ArrayList<Task> getAllTasks() {
        return new ArrayList<>(taskList.values());
    }

    public ArrayList<Epic> getAllEpics() {
        return new ArrayList<>(epicList.values());
    }

    public ArrayList<SubTask> getAllSubTasks() {
       return new ArrayList<>(subTaskList.values());
    }

    public void deleteAllTasks() {
        taskList.clear();
        epicList.clear();
        subTaskList.clear();
        taskCount = 0;
    }

    public Task getTaskById(int id) {
        if (taskList.containsKey(id)) {
            return taskList.get(id);
        } else {
            return new Task("", "");
        }
    }

    public ArrayList<SubTask> getEpicSubTasks(int id) {
        if (epicList.containsKey(id)) {
            Epic epic = epicList.get(id);
            ArrayList<Integer> subTaskIds = epic.getSubTaskIdList();
            ArrayList<SubTask> subTasks = new ArrayList<>();

            for (int subTaskId : subTaskIds) {
                SubTask subTask = subTaskList.get(subTaskId);
                subTasks.add(subTask);
            }
            return subTasks;
        } else {
            return new ArrayList<>();
        }
    }

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
            epic.getSubTaskIdList().remove((Integer) id);
            resetEpicStatus(epicId);
            subTaskList.remove(id);
            taskList.remove(id);

        } else if (taskList.containsKey(id)) {
            taskList.remove(id);

        } else {
            taskManagerStatus = WRONG_ID;
        }
    }

    private void resetEpicStatus(int id) {
        Epic epic = epicList.get(id);
        ArrayList<Integer> SubTaskIds = epic.getSubTaskIdList();

        if (SubTaskIds.isEmpty()) {
            epic.setStatus(Status.NEW);
        } else {
            boolean isNew = true;
            boolean isDone = true;

            for (int subTaskId : SubTaskIds) {
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

    public void updateTask(Task newTask) {

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

    private void updateEpic(Epic newEpic) {
        Epic oldEpic = epicList.get(newEpic.getId());

        if (oldEpic != null) {
            ArrayList<Integer> newIdList = newEpic.getSubTaskIdList();
            ArrayList<Integer> oldIdList = oldEpic.getSubTaskIdList();
            newIdList.addAll(oldIdList);

            epicList.put(newEpic.getId(), newEpic);
            resetEpicStatus(newEpic.getId());
            taskList.put(newEpic.getId(), newEpic);
        } else {
            taskManagerStatus = WRONG_ID;
        }
    }

    private void updateSubTask(SubTask newSubTask) {
        Epic epic = epicList.get(newSubTask.getEpicId());

        if (epic == null) {
            taskManagerStatus = WRONG_EPIC_ID;
            return;
        }
        ArrayList<Integer> checkIds = epic.getSubTaskIdList();

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

    public TaskManagerStatus getTaskManagerStatus() {
        TaskManagerStatus currentStatus = taskManagerStatus;
        taskManagerStatus = DEFAULT;
        return currentStatus;
    }

    public int getTaskCount() {
        return taskCount;
    }
}
