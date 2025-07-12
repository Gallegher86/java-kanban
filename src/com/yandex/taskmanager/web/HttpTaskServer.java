package com.yandex.taskmanager.web;

import com.sun.net.httpserver.HttpServer;
import com.yandex.taskmanager.service.Managers;
import com.yandex.taskmanager.service.TaskManager;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpTaskServer {
    private static final int PORT = 8080;
    private final HttpServer httpServer;

    public HttpTaskServer(TaskManager manager) {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);
            httpServer.createContext("/tasks", new TasksHandler(manager));
            httpServer.createContext("/epics", new EpicsHandler(manager));
            httpServer.createContext("/subtasks", new SubTasksHandler(manager));
            httpServer.createContext("/history", new HistoryHandler(manager));
            httpServer.createContext("/prioritized", new PrioritizedHandler(manager));
            httpServer.createContext("/all", new AllTasksHandler(manager));
        } catch (IOException ex) {
            throw new RuntimeException("Can't start the server at PORT: " + PORT, ex);
        }
    }

    public void start() {
        httpServer.start();
        System.out.println("HTTP server is running on port " + PORT);
    }

    public void stop() {
        httpServer.stop(0);
        System.out.println("HTTP server is stopped.");
    }

    public static void main(String[] args) {
        TaskManager manager = Managers.getDefaultTaskManager();
        HttpTaskServer server = new HttpTaskServer(manager);
        server.start();
    }
}
