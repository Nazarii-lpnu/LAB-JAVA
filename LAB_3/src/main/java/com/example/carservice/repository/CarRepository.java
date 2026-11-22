package com.example.carservice.repository;

import com.example.carservice.domain.Car;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, String> {
    Optional<Car> findByVin(String vin);

    boolean existsByVin(String vin);

}
