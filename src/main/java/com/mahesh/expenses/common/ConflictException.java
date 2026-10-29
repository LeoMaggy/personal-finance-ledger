package com.mahesh.expenses.common;

/** The request clashes with existing data, e.g. a duplicate name. Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
