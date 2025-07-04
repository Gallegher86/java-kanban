package com.yandex.taskmanager.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BaseHttpHandler {
    protected void sendText(HttpExchange exchange, String text) throws IOException {
        sendResponse(exchange, text, 200);
    }

    protected void sendNotFound(HttpExchange exchange, String text) throws IOException {
        sendResponse(exchange, text, 404);
    }

    protected void sendHasInteractions(HttpExchange exchange, String text) throws IOException {
        sendResponse(exchange, text, 409);
    }

    private void sendResponse(HttpExchange exchange, String text, int code) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        exchange.sendResponseHeaders(code, resp.length);
        exchange.getResponseBody().write(resp);
        exchange.close();
    }
}
