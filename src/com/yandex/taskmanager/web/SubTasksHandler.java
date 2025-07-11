package com.yandex.taskmanager.web;

import com.yandex.taskmanager.model.Status;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.service.TaskManager;
import com.yandex.taskmanager.web.json.GsonAdapters;
import com.yandex.taskmanager.web.dto.TaskDto;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonSyntaxException;
import com.yandex.taskmanager.exceptions.NotFoundException;

public class SubTasksHandler extends BaseHttpHandler implements HttpHandler {
    TaskManager manager;
    Gson gson = GsonAdapters.createGson();

    public SubTasksHandler(TaskManager manager) {
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
                        sendSubTasks(httpExchange);
                    } else if (parts.length == 3) {
                        int id = parseId(httpExchange, parts);
                        SubTask subTask = manager.getSubTaskById(id);
                        sendText(httpExchange, gson.toJson(TaskDto.fromSubTask(subTask)));
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "POST":
                    if (parts.length == 2) {
                        createSubTask(httpExchange);
                    } else if (parts.length == 3) {
                        int id = parseId(httpExchange, parts);
                        updateSubTask(httpExchange, id);
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "DELETE":
                    if (parts.length == 3) {
                        int id = parseId(httpExchange, parts);
                        manager.deleteSubTask(id);
                        sendOk(httpExchange, "SubTask with id: " + id + " deleted.");
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
            sendInternalServerError(httpExchange, "Internal server error.");
        }
    }

    private void sendSubTasks(HttpExchange httpExchange) throws IOException {
        List<TaskDto> dtoList = manager.getSubTasks().stream()
                .map(TaskDto::fromSubTask)
                .collect(Collectors.toList());

        sendText(httpExchange, gson.toJson(dtoList));
    }

    private void createSubTask(HttpExchange httpExchange) throws IOException {
        InputStream is = httpExchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        try {
            TaskDto dtoSubTask = gson.fromJson(body, TaskDto.class);
            String name = dtoSubTask.getName();
            String description = dtoSubTask.getDescription();
            LocalDateTime startTime = dtoSubTask.getStartTime();
            Duration duration = dtoSubTask.getDuration();
            int epicId;

            try {
                epicId = dtoSubTask.getEpicId();
            } catch (NullPointerException ex) {
                sendInvalidPathFormat(httpExchange, "EpicId provided to Task Manager is null.");
                throw new NullPointerException("EpicId provided to Task Manager is null.");
            }

            SubTask newSubTask = manager.createSubTask(new SubTask(name, description, epicId, startTime, duration));
            String jsonResponse = gson.toJson(TaskDto.fromSubTask(newSubTask));
            sendCreated(httpExchange, jsonResponse);
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        } catch (JsonSyntaxException ex) {
            sendInvalidPathFormat(httpExchange, "Invalid JSON format.");
        }
    }

    private void updateSubTask(HttpExchange httpExchange, int id) throws IOException {
        InputStream is = httpExchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        try {
            TaskDto dtoSubTask = gson.fromJson(body, TaskDto.class);
            String name = dtoSubTask.getName();
            String description = dtoSubTask.getDescription();
            Status status = dtoSubTask.getStatus();
            LocalDateTime startTime = dtoSubTask.getStartTime();
            Duration duration = dtoSubTask.getDuration();

            SubTask subTaskInManager = manager.getSubTaskById(id);
            int epicId = subTaskInManager.getEpicId();
            SubTask subTaskToUpdate = new SubTask(id, name, description, status, epicId, startTime, duration);
            manager.updateSubTask(subTaskToUpdate);
            sendOk(httpExchange, "SubTask with id: " + id + " updated.");
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        } catch (NotFoundException ex) {
            sendNotFound(httpExchange, ex.getMessage());
        } catch (JsonSyntaxException ex) {
            sendInvalidPathFormat(httpExchange, "Invalid JSON format.");
        }
    }

    private Integer parseId(HttpExchange httpExchange, String[] parts) throws IOException {
        try {
            return Integer.parseInt(parts[2]);
        } catch (NumberFormatException ex) {
            sendInvalidPathFormat(httpExchange, "Invalid id format.");
            throw new IllegalArgumentException("Invalid id format.");
        }
    }
}
