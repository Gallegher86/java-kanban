package com.yandex.taskmanager.web;

import com.yandex.taskmanager.model.Status;
import com.yandex.taskmanager.model.SubTask;
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

import com.google.gson.JsonSyntaxException;
import com.yandex.taskmanager.exceptions.NotFoundException;

class SubTasksHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    SubTasksHandler(TaskManager manager, Gson gson) {
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
                    if (path.matches("^/subtasks$")) {
                        sendSubTasks(httpExchange);
                    } else if (path.matches("^/subtasks/\\d+$")) {
                        int id = parseId(path);
                        SubTask subTask = manager.getSubTaskById(id);
                        sendText(httpExchange, gson.toJson(TaskDto.fromSubTask(subTask)));
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "POST":
                    if (path.matches("^/subtasks$")) {
                        createSubTask(httpExchange);
                    } else if (path.matches("^/subtasks/\\d+$")) {
                        int id = parseId(path);
                        updateSubTask(httpExchange, id);
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "DELETE":
                    if (path.matches("^/subtasks/\\d+$")) {
                        int id = parseId(path);
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
            sendInternalServerError(httpExchange);
        }
    }

    private void sendSubTasks(HttpExchange httpExchange) throws IOException {
        List<TaskDto> dtoList = manager.getSubTasks().stream()
                .map(TaskDto::fromSubTask)
                .collect(Collectors.toList());

        sendText(httpExchange, gson.toJson(dtoList));
    }

    private void createSubTask(HttpExchange httpExchange) throws IOException {
        try {
            TaskDto dtoSubTask = readDto(httpExchange, gson);
            if (dtoSubTask == null) {
                sendInvalidPathFormat(httpExchange, "Empty or malformed JSON, can't create Subtask.");
                return;
            }

            SubTask newSubTask = manager.createSubTask(TaskDto.toNewSubTask(dtoSubTask));
            String jsonResponse = gson.toJson(TaskDto.fromSubTask(newSubTask));
            sendCreated(httpExchange, jsonResponse);
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        } catch (JsonSyntaxException ex) {
            sendInvalidPathFormat(httpExchange, "Invalid JSON format.");
        }
    }

    private void updateSubTask(HttpExchange httpExchange, int id) throws IOException {
        try {
            TaskDto dtoSubTask = readDto(httpExchange, gson);
            if (dtoSubTask == null) {
                sendInvalidPathFormat(httpExchange, "Empty or malformed JSON, can't update Subtask.");
                return;
            }

            SubTask subTaskToUpdate = TaskDto.toSubTask(dtoSubTask);
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
}
