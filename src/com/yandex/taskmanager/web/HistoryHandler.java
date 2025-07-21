package com.yandex.taskmanager.web;

import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.service.TaskManager;
import com.yandex.taskmanager.web.dto.TaskDto;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

class HistoryHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    HistoryHandler(TaskManager manager, Gson gson) {
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
                    if (path.matches("^/history$")) {
                        sendHistory(httpExchange);
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "DELETE":
                    if (path.matches("^/history$")) {
                        manager.clearHistory();
                        sendOk(httpExchange, "TaskManager history cleared.");
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
        } catch (Exception ex) {
            ex.printStackTrace();
            sendInternalServerError(httpExchange);
        }
    }

    private void sendHistory(HttpExchange httpExchange) throws IOException {
        List<TaskDto> dtoList = manager.getHistory().stream()
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
}
