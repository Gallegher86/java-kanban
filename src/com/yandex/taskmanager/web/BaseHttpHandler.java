package com.yandex.taskmanager.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class BaseHttpHandler {
    protected void sendText(HttpExchange exchange, String text) throws IOException {
        sendResponseWithJson(exchange, text, 200);
    }

    protected void sendCreated(HttpExchange exchange, String text) throws IOException {
        sendResponseWithJson(exchange, text, 201);
    }

    protected void sendNotFound(HttpExchange exchange, String text) throws IOException {
        sendResponseWithText(exchange, text, 404);
    }

    protected void sendHasInteractions(HttpExchange exchange, String text) throws IOException {
        sendResponseWithText(exchange, text, 409);
    }

    protected void sendMethodNotAllowed(HttpExchange exchange, String text) throws IOException {
        sendResponseWithText(exchange, text, 405);
    }

    protected void sendInvalidPathFormat(HttpExchange exchange, String text) throws IOException {
        sendResponseWithText(exchange, text, 400);
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
