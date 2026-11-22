package com.example.carservice.exception;

public class OrderModificationNotAllowedException extends DomainException {
    public OrderModificationNotAllowedException(String message) {
        super(message);
    }
}
