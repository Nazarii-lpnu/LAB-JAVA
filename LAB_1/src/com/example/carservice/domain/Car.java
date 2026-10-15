package com.example.carservice.domain;

import com.example.carservice.exception.InvalidArgumentException;

public class Car {

    private static final int FIRST_AUTOMOTIVE_PRODUCTION_YEAR = 1886;

    private String vin;
    private String brand;
    private String model;
    private int productionYear;
    private Customer owner;

    public Car(String vin, String brand, String model, int productionYear, Customer owner) {
        if (vin == null || vin.isBlank()) {
            throw new InvalidArgumentException("VIN cannot be null, empty, or blank.");
        }
        if (brand == null || brand.isBlank()) {
            throw new InvalidArgumentException("Car brand cannot be null, empty, or blank.");
        }
        if (model == null || model.isBlank()) {
            throw new InvalidArgumentException("Car model cannot be null, empty, or blank.");
        }
        if (productionYear < FIRST_AUTOMOTIVE_PRODUCTION_YEAR
                || productionYear > java.time.Year.now().getValue()) {
            throw new InvalidArgumentException("Production year is outside the valid range.");
        }
        if (owner == null) {
            throw new InvalidArgumentException("Car owner cannot be null.");
        }

        this.vin = vin.trim();
        this.brand = brand.trim();
        this.model = model.trim();
        this.productionYear = productionYear;
        this.owner = owner;
    }

    public String getVin() {
        return vin;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public Customer getOwner() {
        return owner;
    }
}
