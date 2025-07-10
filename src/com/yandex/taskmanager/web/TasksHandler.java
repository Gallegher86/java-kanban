package com.yandex.taskmanager.web;

import com.google.gson.JsonSyntaxException;
import com.yandex.taskmanager.exceptions.NotFoundException;
import com.yandex.taskmanager.model.Status;
import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.web.json.GsonAdapters;
import com.yandex.taskmanager.web.dto.TaskDto;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.yandex.taskmanager.service.TaskManager;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

import java.io.IOException;

import com.google.gson.Gson;

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
                        sendText(httpExchange, gson.toJson(manager.getTasks()));
                    } else {
                        Integer id = parseId(httpExchange, parts);
                        if (id == null) return;
                        try {
                            Task task = manager.getTaskById(id);
                            sendText(httpExchange, gson.toJson(task));
                        } catch (NotFoundException ex) {
                            sendNotFound(httpExchange, ex.getMessage());
                        }
                    }
                    break;
                case "POST":
                    if (parts.length == 2) {
                        createTask(httpExchange);
                    } else {
                        Integer id = parseId(httpExchange, parts);
                        if (id == null) return;
                        updateTask(httpExchange, id);
                    }
                    break;
                case "DELETE":
                    Integer id = parseId(httpExchange, parts);
                    if (id == null) return;
                    try {
                        manager.deleteTask(id);
                        sendText(httpExchange, "Task with Id: " + id + " successfully deleted.");
                    } catch (NotFoundException ex) {
                        sendNotFound(httpExchange, ex.getMessage());
                    }
                    break;
                case "HEAD":
                    httpExchange.sendResponseHeaders(200, -1);
                    break;
                default:
                    sendMethodNotAllowed(httpExchange, "Method: " + method + " not allowed.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            sendInternalServerError(httpExchange, "Internal server error.");
        }
    }

    private void createTask(HttpExchange httpExchange) throws IOException {
        InputStream is = httpExchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        try {
            TaskDto dtoTask = gson.fromJson(body, TaskDto.class);
            String name = dtoTask.getName();
            String description = dtoTask.getDescription();
            LocalDateTime startTime = dtoTask.getStartTime();
            Duration duration = dtoTask.getDuration();

            Task newTask = manager.createTask(new Task(name, description, startTime, duration));
            String jsonResponse = gson.toJson(newTask);
            sendCreated(httpExchange, jsonResponse);
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        } catch (JsonSyntaxException ex) {
            sendInvalidPathFormat(httpExchange, "Invalid JSON format.");
        }
    }

    private void updateTask(HttpExchange httpExchange, int id) throws IOException {
        InputStream is = httpExchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        try {
            TaskDto dtoTask = gson.fromJson(body, TaskDto.class);
            String name = dtoTask.getName();
            String description = dtoTask.getDescription();
            Status status = dtoTask.getStatus();
            LocalDateTime startTime = dtoTask.getStartTime();
            Duration duration = dtoTask.getDuration();

            Task taskToUpdate = new Task(id, name, description, status, startTime, duration);
            manager.updateTask(taskToUpdate);
            String jsonResponse = gson.toJson(taskToUpdate);
            sendCreated(httpExchange, jsonResponse);
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        } catch (NotFoundException ex) {
            sendNotFound(httpExchange, ex.getMessage());
        } catch (JsonSyntaxException ex) {
            sendInvalidPathFormat(httpExchange, "Invalid JSON format.");
        }
    }

    private Integer parseId(HttpExchange exchange, String[] parts) throws IOException {
        if (parts.length != 3) {
            sendInvalidPathFormat(exchange, "Bad request: wrong path format");
            return null;
        }
        try {
            return Integer.parseInt(parts[2]);
        } catch (NumberFormatException ex) {
            sendInvalidPathFormat(exchange, "Invalid id format.");
            return null;
        }
    }
}
