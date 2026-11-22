package com.example.carservice.service;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.MechanicSpecialization;
import com.example.carservice.domain.OrderPriority;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidArgumentException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.Duration;
import java.time.LocalDateTime;

public class CarService {
    private RepairOrderService repairOrderService;
    private Map<UUID, Customer> customers = new HashMap<>();
    private Map<String, Car> carsByVin = new HashMap<>();
    private Map<UUID, Mechanic> mechanics = new HashMap<>();

    public CarService(RepairOrderService repairOrderService) {
        if (repairOrderService == null) throw new InvalidArgumentException("Repair order service cannot be null.");
        this.repairOrderService = repairOrderService;
    }

    public Customer registerCustomer(String name) {
        Customer customer = new Customer(UUID.randomUUID(), name);
        customers.put(customer.getId(), customer);
        return customer;
    }

    public Car registerCar(String vin, String brand, String model, int productionYear, UUID customerId) {
        Customer customer = findCustomer(customerId);
        Car car = new Car(vin, brand, model, productionYear, customer);
        if (carsByVin.containsKey(car.getVin())) {
            throw new EntityAlreadyExistsException("A car with VIN '" + car.getVin() + "' is already registered.");
        }
        customer.addCar(car);
        carsByVin.put(car.getVin(), car);
        return car;
    }

    public Mechanic registerMechanic(String name, MechanicSpecialization specialization) {
        Mechanic mechanic = new Mechanic(UUID.randomUUID(), name, specialization);
        mechanics.put(mechanic.getId(), mechanic);
        return mechanic;
    }

    public RepairOrder createOrder(String vin) {
        return createOrder(vin, OrderPriority.NORMAL);
    }

    public RepairOrder createOrder(String vin, OrderPriority priority) {
        Car car = findCarByVin(vin);
        return repairOrderService.create(new RepairOrder(UUID.randomUUID(), car, priority));
    }

    public Car findCarByVin(String vin) {
        if (vin == null || vin.isBlank()) throw new InvalidArgumentException("VIN cannot be null, empty, or blank.");
        Car car = carsByVin.get(vin.trim());
        if (car == null) throw new EntityNotFoundException("No registered car was found for VIN '" + vin.trim() + "'.");
        return car;
    }

    public Mechanic findMechanicById(UUID mechanicId) {
        if (mechanicId == null) throw new InvalidArgumentException("Mechanic id cannot be null.");
        Mechanic mechanic = mechanics.get(mechanicId);
        if (mechanic == null) throw new EntityNotFoundException("No registered mechanic was found for id '" + mechanicId + "'.");
        return mechanic;
    }

    public RepairOrder findOrderById(UUID orderId) {
        return repairOrderService.getById(orderId);
    }

    public void addWorkToOrder(UUID orderId, ServiceWork work) {
        repairOrderService.addWork(orderId, work);
    }


    public void assignMechanic(UUID orderId, UUID mechanicId) {
        repairOrderService.assignMechanic(orderId, findMechanicById(mechanicId));
    }



    public void diagnoseOrder(UUID orderId) {
        repairOrderService.diagnose(orderId);
    }

    public void approveOrder(UUID orderId) {
        repairOrderService.approve(orderId);
    }

    public void startOrder(UUID orderId) {
        repairOrderService.start(orderId);
    }

    public void completeWork(UUID orderId, UUID workId) {
        repairOrderService.completeWork(orderId, workId);
    }

    public void completeOrder(UUID orderId) {
        repairOrderService.complete(orderId);
    }


    private Customer findCustomer(UUID customerId) {
        if (customerId == null) throw new InvalidArgumentException("Customer id cannot be null.");
        Customer customer = customers.get(customerId);
        if (customer == null) throw new EntityNotFoundException("No registered customer was found for id '" + customerId + "'.");
        return customer;
    }
}
