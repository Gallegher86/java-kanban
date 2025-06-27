package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;

import java.util.List;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.Optional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Duration;

import java.util.function.Function;

public class InMemoryTaskManager implements TaskManager {
    private static final int CALENDAR_INTERVAL = 15;
    private static final int CALENDAR_YEARS = 1;
    protected int idCounter = 0;
    protected final HashMap<Integer, Task> tasks = new HashMap<>();
    protected final HashMap<Integer, Epic> epics = new HashMap<>();
    protected final HashMap<Integer, SubTask> subTasks = new HashMap<>();
    protected final TreeSet<Task> prioritizedTasks = new TreeSet<>(Comparator.comparing(Task::getStartTime));
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

    public TreeSet<Task> getPrioritizedTasks() {
        return new TreeSet<>(prioritizedTasks);
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
        prioritizedTasks.clear();
        calendar.replaceAll((k, v) -> Boolean.TRUE);
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
        Epic epic = epics.get(id);

        if (epic == null) {
            return List.of();
        }

        return epic.getSubTaskIdList().stream()
                .map(subTasks::get)
                .toList();
    }

    @Override
    public void deleteTaskById(int id) {
        if (tasks.containsKey(id)) {
            removeFromPrioritizedTasks(tasks.get(id));
            historyManager.remove(id);
            tasks.remove(id);
        } else if (epics.containsKey(id)) {
            deleteEpicById(id);
        } else if (subTasks.containsKey(id)) {
            removeFromPrioritizedTasks(subTasks.get(id));
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
            updatePrioritizedTasks(task, tasks.get(task.getId()));
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
            setEpicTime(epicId);
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
                updatePrioritizedTasks(subTask, subTasks.get(subTaskId));
                subTasks.put(subTaskId, subTask);
                updateEpicStatus(epicId);
                setEpicTime(epicId);
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
        Task newTask = new Task(idCounter, task.getName(), task.getDescription(), task.getStartTime(), task.getDuration());
        tasks.put(newTask.getId(), newTask);
        addToPrioritizedTasks(newTask);
    }

    private void addToEpics(Epic epic) {
        idCounter++;
        Epic newEpic = new Epic(idCounter, epic.getName(), epic.getDescription());
        epics.put(newEpic.getId(), newEpic);
        setEpicTime(newEpic.getId());
    }

    private void addToSubTasks(SubTask subTask) {
        idCounter++;
        SubTask newSubTask = new SubTask(idCounter, subTask.getName(), subTask.getDescription(), subTask.getEpicId(),
                subTask.getStartTime(), subTask.getDuration());
        subTasks.put(newSubTask.getId(), newSubTask);
        addToPrioritizedTasks(newSubTask);

        Epic epic = epics.get(newSubTask.getEpicId());
        epic.addSubTaskId(newSubTask.getId());
        updateEpicStatus(epic.getId());
        setEpicTime(epic.getId());
    }

    private void deleteEpicById(int id) {
        Epic epic = epics.get(id);

        if (epic == null) {
            return;
        }

        epic.getSubTaskIdList().forEach(subTaskId -> {
            subTasks.remove(subTaskId);
            historyManager.remove(subTaskId);
        });
        historyManager.remove(id);
        epics.remove(id);
    }

    private void deleteSubTaskById(int id) {
        SubTask subTask = subTasks.get(id);

        if (subTask == null) {
            return;
        }

        Epic epic = epics.get(subTask.getEpicId());
        epic.removeSubTaskId(subTask.getId());
        updateEpicStatus(epic.getId());
        setEpicTime(epic.getId());
        historyManager.remove(id);
        subTasks.remove(id);
    }

    protected void updateEpicStatus(int id) {
        Epic epic = epics.get(id);

        List<SubTask> epicSubTasks = epic.getSubTaskIdList().stream()
                .map(subTasks::get)
                .toList();

        if (epicSubTasks.isEmpty()) {
            epic.setStatus(Status.NEW);
        } else if (epicSubTasks.stream().allMatch(subTask -> subTask.getStatus() == Status.NEW)) {
            epic.setStatus(Status.NEW);
        } else if (epicSubTasks.stream().allMatch(subTask -> subTask.getStatus() == Status.DONE)) {
            epic.setStatus(Status.DONE);
        } else {
            epic.setStatus(Status.IN_PROGRESS);
        }
    }

    private void addToPrioritizedTasks(Task task) {
        if (task.getEndTime() == null) {
            return;
        }

        if (!isCalendarIntervalFree(task)) {
            throw new IllegalArgumentException("Cannot add Task - task interval is occupied");
        }
        markCalendarInterval(task);
        prioritizedTasks.add(task);
    }

    private void removeFromPrioritizedTasks(Task task) {
        if (task.getEndTime() == null) {
            return;
        }

        freeCalendarInterval(task);
        prioritizedTasks.remove(task);
    }

    private void updatePrioritizedTasks(Task newTask, Task oldTask) {
        if (newTask.getEndTime() == null) {
            if (prioritizedTasks.contains(oldTask)) {
                freeCalendarInterval(oldTask);
                prioritizedTasks.remove(oldTask);
            }
            return;
        }

        freeCalendarInterval(oldTask);
        if (isCalendarIntervalFree(newTask)) {
            prioritizedTasks.remove(oldTask);
            markCalendarInterval(newTask);
            prioritizedTasks.add(newTask);
        } else {
            markCalendarInterval(oldTask);
            throw new IllegalArgumentException("Cannot update Task - task interval is occupied");
        }
    }

    protected void setEpicTime(int id) {
        Epic epic = epics.get(id);

        List<SubTask> epicSubTasks = epic.getSubTaskIdList().stream()
                .map(subTasks::get)
                .filter(subTask -> subTask.getEndTime() != null)
                .toList();

        if (epicSubTasks.isEmpty()) {
            epic.setEpicTime(null, null, null);
            return;
        }

        LocalDateTime startTime = epicSubTasks.stream()
                .map(SubTask::getStartTime)
                .min(LocalDateTime::compareTo)
                .orElseThrow();

        LocalDateTime endTime = epicSubTasks.stream()
                .map(SubTask::getEndTime)
                .max(LocalDateTime::compareTo)
                .orElseThrow();

        Duration duration = epicSubTasks.stream()
                .map(SubTask::getDuration)
                .reduce(Duration.ZERO, Duration::plus);

        epic.setEpicTime(startTime, duration, endTime);
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

    protected void markCalendarInterval(Task task) {
        LocalDateTime roundedStartTime = roundDownTime.apply(task.getStartTime());
        LocalDateTime roundedEndTime = roundUpTime.apply(task.getEndTime());

        calendar.subMap(roundedStartTime, roundedEndTime)
                .replaceAll((k, v) -> Boolean.FALSE);
    }

    private void freeCalendarInterval(Task task) {
        LocalDateTime roundedStartTime = roundDownTime.apply(task.getStartTime());
        LocalDateTime roundedEndTime = roundUpTime.apply(task.getEndTime());

        calendar.subMap(roundedStartTime, roundedEndTime)
                .replaceAll((k, v) -> Boolean.TRUE);
    }

    private boolean isCalendarIntervalFree(Task task) {
        LocalDateTime roundedStartTime = roundDownTime.apply(task.getStartTime());
        LocalDateTime roundedEndTime = roundUpTime.apply(task.getEndTime());

        if (!calendar.containsKey(roundedStartTime) || !calendar.containsKey(roundedEndTime)) {
            throw new IllegalArgumentException("Task time is outside of calendar range.");
        }

        SortedMap<LocalDateTime, Boolean> taskInterval = calendar.subMap(roundedStartTime, roundedEndTime);

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
