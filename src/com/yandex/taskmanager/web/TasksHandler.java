package com.yandex.taskmanager.web;

import com.yandex.taskmanager.model.Status;
import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.service.TaskManager;
import com.yandex.taskmanager.web.json.GsonAdapters;
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

public class TasksHandler extends BaseHttpHandler implements HttpHandler {
    TaskManager manager;
    Gson gson = GsonAdapters.createGson();

    public TasksHandler(TaskManager manager) {
        this.manager = manager;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            String method = httpExchange.getRequestMethod();
            String path = httpExchange.getRequestURI().getPath();
            String[] parts = path.split("/");

            switch (method) {
                case "GET":
                    if (parts.length == 2) {
                        sendTasks(httpExchange);
                    } else if (parts.length == 3) {
                        int id = parseId(parts);
                        Task task = manager.getTaskById(id);
                        sendText(httpExchange, gson.toJson(TaskDto.fromTask(task)));
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "POST":
                    if (parts.length == 2) {
                        createTask(httpExchange);
                    } else if (parts.length == 3) {
                        int id = parseId(parts);
                        updateTask(httpExchange, id);
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "DELETE":
                    if (parts.length == 3) {
                        int id = parseId(parts);
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
            String name = dtoTask.getName();
            String description = dtoTask.getDescription();
            LocalDateTime startTime = dtoTask.getStartTime();
            Duration duration = dtoTask.getDuration();

            Task newTask = manager.createTask(new Task(name, description, startTime, duration));
            String jsonResponse = gson.toJson(TaskDto.fromTask(newTask));
            sendCreated(httpExchange, jsonResponse);
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        }
    }

    private void updateTask(HttpExchange httpExchange, int id) throws IOException {
        try {
            TaskDto dtoTask = readDto(httpExchange, gson);
            String name = dtoTask.getName();
            String description = dtoTask.getDescription();
            Status status = dtoTask.getStatus();
            LocalDateTime startTime = dtoTask.getStartTime();
            Duration duration = dtoTask.getDuration();

            Task taskToUpdate = new Task(id, name, description, status, startTime, duration);
            manager.updateTask(taskToUpdate);
            sendOk(httpExchange, "Task with id: " + id + " updated.");
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        } catch (NotFoundException ex) {
            sendNotFound(httpExchange, ex.getMessage());
        }
    }
}
