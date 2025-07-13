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
            String path = httpExchange.getRequestURI().getPath();
            String[] parts = path.split("/");

            switch (method) {
                case "GET":
                    if (parts.length == 2) {
                        sendSubTasks(httpExchange);
                    } else if (parts.length == 3) {
                        int id = parseId(parts);
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
                        int id = parseId(parts);
                        updateSubTask(httpExchange, id);
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "DELETE":
                    if (parts.length == 3) {
                        int id = parseId(parts);
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
}
