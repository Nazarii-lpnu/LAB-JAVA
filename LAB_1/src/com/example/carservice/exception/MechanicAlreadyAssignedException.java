package com.example.carservice.exception;

public class MechanicAlreadyAssignedException extends DomainException {

    private static final long serialVersionUID = 1L;

    public MechanicAlreadyAssignedException(String message) {
        super(message);
    }
}
