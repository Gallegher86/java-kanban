package com.yandex.taskmanager.web;

import com.yandex.taskmanager.service.Managers;
import com.yandex.taskmanager.service.TaskManager;

import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;
import com.yandex.taskmanager.model.TaskType;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.yandex.taskmanager.web.dto.TaskDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;

import java.lang.reflect.Type;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;

public class HttpTaskManagerTasksTest {
    TaskManager manager = Managers.getDefaultTaskManager();
    HttpTaskServer server = new HttpTaskServer(manager);
    Gson gson = server.getGson();
    HttpClient client = HttpClient.newHttpClient();
    LocalDateTime now;
    private static final Type TASK_DTO_LIST_TYPE = new TypeToken<List<TaskDto>>() {
    }.getType();

    Task task1;
    Task task2;
    Epic epic1;
    Epic epic2;
    SubTask subTask1;
    SubTask subTask2;
    SubTask subTask3;

    @BeforeEach
    public void setUp() {
        manager.deleteAllTasks();
        server.start();
        now = LocalDate.now().atStartOfDay();
    }

    @AfterEach
    public void shutDown() {
        server.stop();
    }

    //ТЕСТЫ /tasks
    @Test
    public void getTasksWorksCorrectly() throws IOException, InterruptedException {
        createSevenTaskList(manager);
        List<Task> tasksInManager = manager.getTasks();

        HttpResponse<String> response = sendRequest("GET", "/tasks/", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        List<TaskDto> taskDto = gson.fromJson(response.body(), TASK_DTO_LIST_TYPE);
        List<Task> tasksFromResponse = taskDto.stream()
                .map(TaskDto::toTask)
                .toList();

        checkTasksUnchangedCustom(tasksFromResponse, tasksInManager);
    }

    @Test
    public void getTaskByIdWorksCorrectly() throws IOException, InterruptedException {
        createThreeTaskListForTests(manager);

        HttpResponse<String> response = sendRequest("GET", "/tasks/1", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        TaskDto taskDto = gson.fromJson(response.body(), TaskDto.class);
        Task returnedTask = TaskDto.toTask(taskDto);

        checkTasksUnchangedCustom(List.of(task1), List.of(returnedTask));
    }

    @Test
    public void createTaskWorksCorrectly() throws IOException, InterruptedException {
        task1 = new Task("НОВАЯ ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ", now, Duration.ofMinutes(15));
        Task taskToCheck = new Task(1, "НОВАЯ ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ", now, Duration.ofMinutes(15));
        String taskJson = gson.toJson(TaskDto.fromTask(task1));

        HttpResponse<String> response = sendRequest("POST", "/tasks", taskJson);
        assertEquals(201, response.statusCode(), "Сервер должен возвращать код 201.");
        TaskDto taskDto = gson.fromJson(response.body(), TaskDto.class);
        Task returnedTask = TaskDto.toTask(taskDto);

        List<Task> tasksFromManager = manager.getTasks();

        assertNotNull(tasksFromManager, "Задачи не возвращаются");
        assertEquals(1, tasksFromManager.size(), "Некорректное количество задач");
        checkTasksUnchangedCustom(tasksFromManager, List.of(taskToCheck));
        checkTasksUnchangedCustom(tasksFromManager, List.of(returnedTask));
    }

    @Test
    public void updateTaskWorksCorrectly() throws IOException, InterruptedException {
        task1 = new Task("НОВАЯ ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ", now, Duration.ofMinutes(15));
        manager.createTask(task1);

        task1 = new Task(1, "ОБНОВЛЕННАЯ ЗАДАЧА", "ОПИСАНИЕ ИЗМЕНЕНО", Status.DONE,
                now.plusMinutes(10), Duration.ofMinutes(45));
        String taskJson = gson.toJson(TaskDto.fromTask(task1));
        HttpResponse<String> response = sendRequest("POST", "/tasks/1", taskJson);
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");

        Task taskToCheck = manager.getTaskById(1);

        checkTasksUnchangedCustom(List.of(taskToCheck), List.of(task1));
    }

    @Test
    public void deleteTaskWorksCorrectly() throws IOException, InterruptedException {
        task1 = new Task("НОВАЯ ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ", now, Duration.ofMinutes(15));
        manager.createTask(task1);

        checkTaskCountCustom(manager, 1, 0, 0, 1);

        HttpResponse<String> response = sendRequest("DELETE", "/tasks/1", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");

        checkTaskCountCustom(manager, 0, 0, 0, 1);
    }

    @Test
    public void getTaskByIdSendsNotFoundIfThereIsNoTask() throws IOException, InterruptedException {
        task1 = manager.createTask(new Task("НОВАЯ ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ", now, Duration.ofMinutes(15)));

        HttpResponse<String> response = sendRequest("GET", "/tasks/999", "");
        assertEquals(404, response.statusCode(), "Сервер должен возвращать код 404.");
        assertTrue(response.body().contains("not found"), "Тело ответа должно содержать 'not found'.");
    }

    @Test
    public void createTaskSendsHasInteractionsIfTaskIntervalIsOccupied() throws IOException, InterruptedException {
        task1 = manager.createTask(new Task("НОВАЯ ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ", now, Duration.ofMinutes(15)));
        task2 = new Task("ЗАДАЧА С ТЕМ ЖЕ ВРЕМЕНЕМ", "НОВОЕ ОПИСАНИЕ ЗАДАЧИ", now, Duration.ofMinutes(15));

        String taskJson = gson.toJson(TaskDto.fromTask(task2));
        HttpResponse<String> response = sendRequest("POST", "/tasks/", taskJson);
        assertEquals(406, response.statusCode(), "Сервер должен возвращать код 406.");
        assertTrue(response.body().contains("interval is occupied"),
                "Тело ответа должно содержать 'interval is occupied'.");

        checkTaskCountCustom(manager, 1, 0, 0, 1);
        checkTasksUnchangedCustom(manager.getTasks(), List.of(task1));
    }

    //ТЕСТЫ /subtasks
    @Test
    public void getSubTasksWorksCorrectly() throws IOException, InterruptedException {
        createSevenTaskList(manager);
        List<SubTask> subTasksInManager = manager.getSubTasks();

        HttpResponse<String> response = sendRequest("GET", "/subtasks", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        List<TaskDto> subTasksDto = gson.fromJson(response.body(), TASK_DTO_LIST_TYPE);
        List<SubTask> subTasksFromResponse = subTasksDto.stream()
                .map(TaskDto::toSubTask)
                .toList();

        checkTasksUnchangedCustom(subTasksFromResponse, subTasksInManager);
    }

    @Test
    public void getSubTaskByIdWorksCorrectly() throws IOException, InterruptedException {
        createThreeTaskListForTests(manager);

        HttpResponse<String> response = sendRequest("GET", "/subtasks/3", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        TaskDto taskDto = gson.fromJson(response.body(), TaskDto.class);
        Task returnedSubTask = TaskDto.toSubTask(taskDto);

        checkTasksUnchangedCustom(List.of(subTask1), List.of(returnedSubTask));
    }

    @Test
    public void createSubTaskWorksCorrectly() throws IOException, InterruptedException {
        epic1 = manager.createEpic(new Epic("ЭПИК", "Описание"));
        subTask1 = new SubTask("НОВАЯ ПОДЗАДАЧА", "ОПИСАНИЕ", 1,
                now, Duration.ofMinutes(15));
        SubTask subTaskToCheck = new SubTask(2, "НОВАЯ ПОДЗАДАЧА", "ОПИСАНИЕ", 1,
                now, Duration.ofMinutes(15));
        String subTaskJson = gson.toJson(TaskDto.fromSubTask(subTask1));

        HttpResponse<String> response = sendRequest("POST", "/subtasks", subTaskJson);
        assertEquals(201, response.statusCode(), "Сервер должен возвращать код 201.");
        TaskDto subTaskDto = gson.fromJson(response.body(), TaskDto.class);
        SubTask returnedSubTask = TaskDto.toSubTask(subTaskDto);

        List<SubTask> subTasksFromManager = manager.getSubTasks();

        assertNotNull(subTasksFromManager, "Подзадачи не возвращаются.");
        assertEquals(1, subTasksFromManager.size(), "Некорректное количество подзадач.");
        checkTasksUnchangedCustom(subTasksFromManager, List.of(subTaskToCheck));
        checkTasksUnchangedCustom(subTasksFromManager, List.of(returnedSubTask));
    }

    @Test
    public void updateSubTaskWorksCorrectly() throws IOException, InterruptedException {
        epic1 = manager.createEpic(new Epic("ЭПИК", "Описание"));
        subTask1 = manager.createSubTask(new SubTask("НОВАЯ ПОДЗАДАЧА", "ОПИСАНИЕ", 1,
                now.plusMinutes(30), Duration.ofMinutes(15)));

        subTask1 = new SubTask(2, "ОБНОВЛЕННАЯ ПОДЗАДАЧА", "ОПИСАНИЕ ИЗМЕНЕНО", Status.DONE,
                1, now.plusMinutes(10), Duration.ofMinutes(45));
        String subTaskJson = gson.toJson(TaskDto.fromSubTask(subTask1));
        HttpResponse<String> response = sendRequest("POST", "/subtasks/2", subTaskJson);
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");

        Task subTaskToCheck = manager.getSubTaskById(2);

        checkTasksUnchangedCustom(List.of(subTaskToCheck), List.of(subTask1));
    }

    @Test
    public void deleteSubTaskWorksCorrectly() throws IOException, InterruptedException {
        epic1 = manager.createEpic(new Epic("ЭПИК", "Описание"));
        subTask1 = manager.createSubTask(new SubTask("НОВАЯ ПОДЗАДАЧА", "ОПИСАНИЕ", 1,
                now, Duration.ofMinutes(15)));

        checkTaskCountCustom(manager, 0, 1, 1, 2);

        HttpResponse<String> response = sendRequest("DELETE", "/subtasks/2", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");

        checkTaskCountCustom(manager, 0, 1, 0, 2);
    }

    @Test
    public void getSubTaskByIdSendsNotFoundIfThereIsNoSubTask() throws IOException, InterruptedException {
        epic1 = manager.createEpic(new Epic("ЭПИК", "Описание"));
        subTask1 = manager.createSubTask(new SubTask("НОВАЯ ПОДЗАДАЧА", "ОПИСАНИЕ", 1,
                now.plusMinutes(30), Duration.ofMinutes(15)));

        HttpResponse<String> response = sendRequest("GET", "/subtasks/999", "");
        assertEquals(404, response.statusCode(), "Сервер должен возвращать код 404.");
        assertTrue(response.body().contains("not found"), "Тело ответа должно содержать 'not found'.");
    }

    @Test
    public void createSubTaskSendsHasInteractionsIfSubTaskIntervalIsOccupied() throws IOException, InterruptedException {
        epic1 = manager.createEpic(new Epic("ЭПИК", "Описание"));
        subTask1 = manager.createSubTask(new SubTask("НОВАЯ ПОДЗАДАЧА", "ОПИСАНИЕ", 1,
                now, Duration.ofMinutes(15)));
        subTask2 = new SubTask("ПОДЗАДАЧА С ТЕМ ЖЕ ВРЕМЕНЕМ", "ДРУГОЕ ОПИСАНИЕ", 1,
                now, Duration.ofMinutes(15));

        String subTaskJson = gson.toJson(TaskDto.fromSubTask(subTask2));
        HttpResponse<String> response = sendRequest("POST", "/subtasks/", subTaskJson);
        assertEquals(406, response.statusCode(), "Сервер должен возвращать код 406.");
        assertTrue(response.body().contains("interval is occupied"),
                "Тело ответа должно содержать 'interval is occupied'.");

        checkTaskCountCustom(manager, 0, 1, 1, 2);
        checkTasksUnchangedCustom(manager.getSubTasks(), List.of(subTask1));
    }

    //ТЕСТЫ /epics
    @Test
    public void getEpicsWorksCorrectly() throws IOException, InterruptedException {
        createSevenTaskList(manager);
        List<Epic> epicsInManager = manager.getEpics();

        HttpResponse<String> response = sendRequest("GET", "/epics", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        List<TaskDto> epicsDto = gson.fromJson(response.body(), TASK_DTO_LIST_TYPE);
        List<Epic> epicsFromResponse = epicsDto.stream()
                .map(TaskDto::toEpic)
                .toList();

        checkTasksUnchangedCustom(epicsFromResponse, epicsInManager);
    }

    @Test
    public void getEpicByIdWorksCorrectly() throws IOException, InterruptedException {
        epic1 = manager.createEpic(new Epic("НОВЫЙ ЭПИК", "Описание"));

        HttpResponse<String> response = sendRequest("GET", "/epics/1", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        TaskDto epicDto = gson.fromJson(response.body(), TaskDto.class);
        Task returnedEpic = TaskDto.toEpic(epicDto);

        checkTasksUnchangedCustom(List.of(epic1), List.of(returnedEpic));
    }

    @Test
    public void getEpicSubtasksWorksCorrectly() throws IOException, InterruptedException {
        createSevenTaskList(manager);
        List<SubTask> epicsSubtasksInManager = manager.getEpicSubTasks(3);

        HttpResponse<String> response = sendRequest("GET", "/epics/3/subtasks", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        List<TaskDto> subTasksDto = gson.fromJson(response.body(), TASK_DTO_LIST_TYPE);
        List<SubTask> subTasksFromResponse = subTasksDto.stream()
                .map(TaskDto::toSubTask)
                .toList();

        checkTasksUnchangedCustom(subTasksFromResponse, epicsSubtasksInManager);
    }

    @Test
    public void createEpicWorksCorrectly() throws IOException, InterruptedException {
        epic1 = new Epic("НОВЫЙ ЭПИК", "Описание");
        Epic epicToCheck = new Epic(1, "НОВЫЙ ЭПИК", "Описание");
        String epicJson = gson.toJson(TaskDto.fromEpic(epic1));

        HttpResponse<String> response = sendRequest("POST", "/epics", epicJson);
        assertEquals(201, response.statusCode(), "Сервер должен возвращать код 201.");
        TaskDto epicDto = gson.fromJson(response.body(), TaskDto.class);
        Epic returnedEpic = TaskDto.toEpic(epicDto);

        List<Epic> epicsFromManager = manager.getEpics();

        assertNotNull(epicsFromManager, "Эпики не возвращаются.");
        assertEquals(1, epicsFromManager.size(), "Некорректное количество эпиков.");
        checkTasksUnchangedCustom(epicsFromManager, List.of(epicToCheck));
        checkTasksUnchangedCustom(epicsFromManager, List.of(returnedEpic));
    }

    @Test
    public void updateEpicWorksCorrectly() throws IOException, InterruptedException {
        createThreeTaskListForTests(manager);

        epic1 = new Epic(2, "ОБНОВЛЕННЫЙ ЭПИК", "ОПИСАНИЕ ИЗМЕНЕНО");
        String epicJson = gson.toJson(TaskDto.fromEpic(epic1));
        HttpResponse<String> response = sendRequest("POST", "/epics/2", epicJson);
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");

        epic1.setEpicTime(subTask1.getStartTime(), subTask1.getDuration(), subTask1.getEndTime());
        Task epicToCheck = manager.getEpicById(2);

        checkTasksUnchangedCustom(List.of(epicToCheck), List.of(epic1));
    }

    @Test
    public void deleteEpicWorksCorrectly() throws IOException, InterruptedException {
        createThreeTaskListForTests(manager);

        HttpResponse<String> response = sendRequest("DELETE", "/epics/2", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");

        checkTaskCountCustom(manager, 1, 0, 0, 3);
    }

    @Test
    public void getEpicByIdSendsNotFoundIfThereIsNoEpic() throws IOException, InterruptedException {
        createThreeTaskListForTests(manager);

        HttpResponse<String> response = sendRequest("GET", "/epics/999", "");
        assertEquals(404, response.statusCode(), "Сервер должен возвращать код 404.");
        assertTrue(response.body().contains("not found"), "Тело ответа должно содержать 'not found'.");
    }

    @Test
    public void getEpicSubTasksSendsNotFoundIfThereIsNoEpic() throws IOException, InterruptedException {
        createThreeTaskListForTests(manager);

        HttpResponse<String> response = sendRequest("GET", "/epics/999/subtasks", "");
        assertEquals(404, response.statusCode(), "Сервер должен возвращать код 404.");
        assertTrue(response.body().contains("not found"), "Тело ответа должно содержать 'not found'.");
    }

    //ТЕСТЫ /history
    @Test
    public void getHistoryWorksCorrectly() throws IOException, InterruptedException {
        createThreeTaskListForTests(manager);
        manager.getSubTaskById(3);
        manager.getEpicById(2);
        manager.getTaskById(1);

        HttpResponse<String> response = sendRequest("GET", "/history", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        List<TaskDto> historyDto = gson.fromJson(response.body(), TASK_DTO_LIST_TYPE);
        List<Task> historyFromResponse = historyDto.stream()
                .map(dto -> switch (dto.getTaskType()) {
                    case TaskType.TASK -> TaskDto.toTask(dto);
                    case TaskType.EPIC -> TaskDto.toEpic(dto);
                    case TaskType.SUBTASK -> TaskDto.toSubTask(dto);
                })
                .toList();

        checkTasksUnchangedCustom(historyFromResponse, manager.getHistory());
    }

    @Test
    public void clearHistoryWorksCorrectly() throws IOException, InterruptedException {
        createThreeTaskListForTests(manager);
        manager.getSubTaskById(3);
        manager.getEpicById(2);
        manager.getTaskById(1);

        HttpResponse<String> response = sendRequest("DELETE", "/history", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        assertTrue(manager.getHistory().isEmpty(), "История должна быть очищена.");
    }

    //ТЕСТЫ /prioritized
    @Test
    public void getPrioritizedTasksWorksCorrectly() throws IOException, InterruptedException {
        createSevenTaskList(manager);

        HttpResponse<String> response = sendRequest("GET", "/prioritized", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        List<TaskDto> prioritizedDto = gson.fromJson(response.body(), TASK_DTO_LIST_TYPE);
        List<Task> prioritizedFromResponse = prioritizedDto.stream()
                .map(dto -> switch (dto.getTaskType()) {
                    case TaskType.TASK -> TaskDto.toTask(dto);
                    case TaskType.EPIC -> TaskDto.toEpic(dto);
                    case TaskType.SUBTASK -> TaskDto.toSubTask(dto);
                })
                .toList();

        checkTasksUnchangedCustom(prioritizedFromResponse, new ArrayList<>(manager.getPrioritizedTasks()));
    }

    //ТЕСТЫ /all
    @Test
    public void getAllTasksWorksCorrectly() throws IOException, InterruptedException {
        createSevenTaskList(manager);

        HttpResponse<String> response = sendRequest("GET", "/all", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        List<TaskDto> allTasksDto = gson.fromJson(response.body(), TASK_DTO_LIST_TYPE);
        List<Task> allTasksFromResponse = allTasksDto.stream()
                .map(dto -> switch (dto.getTaskType()) {
                    case TaskType.TASK -> TaskDto.toTask(dto);
                    case TaskType.EPIC -> TaskDto.toEpic(dto);
                    case TaskType.SUBTASK -> TaskDto.toSubTask(dto);
                })
                .toList();

        checkTasksUnchangedCustom(allTasksFromResponse, manager.getAllTasks());
    }

    @Test
    public void findAnyTaskByIdWorksCorrectly() throws IOException, InterruptedException {
        createThreeTaskListForTests(manager);
        HttpResponse<String> response = sendRequest("GET", "/all/1", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        TaskDto taskDto = gson.fromJson(response.body(), TaskDto.class);
        Task returnedTask = TaskDto.toTask(taskDto);

        response = sendRequest("GET", "/all/2", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        TaskDto epicDto = gson.fromJson(response.body(), TaskDto.class);
        Epic returnedEpic = TaskDto.toEpic(epicDto);

        response = sendRequest("GET", "/all/3", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        TaskDto subTaskDto = gson.fromJson(response.body(), TaskDto.class);
        SubTask returnedSubTask = TaskDto.toSubTask(subTaskDto);

        checkTasksUnchangedCustom(List.of(returnedTask, returnedEpic, returnedSubTask), manager.getAllTasks());
    }

    @Test
    public void deleteAllTasksWorksCorrectly() throws IOException, InterruptedException {
        createThreeTaskListForTests(manager);

        HttpResponse<String> response = sendRequest("DELETE", "/all", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");

        checkTaskCountCustom(manager, 0, 0, 0, 0);
    }

    @Test
    public void deleteAnyTaskByIdWorksCorrectly() throws IOException, InterruptedException {
        createThreeTaskListForTests(manager);

        HttpResponse<String> response = sendRequest("DELETE", "/all/1", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        checkTaskCountCustom(manager, 0, 1, 1, 3);

        response = sendRequest("DELETE", "/all/3", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        checkTaskCountCustom(manager, 0, 1, 0, 3);

        response = sendRequest("DELETE", "/all/2", "");
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        checkTaskCountCustom(manager, 0, 0, 0, 3);
    }

    @Test
    public void findAnyTaskByIdSendsNotFoundIfThereIsNoTask() throws IOException, InterruptedException {
        createThreeTaskListForTests(manager);

        HttpResponse<String> response = sendRequest("GET", "/all/999", "");
        assertEquals(404, response.statusCode(), "Сервер должен возвращать код 404.");
        assertTrue(response.body().contains("not found"), "Тело ответа должно содержать 'not found'.");
    }

    @Test
    public void createAnyTaskWorksCorrectly() throws IOException, InterruptedException {
        HttpResponse<String> response;
        task1 = new Task("НОВАЯ ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ", now, Duration.ofMinutes(15));
        epic1 = new Epic("НОВЫЙ ЭПИК", "ОПИСАНИЕ ЭПИКА");
        subTask1 = new SubTask("НОВАЯ ПОДЗАДАЧА", "ОПИСАНИЕ ПОДЗАДАЧИ", 2,
                now.plusMinutes(15), Duration.ofMinutes(15));

        Task taskToCheck = new Task(1, "НОВАЯ ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ", now, Duration.ofMinutes(15));
        Epic epicToCheck = new Epic(2, "НОВЫЙ ЭПИК", "ОПИСАНИЕ ЭПИКА");
        SubTask subTaskToCheck = new SubTask(3, "НОВАЯ ПОДЗАДАЧА", "ОПИСАНИЕ ПОДЗАДАЧИ", 2,
                now.plusMinutes(15), Duration.ofMinutes(15));

        String taskJson = gson.toJson(TaskDto.fromTask(task1));
        String epicJson = gson.toJson(TaskDto.fromEpic(epic1));
        String subTaskJson = gson.toJson(TaskDto.fromSubTask(subTask1));

        response = sendRequest("POST", "/all", taskJson);
        assertEquals(201, response.statusCode(), "Сервер должен возвращать код 201.");
        TaskDto taskDto = gson.fromJson(response.body(), TaskDto.class);
        Task returnedTask = TaskDto.toTask(taskDto);

        List<Task> tasksFromManager = manager.getTasks();

        assertNotNull(tasksFromManager, "Задачи не возвращаются");
        assertEquals(1, tasksFromManager.size(), "Некорректное количество задач");
        checkTasksUnchangedCustom(tasksFromManager, List.of(taskToCheck));
        checkTasksUnchangedCustom(tasksFromManager, List.of(returnedTask));

        response = sendRequest("POST", "/all", epicJson);
        assertEquals(201, response.statusCode(), "Сервер должен возвращать код 201.");
        TaskDto epicDto = gson.fromJson(response.body(), TaskDto.class);
        Task returnedEpic = TaskDto.toEpic(epicDto);

        List<Epic> epicsFromManager = manager.getEpics();

        assertNotNull(epicsFromManager, "Эпики не возвращаются");
        assertEquals(1, epicsFromManager.size(), "Некорректное количество эпиков");
        checkTasksUnchangedCustom(epicsFromManager, List.of(epicToCheck));
        checkTasksUnchangedCustom(epicsFromManager, List.of(returnedEpic));

        response = sendRequest("POST", "/all", subTaskJson);
        assertEquals(201, response.statusCode(), "Сервер должен возвращать код 201.");
        TaskDto subTaskDto = gson.fromJson(response.body(), TaskDto.class);
        Task returnedSubTask = TaskDto.toSubTask(subTaskDto);

        List<SubTask> subTasksFromManager = manager.getSubTasks();

        assertNotNull(subTasksFromManager, "Подзадачи не возвращаются");
        assertEquals(1, subTasksFromManager.size(), "Некорректное количество подзадач");
        checkTasksUnchangedCustom(subTasksFromManager, List.of(subTaskToCheck));
        checkTasksUnchangedCustom(subTasksFromManager, List.of(returnedSubTask));
    }

    @Test
    public void updateAnyTaskWorksCorrectly() throws IOException, InterruptedException {
        HttpResponse<String> response;
        createThreeTaskListForTests(manager);

        task1 = new Task(1, "ОБНОВЛЕННАЯ ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ ИЗМЕНЕНО", Status.DONE,
                now.plusMinutes(10), Duration.ofMinutes(45));
        subTask1 = new SubTask(3, "ОБНОВЛЕННАЯ ПОДЗАДАЧА", "ОПИСАНИЕ ПОДЗАДАЧИ ИЗМЕНЕНО", Status.DONE,
                2, now.plusMinutes(60), Duration.ofMinutes(45));
        epic1 = new Epic(2, "ОБНОВЛЕННЫЙ ЭПИК", "ОПИСАНИЕ ЭПИКА ИЗМЕНЕНО");

        String taskJson = gson.toJson(TaskDto.fromTask(task1));
        String subTaskJson = gson.toJson(TaskDto.fromSubTask(subTask1));
        String epicJson = gson.toJson(TaskDto.fromEpic(epic1));

        response = sendRequest("POST", "/all/1", taskJson);
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        response = sendRequest("POST", "/all/3", subTaskJson);
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");
        response = sendRequest("POST", "/all/2", epicJson);
        assertEquals(200, response.statusCode(), "Сервер должен возвращать код 200.");

        epic1.setStatus(Status.DONE);
        epic1.setEpicTime(subTask1.getStartTime(), subTask1.getDuration(), subTask1.getEndTime());

        checkTasksUnchangedCustom(manager.getAllTasks(), List.of(task1, epic1, subTask1));
    }

    @Test
    public void createAnyTaskSendsHasInteractionsIfTaskIntervalIsOccupied() throws IOException, InterruptedException {
        HttpResponse<String> response;

        epic1 = manager.createEpic(new Epic("ЭПИК", "Описание"));
        subTask1 = manager.createSubTask(new SubTask("НОВАЯ ПОДЗАДАЧА", "ОПИСАНИЕ", 1,
                now, Duration.ofMinutes(15)));
        subTask2 = new SubTask("ПОДЗАДАЧА С ТЕМ ЖЕ ВРЕМЕНЕМ", "ДРУГОЕ ОПИСАНИЕ", 1,
                now, Duration.ofMinutes(15));
        task1 = manager.createTask(new Task("НОВАЯ ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ",
                now.plusMinutes(60), Duration.ofMinutes(15)));
        task2 = new Task("ЗАДАЧА С ТЕМ ЖЕ ВРЕМЕНЕМ", "НОВОЕ ОПИСАНИЕ ЗАДАЧИ",
                now.plusMinutes(60), Duration.ofMinutes(15));

        String subTaskJson = gson.toJson(TaskDto.fromSubTask(subTask2));
        String taskJson = gson.toJson(TaskDto.fromTask(task2));

        response = sendRequest("POST", "/all", subTaskJson);
        assertEquals(406, response.statusCode(), "Сервер должен возвращать код 406.");
        assertTrue(response.body().contains("interval is occupied"),
                "Тело ответа должно содержать 'interval is occupied'.");

        response = sendRequest("POST", "/all", taskJson);
        assertEquals(406, response.statusCode(), "Сервер должен возвращать код 406.");
        assertTrue(response.body().contains("interval is occupied"),
                "Тело ответа должно содержать 'interval is occupied'.");

        checkTaskCountCustom(manager, 1, 1, 1, 3);
        checkTasksUnchangedCustom(manager.getAllTasks(), List.of(task1, epic1, subTask1));
    }

    //ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    private HttpResponse<String> sendRequest(String method, String path, String body) throws IOException, InterruptedException {
        URI url = URI.create("http://localhost:8080" + path);
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(url);

        switch (method.toUpperCase()) {
            case "GET" -> builder.GET();
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body));
            case "DELETE" -> builder.DELETE();
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected void createSevenTaskList(TaskManager manager) {
        task1 = new Task("ПЕРВАЯ ЗАДАЧА", "Описание 1", now.plusMinutes(45), Duration.ofMinutes(60));
        task2 = new Task("ПОСЛЕДНЯЯ ЗАДАЧА", "Описание 2", now.plusYears(1).minusMinutes(15), Duration.ofMinutes(15));
        epic1 = new Epic("ЭПИК1", "Описание 3");
        epic2 = new Epic("ЭПИК2", "Описание 4");
        subTask1 = new SubTask("ПОДЗАДАЧА1-1", "Описание 5", 3, now.plusMinutes(30), Duration.ofMinutes(15));
        subTask2 = new SubTask("ПОДЗАДАЧА1-1", "Описание 6", 3, now.plusMinutes(10), Duration.ofMinutes(4));
        subTask3 = new SubTask("ПОДЗАДАЧА1-1", "Описание 7", 4, now.plusMinutes(20), Duration.ofMinutes(9));

        task1 = manager.createTask(task1);
        task2 = manager.createTask(task2);
        epic1 = manager.createEpic(epic1);
        epic2 = manager.createEpic(epic2);
        subTask1 = manager.createSubTask(subTask1);
        subTask2 = manager.createSubTask(subTask2);
        subTask3 = manager.createSubTask(subTask3);
    }

    protected void createThreeTaskListForTests(TaskManager manager) {
        task1 = new Task("ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ", now, Duration.ofMinutes(30));
        epic1 = new Epic("ЭПИК", "ОПИСАНИЕ ЭПИКА");
        subTask1 = new SubTask("ПОДЗАДАЧА", "ОПИСАНИЕ ПОДЗАДАЧИ", 2, now.plusHours(1), Duration.ofMinutes(30));

        task1 = manager.createTask(task1);
        epic1 = manager.createEpic(epic1);
        subTask1 = manager.createSubTask(subTask1);
    }

    private void checkTasksUnchangedCustom(List<? extends Task> tasksToCheck, List<? extends Task> initialTasks) {
        assertEquals(tasksToCheck.size(), initialTasks.size(), "Размер списка на проверку должен " +
                "совпадать с размером списка менеджера.");

        for (int i = 0; i < tasksToCheck.size(); i++) {
            Task initialTask = initialTasks.get(i);
            Task taskToCheck = tasksToCheck.get(i);

            assertEquals(initialTask.getClass(), taskToCheck.getClass(),
                    "Классы задач не совпадают на позиции " + i);
            assertEquals(initialTask.getId(), taskToCheck.getId(),
                    "ID задач не совпадает на позиции " + i);
            assertEquals(initialTask.getName(), taskToCheck.getName(),
                    "Имя задачи не совпадает на позиции " + i);
            assertEquals(initialTask.getDescription(), taskToCheck.getDescription(),
                    "Описание не совпадает на позиции " + i);
            assertEquals(initialTask.getStatus(), taskToCheck.getStatus(),
                    "Статус не совпадает на позиции " + i);
            assertEquals(initialTask.getStartTime(), taskToCheck.getStartTime(),
                    "Начало задачи не совпадает на позиции " + i);
            assertEquals(initialTask.getDuration(), taskToCheck.getDuration(),
                    "Длительность задачи не совпадает на позиции " + i);
            assertEquals(initialTask.getEndTime(), taskToCheck.getEndTime(),
                    "Конец задачи не совпадает на позиции " + i);
            if (initialTask instanceof SubTask initialSubTask && taskToCheck instanceof SubTask subTaskToCheck) {
                assertEquals(initialSubTask.getEpicId(), subTaskToCheck.getEpicId(),
                        "epicId подзадачи не совпадает на позиции " + i);
            }
        }
    }

    protected void checkTaskCountCustom(TaskManager manager, int tasks, int epics, int subTasks, int taskCount) {
        assertEquals(tasks, manager.getTasks().size(), "В списке задач должно быть " + tasks + " задач.");
        assertEquals(epics, manager.getEpics().size(), "В списке эпиков должно быть " + epics + " эпиков.");
        assertEquals(subTasks, manager.getSubTasks().size(), "В списке подзадач должно быть " + subTasks + " подзадач.");
        assertEquals(taskCount, manager.getIdCounter(), "Счетчик менеджера должен быть равен " + taskCount + ".");
    }
}


