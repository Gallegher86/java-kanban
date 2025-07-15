package com.yandex.taskmanager.web;

import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.TaskType;
import com.yandex.taskmanager.service.TaskManager;
import com.yandex.taskmanager.web.dto.TaskDto;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.yandex.taskmanager.exceptions.NotFoundException;

class AllTasksHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    AllTasksHandler(TaskManager manager, Gson gson) {
        this.manager = manager;
        this.gson = gson;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            String method = httpExchange.getRequestMethod();
            String rawPath = httpExchange.getRequestURI().getPath();
            String path = rawPath.replaceAll("/+$", "");

            switch (method) {
                case "GET":
                    if (path.matches("^/all$")) {
                        sendAllTasks(httpExchange);
                    } else if (path.matches("^/all/\\d+$")) {
                        int id = parseId(path);
                        sendAnyTask(httpExchange, id);
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "POST":
                    if (path.matches("^/all$")) {
                        createAnyTask(httpExchange);
                    } else if (path.matches("^/all/\\d+$")) {
                        int id = parseId(path);
                        updateAnyTask(httpExchange, id);
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "DELETE":
                    if (path.matches("^/all$")) {
                        manager.deleteAllTasks();
                        sendOk(httpExchange, "All tasks deleted.");
                    } else if (path.matches("^/all/\\d+$")) {
                        int id = parseId(path);
                        manager.deleteAnyTaskById(id);
                        sendOk(httpExchange, "Task with id: " + id + " deleted.");
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "HEAD":
                    httpExchange.sendResponseHeaders(200, -1);
                    break;
                default:
                    sendMethodNotAllowed(httpExchange, "Method: " + method + " not allowed.");
            }
        } catch (NotFoundException ex) {
            sendNotFound(httpExchange, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendInvalidPathFormat(httpExchange, ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            sendInternalServerError(httpExchange);
        }
    }

    private void sendAnyTask(HttpExchange httpExchange, int id) throws IOException {
        Task task = manager.findAnyTaskById(id)
                .orElseThrow(() -> new NotFoundException("Task with id: " + id + " not found in TaskManager."));

        TaskDto dto;
        if (task instanceof Epic epic) {
            dto = TaskDto.fromEpic(epic);
        } else if (task instanceof SubTask subTask) {
            dto = TaskDto.fromSubTask(subTask);
        } else {
            dto = TaskDto.fromTask(task);
        }

        sendText(httpExchange, gson.toJson(dto));
    }

    private void sendAllTasks(HttpExchange httpExchange) throws IOException {
        List<TaskDto> dtoList = manager.getAllTasks().stream()
                .map(task -> {
                    if (task instanceof Epic) {
                        return TaskDto.fromEpic((Epic) task);
                    } else if (task instanceof SubTask) {
                        return TaskDto.fromSubTask((SubTask) task);
                    } else {
                        return TaskDto.fromTask(task);
                    }
                })
                .collect(Collectors.toList());

        sendText(httpExchange, gson.toJson(dtoList));
    }

    private void createAnyTask(HttpExchange httpExchange) throws IOException {
        String jsonResponse;

        try {
            TaskDto dto = readDto(httpExchange, gson);
            if (dto == null) {
                sendInvalidPathFormat(httpExchange, "Empty or malformed JSON, can't create Task.");
                return;
            }

            if (dto.getTaskType() == null) {
                throw new IllegalArgumentException("'taskType' is required to create Task.");
            }

            switch (dto.getTaskType()) {
                case TaskType.TASK:
                    Task newTask = manager.createTask(TaskDto.toNewTask(dto));
                    jsonResponse = gson.toJson(TaskDto.fromTask(newTask));
                    sendCreated(httpExchange, jsonResponse);
                    break;
                case TaskType.EPIC:
                    Epic newEpic = manager.createEpic(TaskDto.toNewEpic(dto));
                    jsonResponse = gson.toJson(TaskDto.fromEpic(newEpic));
                    sendCreated(httpExchange, jsonResponse);
                    break;
                case TaskType.SUBTASK:
                    SubTask newSubTask = manager.createSubTask(TaskDto.toNewSubTask(dto));
                    jsonResponse = gson.toJson(TaskDto.fromSubTask(newSubTask));
                    sendCreated(httpExchange, jsonResponse);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported task type: " + dto.getTaskType());
            }
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        }
    }

    private void updateAnyTask(HttpExchange httpExchange, int id) throws IOException {
        try {
            TaskDto dto = readDto(httpExchange, gson);
            if (dto == null) {
                sendInvalidPathFormat(httpExchange, "Empty or malformed JSON, can't create Task.");
                return;
            }

            if (dto.getTaskType() == null) {
                throw new IllegalArgumentException("'taskType' is required to create Task.");
            }

            switch (dto.getTaskType()) {
                case TaskType.TASK:
                    Task taskToUpdate = TaskDto.toTaskWithId(dto, id);
                    manager.updateTask(taskToUpdate);
                    sendOk(httpExchange, "Task with id: " + id + " updated.");
                    break;
                case TaskType.EPIC:
                    String name = dto.getName();
                    String description = dto.getDescription();

                    Epic epicToUpdate = new Epic(id, name, description);
                    manager.updateEpic(epicToUpdate);
                    sendOk(httpExchange, "Epic with id: " + id + " updated.");
                    break;
                case TaskType.SUBTASK:
                    SubTask subTaskToUpdate = TaskDto.toSubTaskWithId(dto, id);
                    manager.updateSubTask(subTaskToUpdate);
                    sendOk(httpExchange, "SubTask with id: " + id + " updated.");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported task type: " + dto.getTaskType());
            }
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        } catch (NotFoundException ex) {
            sendNotFound(httpExchange, ex.getMessage());
        }
    }
}
