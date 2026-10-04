package com.example.carservice.exception;

public class MechanicNotCompatibleException extends DomainException {

    private static final long serialVersionUID = 1L;

    public MechanicNotCompatibleException(String message) {
        super(message);
    }
}
