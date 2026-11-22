package com.example.carservice.exception;

public class MechanicAlreadyAssignedException extends DomainException {
    public MechanicAlreadyAssignedException(String message) {
        super(message);
    }
}
