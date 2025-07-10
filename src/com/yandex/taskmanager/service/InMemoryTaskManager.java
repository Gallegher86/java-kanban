package com.yandex.taskmanager.service;

import com.yandex.taskmanager.exceptions.NotFoundException;
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
    public Task createTask(Task task) {
        if (task instanceof Epic || task instanceof SubTask) {
            throw new IllegalArgumentException("Epics and SubTasks must be added using their own methods.");
        }

        checkTaskOkToAdd(task);
        return addToTasks(task);
    }

    @Override
    public Epic createEpic(Epic epic) {
        checkTaskOkToAdd(epic);

        if (!epic.getSubTaskIdList().isEmpty()) {
            throw new IllegalArgumentException("Cannot add Epic with non-empty subTaskIdList. Only new Epics " +
                    "are allowed.");
        }
        return addToEpics(epic);
    }

    @Override
    public SubTask createSubTask(SubTask subTask) {
        checkTaskOkToAdd(subTask);

        Epic epic = epics.get(subTask.getEpicId());
        if (epic == null) {
            throw new NotFoundException("Cannot add SubTask. Epic with ID: " + subTask.getEpicId()
                    + " not found in TaskManager.");
        }
        return addToSubTasks(subTask);
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
    public Optional<Task> findAnyTaskById(int id) {
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
    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
            return task;
        } else {
            throw new NotFoundException("Task with id: " + id + " not found in TaskManager.");
        }
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
            return epic;
        } else {
            throw new NotFoundException("Epic with id: " + id + " not found in TaskManager.");
        }
    }

    @Override
    public SubTask getSubTaskById(int id) {
        SubTask subTask = subTasks.get(id);
        if (subTask != null) {
            historyManager.add(subTask);
            return subTask;
        } else {
            throw new NotFoundException("SubTask with id: " + id + " not found in TaskManager.");
        }
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
    public void deleteAnyTaskById(int id) {
        if (tasks.containsKey(id)) {
            deleteTask(id);
        } else if (epics.containsKey(id)) {
            deleteEpic(id);
        } else if (subTasks.containsKey(id)) {
            deleteSubTask(id);
        } else {
            throw new NotFoundException("Task with Id: " + id + " not found in TaskManager.");
        }
    }

    @Override
    public void deleteTask(int id) {
        Task task = tasks.get(id);

        if (task == null) {
            throw new NotFoundException("Task with Id: " + id + " not found in TaskManager.");
        }

        removeFromPrioritizedTasks(tasks.get(id));
        historyManager.remove(id);
        tasks.remove(id);
    }

    @Override
    public void deleteEpic(int id) {
        Epic epic = epics.get(id);

        if (epic == null) {
            throw new NotFoundException("Epic with Id: " + id + " not found in TaskManager.");
        }

        epic.getSubTaskIdList().forEach(subTaskId -> {
            subTasks.remove(subTaskId);
            historyManager.remove(subTaskId);
        });
        historyManager.remove(id);
        epics.remove(id);
    }

    @Override
    public void deleteSubTask(int id) {
        SubTask subTask = subTasks.get(id);

        if (subTask == null) {
            throw new NotFoundException("Subtask with Id: " + id + " not found in TaskManager.");
        }

        removeFromPrioritizedTasks(subTasks.get(id));

        Epic epic = epics.get(subTask.getEpicId());
        epic.removeSubTaskId(subTask.getId());
        updateEpicStatus(epic.getId());
        setEpicTime(epic.getId());
        historyManager.remove(id);
        subTasks.remove(id);
    }

    @Override
    public void updateTask(Task task) {
        if (task instanceof Epic || task instanceof SubTask) {
            throw new IllegalArgumentException("Epics and SubTasks must be updated using their own methods.");
        }
        int id = task.getId();

        if (!tasks.containsKey(id)) {
            throw new NotFoundException("Cannot update Task. Task with Id: " + id +
                    " not found in TaskManager.");
        }
        updatePrioritizedTasks(task, tasks.get(task.getId()));
        tasks.put(task.getId(), task);
    }

    @Override
    public void updateEpic(Epic epic) {
        checkTaskDataCorrect(epic);
        int id = epic.getId();

        if (!epic.getSubTaskIdList().isEmpty()) {
            throw new IllegalArgumentException("Cannot update Epic: subTaskIdList must be empty to avoid " +
                    "conflicts with TaskManager.");
        }

        if (!epics.containsKey(id)) {
            throw new NotFoundException("Cannot update Epic. Epic with Id: " + id +
                    " not found in TaskManager.");
        }

        Epic oldEpic = epics.get(id);
        Epic updatedEpic = new Epic(epic, oldEpic.getSubTaskIdList());
        epics.put(id, updatedEpic);
        updateEpicStatus(id);
        setEpicTime(id);
    }

    @Override
    public void updateSubTask(SubTask subTask) {
        checkTaskDataCorrect(subTask);
        int subTaskId = subTask.getId();
        int epicId = subTask.getEpicId();

        if (!subTasks.containsKey(subTaskId)) {
            throw new NotFoundException("Cannot update SubTask. SubTask with Id: " + subTaskId +
                    " not found in TaskManager.");
        }

        Epic epic = epics.get(epicId);
        if (epic == null) {
            throw new NotFoundException("Cannot update SubTask. EpicId: " + epicId + "of SubTask with Id: "
                    + subTaskId + " not found in TaskManager.");
        }

        List<Integer> checkIds = epic.getSubTaskIdList();
        if (checkIds.contains(subTaskId)) {
            updatePrioritizedTasks(subTask, subTasks.get(subTaskId));
            subTasks.put(subTaskId, subTask);
            updateEpicStatus(epicId);
            setEpicTime(epicId);
        } else {
            throw new NotFoundException("Cannot update SubTask. In Epic with EpicId: " + epicId +
                    " there is no SubTask with Id: " + subTaskId + ".");
        }
    }

    private void checkTaskOkToAdd(Task task) {
        checkTaskDataCorrect(task);

        if (task.getId() != 0 || task.getStatus() != Status.NEW) {
            throw new IllegalArgumentException("Cannot add Task. Task (Id: " + task.getId() + ", Status: " +
                    task.getStatus() + ") is not new. New Task must have id = 0 and status = NEW.");
        }
    }

    private void checkTaskDataCorrect(Task task) {
        List<String> errors = new ArrayList<>();

        if (task == null) {
            throw new NullPointerException("Task provided to Task Manager is null.");
        }

        if (task.getName() == null) {
            errors.add("Task provided to Task Manager has null name.");
        }
        if (task.getDescription() == null) {
            errors.add("Task provided to Task Manager has null description.");
        }
        if (task.getStatus() == null) {
            errors.add("Task provided to Task Manager has null status.");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
    }

    private Task addToTasks(Task task) {
        if (task.getEndTime() != null && !isCalendarIntervalFree(task)) {
            throw new IllegalArgumentException("Cannot add Task - task interval is occupied.");
        }

        idCounter++;
        Task newTask = new Task(idCounter, task.getName(), task.getDescription(), task.getStartTime(), task.getDuration());
        tasks.put(newTask.getId(), newTask);
        addToPrioritizedTasks(newTask);
        return newTask;
    }

    private Epic addToEpics(Epic epic) {
        idCounter++;
        Epic newEpic = new Epic(idCounter, epic.getName(), epic.getDescription());
        epics.put(newEpic.getId(), newEpic);
        setEpicTime(newEpic.getId());
        return newEpic;
    }

    private SubTask addToSubTasks(SubTask subTask) {
        if (subTask.getEndTime() != null && !isCalendarIntervalFree(subTask)) {
            throw new IllegalArgumentException("Cannot add SubTask - SubTask interval is occupied.");
        }

        idCounter++;
        SubTask newSubTask = new SubTask(idCounter, subTask.getName(), subTask.getDescription(), subTask.getEpicId(),
                subTask.getStartTime(), subTask.getDuration());
        subTasks.put(newSubTask.getId(), newSubTask);
        addToPrioritizedTasks(newSubTask);

        Epic epic = epics.get(newSubTask.getEpicId());
        epic.addSubTaskId(newSubTask.getId());
        updateEpicStatus(epic.getId());
        setEpicTime(epic.getId());

        return newSubTask;
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

        if (oldTask.getEndTime() == null) {
            if (isCalendarIntervalFree(newTask)) {
                markCalendarInterval(newTask);
                prioritizedTasks.add(newTask);
            } else {
                throw new IllegalArgumentException("Cannot update Task - task interval is occupied");
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
