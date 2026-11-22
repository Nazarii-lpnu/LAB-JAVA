package com.example.carservice.service;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.repository.CarRepository;
import com.example.carservice.service.impl.CarServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CarServiceTest {
    @Test
    void registersCarForExistingCustomer() {
        Customer customer = new Customer("Owner");
        CarRepository carRepository = mock(CarRepository.class);
        CustomerService customerService = mock(CustomerService.class);
        when(customerService.findCustomerById(10)).thenReturn(customer);
        when(carRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Car.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CarService service = new CarServiceImpl(carRepository, customerService);

        Car car = service.registerCar("VIN-1", "Brand", "Model", 2020, 10);

        assertEquals("VIN-1", car.getVin());
        verify(carRepository).existsByVin("VIN-1");
        verify(carRepository).saveAndFlush(car);
    }

    @Test
    void trimsVinWhenLookingUpCar() {
        Customer customer = new Customer("Owner");
        Car car = new Car("VIN-1", "Brand", "Model", 2020, customer);
        CarRepository carRepository = mock(CarRepository.class);
        when(carRepository.findByVin("VIN-1")).thenReturn(Optional.of(car));
        CarService service = new CarServiceImpl(carRepository, mock(CustomerService.class));

        assertEquals(car, service.findCarByVin("  VIN-1  "));
        verify(carRepository).findByVin("VIN-1");
    }
}
