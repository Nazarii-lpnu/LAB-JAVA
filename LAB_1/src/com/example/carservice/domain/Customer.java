package com.example.carservice.domain;

import com.example.carservice.exception.InvalidArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Customer {

    private UUID id;
    private String name;
    private List<Car> cars;

    public Customer(UUID id, String name) {
        if (id == null) {
            throw new InvalidArgumentException("Customer id cannot be null.");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidArgumentException("Customer name cannot be null, empty, or blank.");
        }

        this.id = id;
        this.name = name.trim();
        this.cars = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void addCar(Car car) {
        if (car == null) {
            throw new InvalidArgumentException("Car cannot be null.");
        }
        if (!this.equals(car.getOwner())) {
            throw new InvalidArgumentException("Car owner must be this customer.");
        }
        if (cars.stream().anyMatch(existingCar -> existingCar.getVin().equals(car.getVin()))) {
            throw new InvalidArgumentException("A car with VIN '" + car.getVin() + "' is already owned by this customer.");
        }
        cars.add(car);
    }
}
