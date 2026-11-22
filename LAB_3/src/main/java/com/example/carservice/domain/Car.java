package com.example.carservice.domain;

import com.example.carservice.exception.InvalidArgumentException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Year;
import java.util.Objects;

@Getter
@Entity
@Table(name = "cars")
public class Car {
    private static final int FIRST_AUTOMOTIVE_PRODUCTION_YEAR = 1886;
    @Id
    @Column(name = "vin", nullable = false, updatable = false, length = 64)
    private String vin;

    @Column(name = "brand", nullable = false, length = 100)
    private String brand;
    @Column(name = "model", nullable = false, length = 100)
    private String model;
    @Column(name = "production_year", nullable = false)
    private int productionYear;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer owner;

    protected Car() {
    }

    public Car(String vin, String brand, String model, int productionYear, Customer owner) {
        if (Objects.isNull(vin) || vin.isBlank())
            throw new InvalidArgumentException("VIN cannot be null, empty, or blank.");
        if (Objects.isNull(brand) || brand.isBlank())
            throw new InvalidArgumentException("Car brand cannot be null, empty, or blank.");
        if (Objects.isNull(model) || model.isBlank())
            throw new InvalidArgumentException("Car model cannot be null, empty, or blank.");
        if (productionYear < FIRST_AUTOMOTIVE_PRODUCTION_YEAR || productionYear > Year.now().getValue()) {
            throw new InvalidArgumentException("Production year is outside the valid range.");
        }
        if (Objects.isNull(owner)) throw new InvalidArgumentException("Car owner cannot be null.");
        this.vin = vin.trim();
        this.brand = brand.trim();
        this.model = model.trim();
        this.productionYear = productionYear;
        this.owner = owner;
    }

}
