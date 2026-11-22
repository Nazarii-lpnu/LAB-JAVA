package com.example.carservice.service.impl;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.repository.CarRepository;
import com.example.carservice.service.CarService;
import com.example.carservice.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@RequiredArgsConstructor
@Service
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final CustomerService customerService;

    @Override
    @Transactional
    public Car registerCar(String vin, String brand, String model, int productionYear, long customerId) {
        Customer customer = customerService.findCustomerById(customerId);
        Car car = new Car(vin, brand, model, productionYear, customer);
        if (carRepository.existsByVin(car.getVin())) {
            throw new EntityAlreadyExistsException("A car with VIN '" + car.getVin() + "' is already registered.");
        }
        try {
            return carRepository.saveAndFlush(car);
        } catch (DataIntegrityViolationException exception) {
            throw new EntityAlreadyExistsException("A car with VIN '" + car.getVin() + "' is already registered.");
        }
    }

    @Override
    public Car findCarByVin(String vin) {
        if (Objects.isNull(vin) || vin.isBlank())
            throw new InvalidArgumentException("VIN cannot be null, empty, or blank.");
        return carRepository.findByVin(vin.trim()).orElseThrow(() ->
                new EntityNotFoundException("No registered car was found for VIN '" + vin.trim() + "'."));
    }
}
