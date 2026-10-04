package com.example.carservice.exception;

public class ScheduleConflictException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ScheduleConflictException(String message) {
        super(message);
    }
}
