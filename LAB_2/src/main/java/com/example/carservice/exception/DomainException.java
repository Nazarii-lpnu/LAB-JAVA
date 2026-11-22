package com.example.carservice.exception;

public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
