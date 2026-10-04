package com.example.carservice.exception;

public class OrderModificationNotAllowedException extends DomainException {

    private static final long serialVersionUID = 1L;

    public OrderModificationNotAllowedException(String message) {
        super(message);
    }
}
