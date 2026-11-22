package com.example.carservice.exception;

public class InvalidArgumentException extends DomainException {
    public InvalidArgumentException(String message) {
        super(message);
    }
}
