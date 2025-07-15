package com.yandex.taskmanager.web;

import com.yandex.taskmanager.model.Status;
import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.service.TaskManager;
import com.yandex.taskmanager.web.dto.TaskDto;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.yandex.taskmanager.exceptions.NotFoundException;

class TasksHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    TasksHandler(TaskManager manager, Gson gson) {
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
                    if (path.matches("^/tasks$")) {
                        sendTasks(httpExchange);
                    } else if (path.matches("^/tasks/\\d+$")) {
                        int id = parseId(path);
                        Task task = manager.getTaskById(id);
                        sendText(httpExchange, gson.toJson(TaskDto.fromTask(task)));
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "POST":
                    if (path.matches("^/tasks$")) {
                        createTask(httpExchange);
                    } else if (path.matches("^/tasks/\\d+$")) {
                        int id = parseId(path);
                        updateTask(httpExchange, id);
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "DELETE":
                    if (path.matches("^/tasks/\\d+$")) {
                        int id = parseId(path);
                        manager.deleteTask(id);
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

    private void sendTasks(HttpExchange httpExchange) throws IOException {
        List<TaskDto> dtoList = manager.getTasks().stream()
                .map(TaskDto::fromTask)
                .collect(Collectors.toList());

        sendText(httpExchange, gson.toJson(dtoList));
    }

    private void createTask(HttpExchange httpExchange) throws IOException {
        try {
            TaskDto dtoTask = readDto(httpExchange, gson);
            if (dtoTask == null) {
                sendInvalidPathFormat(httpExchange, "Empty or malformed JSON, can't create Task.");
                return;
            }

            Task newTask = manager.createTask(TaskDto.toNewTask(dtoTask));
            String jsonResponse = gson.toJson(TaskDto.fromTask(newTask));
            sendCreated(httpExchange, jsonResponse);
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        }
    }

    private void updateTask(HttpExchange httpExchange, int id) throws IOException {
        try {
            TaskDto dtoTask = readDto(httpExchange, gson);
            if (dtoTask == null) {
                sendInvalidPathFormat(httpExchange, "Empty or malformed JSON, can't update Task.");
                return;
            }

            Task taskToUpdate = TaskDto.toTaskWithId(dtoTask, id);
            manager.updateTask(taskToUpdate);
            sendOk(httpExchange, "Task with id: " + id + " updated.");
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        } catch (NotFoundException ex) {
            sendNotFound(httpExchange, ex.getMessage());
        }
    }
}
