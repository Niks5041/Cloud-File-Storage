package ru.anikson.cloudfilestorage.exception;

public class ErrorResponse {
    private final String error;
    private final Object description;

    public ErrorResponse(String error, Object description) {
        this.error = error;
        this.description = description;
    }

    public String getError() {
        return error;
    }

    public Object getDescription() {
        return description;
    }
}