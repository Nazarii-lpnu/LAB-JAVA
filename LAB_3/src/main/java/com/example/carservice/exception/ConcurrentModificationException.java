package com.example.carservice.exception;

public class ConcurrentModificationException extends DomainException {
    public ConcurrentModificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
