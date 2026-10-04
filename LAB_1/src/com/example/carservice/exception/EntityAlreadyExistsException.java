package com.example.carservice.exception;

public class EntityAlreadyExistsException extends DomainException {

    private static final long serialVersionUID = 1L;

    public EntityAlreadyExistsException(String message) {
        super(message);
    }
}
