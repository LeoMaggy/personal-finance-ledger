package com.mahesh.expenses.common;

/** The resource doesn't exist, or belongs to another user. Mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
