package com.example.carservice.service;

import com.example.carservice.domain.Car;
public interface CarService {
    Car registerCar(String vin, String brand, String model, int productionYear, long customerId);
    Car findCarByVin(String vin);
}
