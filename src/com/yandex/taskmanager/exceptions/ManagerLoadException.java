package com.yandex.taskmanager.exceptions;

public class ManagerLoadException extends RuntimeException {
    public ManagerLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}