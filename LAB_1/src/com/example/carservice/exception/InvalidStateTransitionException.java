package com.example.carservice.exception;

public class InvalidStateTransitionException extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
