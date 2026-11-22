package com.example.carservice.domain;

import com.example.carservice.exception.InvalidArgumentException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private List<Car> cars = new ArrayList<>();

    protected Customer() {
    }

    public Customer(String name) {
        if (Objects.isNull(name) || name.isBlank()) {
            throw new InvalidArgumentException("Customer name cannot be null, empty, or blank.");
        }
        this.name = name.trim();
    }

    public Long getId() { return id; }

    public void addCar(Car car) {
        if (Objects.isNull(car)) throw new InvalidArgumentException("Car cannot be null.");
        if (Objects.isNull(car.getOwner()) || !Objects.equals(id, car.getOwner().getId())) {
            throw new InvalidArgumentException("Car owner must be this customer.");
        }
        if (cars.stream().anyMatch(existing -> Objects.equals(existing.getVin(), car.getVin()))) {
            throw new InvalidArgumentException("A car with VIN '" + car.getVin() + "' is already owned by this customer.");
        }
        cars.add(car);
    }

    public List<Car> getCars() { return List.copyOf(cars); }
}
