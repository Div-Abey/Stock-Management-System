package com.stockmanagement.common.exception;

/** Thrown when user input or a business rule is violated. The message is safe to show to the user. */
public class ValidationException extends Exception {
    public ValidationException(String message) {
        super(message);
    }
}
