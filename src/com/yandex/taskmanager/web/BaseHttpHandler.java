package com.yandex.taskmanager.web;

import com.google.gson.Gson;
import com.yandex.taskmanager.web.dto.TaskDto;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonSyntaxException;

public class BaseHttpHandler {
    protected void sendText(HttpExchange exchange, String text) throws IOException {
        sendResponseWithJson(exchange, text, 200);
    }

    protected void sendOk(HttpExchange exchange, String text) throws IOException {
        sendResponseWithText(exchange, text, 200);
    }

    protected void sendCreated(HttpExchange exchange, String text) throws IOException {
        sendResponseWithJson(exchange, text, 201);
    }

    protected void sendInvalidPathFormat(HttpExchange exchange, String text) throws IOException {
        sendResponseWithText(exchange, text, 400);
    }

    protected void sendNotFound(HttpExchange exchange, String text) throws IOException {
        sendResponseWithText(exchange, text, 404);
    }

    protected void sendMethodNotAllowed(HttpExchange exchange, String text) throws IOException {
        sendResponseWithText(exchange, text, 405);
    }

    protected void sendHasInteractions(HttpExchange exchange, String text) throws IOException {
        sendResponseWithText(exchange, text, 406);
    }

    protected void sendInternalServerError(HttpExchange exchange) throws IOException {
        sendResponseWithText(exchange, "Internal server error.", 500);
    }

    protected TaskDto readDto(HttpExchange httpExchange, Gson gson) throws IOException {
        try {
            InputStream is = httpExchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return gson.fromJson(body, TaskDto.class);
        } catch (JsonSyntaxException ex) {
            throw new IllegalArgumentException("Invalid JSON format.", ex);
        }
    }

    protected Integer parseId(String[] parts) {
        try {
            return Integer.parseInt(parts[2]);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid id format.");
        }
    }

    private void sendResponseWithJson(HttpExchange exchange, String text, int code) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        exchange.sendResponseHeaders(code, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }

    private void sendResponseWithText(HttpExchange exchange, String text, int code) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain;charset=utf-8");
        exchange.sendResponseHeaders(code, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }
}
