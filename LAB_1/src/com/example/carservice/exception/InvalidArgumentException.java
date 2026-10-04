package com.example.carservice.exception;

public class InvalidArgumentException extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvalidArgumentException(String message) {
        super(message);
    }
}
