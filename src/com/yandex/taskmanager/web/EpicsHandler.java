package com.yandex.taskmanager.web;

import com.yandex.taskmanager.service.TaskManager;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.web.dto.TaskDto;

import java.util.List;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;

import java.io.IOException;

import com.google.gson.JsonSyntaxException;
import com.yandex.taskmanager.exceptions.NotFoundException;

class EpicsHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    EpicsHandler(TaskManager manager, Gson gson) {
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
                    if (path.matches("^/epics$")) {
                        sendEpics(httpExchange);
                    } else if (path.matches("^/epics/\\d+$")) {
                        int id = parseId(path);
                        Epic epic = manager.getEpicById(id);
                        sendText(httpExchange, gson.toJson(TaskDto.fromEpic(epic)));
                    } else if (path.matches("^/epics/\\d+/subtasks$")) {
                        int id = parseId(path);
                        sendSubTasks(httpExchange, id);
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "POST":
                    if (path.matches("^/epics$")) {
                        createEpic(httpExchange);
                    } else if (path.matches("^/epics/\\d+$")) {
                        int id = parseId(path);
                        updateEpic(httpExchange, id);
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "DELETE":
                    if (path.matches("^/epics/\\d+$")) {
                        int id = parseId(path);
                        manager.deleteEpic(id);
                        sendOk(httpExchange, "Epic with id: " + id + " deleted.");
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

    private void sendEpics(HttpExchange httpExchange) throws IOException {
        List<TaskDto> dtoList = manager.getEpics().stream()
                .map(TaskDto::fromEpic)
                .collect(Collectors.toList());
        sendText(httpExchange, gson.toJson(dtoList));
    }

    private void sendSubTasks(HttpExchange httpExchange, int id) throws IOException {
        List<TaskDto> dtoList = manager.getEpicSubTasks(id).stream()
                .map(TaskDto::fromSubTask)
                .collect(Collectors.toList());

        sendText(httpExchange, gson.toJson(dtoList));
    }

    private void createEpic(HttpExchange httpExchange) throws IOException {
        try {
            TaskDto dtoEpic = readDto(httpExchange, gson);
            if (dtoEpic == null) {
                sendInvalidPathFormat(httpExchange, "Empty or malformed JSON, can't create Epic.");
                return;
            }

            Epic newEpic = manager.createEpic(TaskDto.toNewEpic(dtoEpic));
            String jsonResponse = gson.toJson(TaskDto.fromEpic(newEpic));
            sendCreated(httpExchange, jsonResponse);
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        } catch (JsonSyntaxException ex) {
            sendInvalidPathFormat(httpExchange, "Invalid JSON format.");
        }
    }

    private void updateEpic(HttpExchange httpExchange, int id) throws IOException {
        try {
            TaskDto dtoEpic = readDto(httpExchange, gson);
            String name = dtoEpic.getName();
            String description = dtoEpic.getDescription();

            Epic epicToUpdate = new Epic(id, name, description);
            manager.updateEpic(epicToUpdate);
            sendOk(httpExchange, "Epic with id: " + id + " updated.");
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        } catch (NotFoundException ex) {
            sendNotFound(httpExchange, ex.getMessage());
        } catch (JsonSyntaxException ex) {
            sendInvalidPathFormat(httpExchange, "Invalid JSON format.");
        }
    }
}
