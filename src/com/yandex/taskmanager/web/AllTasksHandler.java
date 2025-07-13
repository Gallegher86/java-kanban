package com.yandex.taskmanager.web;

import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.service.TaskManager;
import com.yandex.taskmanager.web.dto.TaskDto;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.yandex.taskmanager.exceptions.NotFoundException;

class AllTasksHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    AllTasksHandler(TaskManager manager, Gson gson) {
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
                        sendAllTasks(httpExchange);
                    } else if (parts.length == 3) {
                        int id = parseId(parts);
                        sendAnyTask(httpExchange, id);
                    } else {
                        sendInvalidPathFormat(httpExchange, "Bad request: wrong path format");
                    }
                    break;
                case "DELETE":
                    if (parts.length == 2) {
                        manager.deleteAllTasks();
                        sendOk(httpExchange, "All tasks deleted.");
                    } else if (parts.length == 3) {
                        int id = parseId(parts);
                        manager.deleteAnyTaskById(id);
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

    private void sendAnyTask(HttpExchange httpExchange, int id) throws IOException {
        Task task = manager.findAnyTaskById(id)
                .orElseThrow(() -> new NotFoundException("Task with id: " + id + " not found in TaskManager."));

        TaskDto dto;
        if (task instanceof Epic epic) {
            dto = TaskDto.fromEpic(epic);
        } else if (task instanceof SubTask subTask) {
            dto = TaskDto.fromSubTask(subTask);
        } else {
            dto = TaskDto.fromTask(task);
        }

        sendText(httpExchange, gson.toJson(dto));
    }

    private void sendAllTasks(HttpExchange httpExchange) throws IOException {
        List<TaskDto> dtoList = manager.getAllTasks().stream()
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
