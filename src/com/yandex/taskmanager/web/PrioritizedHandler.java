package com.yandex.taskmanager.web;

import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.service.TaskManager;
import com.yandex.taskmanager.web.json.GsonAdapters;
import com.yandex.taskmanager.web.dto.TaskDto;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

class PrioritizedHandler extends BaseHttpHandler implements HttpHandler {
    TaskManager manager;
    Gson gson = GsonAdapters.createGson();

    PrioritizedHandler(TaskManager manager) {
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
                        sendPrioritizedTasks(httpExchange);
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

    private void sendPrioritizedTasks(HttpExchange httpExchange) throws IOException {
        List<TaskDto> dtoList = manager.getPrioritizedTasks().stream()
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
