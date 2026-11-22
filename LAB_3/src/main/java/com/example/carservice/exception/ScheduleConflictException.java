package com.example.carservice.exception;

public class ScheduleConflictException extends DomainException {
    public ScheduleConflictException(String message) {
        super(message);
    }
}
