package com.yandex.taskmanager.web;

import com.yandex.taskmanager.service.TaskManager;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.web.json.GsonAdapters;
import com.yandex.taskmanager.web.dto.TaskDto;

import java.util.List;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;

import java.io.IOException;

import com.google.gson.JsonSyntaxException;
import com.yandex.taskmanager.exceptions.NotFoundException;

public class EpicsHandler extends BaseHttpHandler implements HttpHandler {
    TaskManager manager;
    Gson gson = GsonAdapters.createGson();

    public EpicsHandler(TaskManager manager) {
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
                        sendEpics(httpExchange);
                    } else if (parts.length == 3) {
                        int id = parseId(parts);
                        Epic epic = manager.getEpicById(id);
                        sendText(httpExchange, gson.toJson(TaskDto.fromEpic(epic)));
                    } else if (parts.length == 4 && parts[3].equals("subtasks")) {
                        int id = parseId(parts);
                        sendSubTasks(httpExchange, id);
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "POST":
                    if (parts.length == 2) {
                        createEpic(httpExchange);
                    } else if (parts.length == 3) {
                        int id = parseId(parts);
                        updateEpic(httpExchange, id);
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "DELETE":
                    if (parts.length == 3) {
                        int id = parseId(parts);
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
            String name = dtoEpic.getName();
            String description = dtoEpic.getDescription();

            Epic newEpic = manager.createEpic(new Epic(name, description));
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
            sendOk(httpExchange, "Epic with id: " +id + " updated.");
        } catch (IllegalArgumentException ex) {
            sendHasInteractions(httpExchange, ex.getMessage());
        } catch (NotFoundException ex) {
            sendNotFound(httpExchange, ex.getMessage());
        } catch (JsonSyntaxException ex) {
            sendInvalidPathFormat(httpExchange, "Invalid JSON format.");
        }
    }
}
