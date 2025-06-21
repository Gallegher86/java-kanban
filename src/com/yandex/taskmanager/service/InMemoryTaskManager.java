package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;

import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.function.Function;

public class InMemoryTaskManager implements TaskManager {
    private static final int CALENDAR_INTERVAL = 15;
    private static final int CALENDAR_YEARS = 1;
    protected int idCounter = 0;
    protected final HashMap<Integer, Task> tasks = new HashMap<>();
    protected final HashMap<Integer, Epic> epics = new HashMap<>();
    protected final HashMap<Integer, SubTask> subTasks = new HashMap<>();
    protected final TreeSet<Task> prioritizedTasks = new TreeSet<>();
    protected final TreeMap<LocalDateTime, Boolean> calendar = initializeCalendar();
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

            for (int subTaskId : subTaskIds) {//добавить стрим
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
            tasks.put(task.getId(), task);
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
                subTasks.put(subTaskId, subTask);
                updateEpicStatus(epicId);
            } else {
                throw new IllegalArgumentException("Cannot update SubTask. In Epic with EpicId: " + epicId +
                        " there is no SubTask with Id: " + subTaskId + ".");
            }
        }
    }

    public TreeSet<Task> getPrioritizedTasks() {
        return new TreeSet<>(prioritizedTasks);
    }

    private void addTaskToPrioritizedTasks(Task task) {
        if (task.getEndTime() == null || task instanceof Epic) {
            return;
        }

        if (!IsIntervalFree(task.getStartTime(), task.getEndTime())) {
            throw new IllegalArgumentException("Cannot add Task - task interval is occupied");
        }

        prioritizedTasks.add(task);
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
        epic.addSubTaskId(newSubTask.getId());
        updateEpicStatus(epic.getId());
    }

    private void deleteEpicById(int id) {
        Epic epic = epics.get(id);//добавить стрим
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
            epic.removeSubTaskId(subTask.getId());
            updateEpicStatus(epic.getId());
        }
        historyManager.remove(id);
        subTasks.remove(id);
    }

    protected void updateEpicStatus(int id) {
        Epic epic = epics.get(id);
        List<Integer> subTaskIds = epic.getSubTaskIdList();

        if (subTaskIds.isEmpty()) {
            epic.setStatus(Status.NEW);
        } else {
            boolean isNew = true;
            boolean isDone = true;

            for (int subTaskId : subTaskIds) {//добавить стрим
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

    private TreeMap<LocalDateTime, Boolean> initializeCalendar() {
        TreeMap<LocalDateTime, Boolean> calendar = new TreeMap<>();
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusYears(CALENDAR_YEARS);

        while (!start.isAfter(end)) {
            calendar.put(start, true);
            start = start.plusMinutes(CALENDAR_INTERVAL);
        }
        return calendar;
    }

    private void markInterval(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime roundedStartTime = roundDownTime.apply(startTime);
        LocalDateTime roundedEndTime = roundUpTime.apply(endTime);

        calendar.subMap(roundedStartTime, true,
                        roundedEndTime, true)
                .replaceAll((k, v) -> false);
    }

    private void freeInterval(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime roundedStartTime = roundDownTime.apply(startTime);
        LocalDateTime roundedEndTime = roundUpTime.apply(endTime);

        calendar.subMap(roundedStartTime, true,
                        roundedEndTime, true)
                .replaceAll((k, v) -> true);
    }

    private boolean IsIntervalFree(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime roundedStartTime = roundDownTime.apply(startTime);
        LocalDateTime roundedEndTime = roundUpTime.apply(endTime);

        if (!calendar.containsKey(roundedStartTime) || !calendar.containsKey(roundedEndTime)) {
            throw new IllegalArgumentException("Task time is outside of calendar range.");
        }

        SortedMap<LocalDateTime, Boolean> taskInterval =
                calendar.subMap(roundedStartTime, true,
                        roundedEndTime, true);

        return taskInterval.values().stream().allMatch(Boolean::booleanValue);
    }

    private final Function<LocalDateTime, LocalDateTime> roundDownTime = dt -> {
        int minute = dt.getMinute();
        int rounded = (minute / CALENDAR_INTERVAL) * CALENDAR_INTERVAL;
        return dt.withMinute(rounded).withSecond(0).withNano(0);
    };

    private final Function<LocalDateTime, LocalDateTime> roundUpTime = dt -> {
        int minute = dt.getMinute();
        int mod = minute % CALENDAR_INTERVAL;
        int delta = (mod == 0) ? 0 : CALENDAR_INTERVAL - mod;
        return dt.plusMinutes(delta).withSecond(0).withNano(0);
    };

    @Override
    public int getIdCounter() {
        return idCounter;
    }
}
