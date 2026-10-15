package com.example.carservice.service;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.MechanicSpecialization;
import com.example.carservice.domain.OrderStatus;
import com.example.carservice.domain.ServiceOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.TimeSlot;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.MechanicAlreadyAssignedException;
import com.example.carservice.exception.ScheduleConflictException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Application-facing coordinator and registry for automotive service entities.
 */
public class CarService {

    private Map<UUID, Customer> customers;
    private Map<String, Car> carsByVin;
    private Map<UUID, Mechanic> mechanics;
    private Map<UUID, ServiceOrder> orders;

    public CarService() {
        this.customers = new HashMap<>();
        this.carsByVin = new HashMap<>();
        this.mechanics = new HashMap<>();
        this.orders = new HashMap<>();
    }

    public Customer registerCustomer(String name) {
        Customer customer = new Customer(UUID.randomUUID(), name);
        customers.put(customer.getId(), customer);
        return customer;
    }

    public Car registerCar(
            String vin,
            String brand,
            String model,
            int productionYear,
            UUID customerId) {
        Customer customer = findCustomerById(customerId);
        Car car = new Car(vin, brand, model, productionYear, customer);
        String normalizedVin = car.getVin();

        if (carsByVin.containsKey(normalizedVin)) {
            throw new EntityAlreadyExistsException(
                    "A car with VIN '" + normalizedVin + "' is already registered.");
        }

        customer.addCar(car);
        carsByVin.put(normalizedVin, car);
        return car;
    }

    public Mechanic registerMechanic(String name, MechanicSpecialization specialization) {
        Mechanic mechanic = new Mechanic(UUID.randomUUID(), name, specialization);
        mechanics.put(mechanic.getId(), mechanic);
        return mechanic;
    }

    public ServiceOrder createOrder(String vin) {
        Car car = findCarByVin(vin);
        ServiceOrder order = new ServiceOrder(UUID.randomUUID(), car);
        orders.put(order.getId(), order);
        return order;
    }

    public Car findCarByVin(String vin) {
        if (vin == null || vin.isBlank()) {
            throw new InvalidArgumentException("VIN cannot be null, empty, or blank.");
        }

        String normalizedVin = vin.trim();
        Car car = carsByVin.get(normalizedVin);
        if (car == null) {
            throw new EntityNotFoundException(
                    "No registered car was found for VIN '" + normalizedVin + "'.");
        }
        return car;
    }

    public ServiceOrder findOrderById(UUID orderId) {
        if (orderId == null) {
            throw new InvalidArgumentException("Order id cannot be null.");
        }

        ServiceOrder order = orders.get(orderId);
        if (order == null) {
            throw new EntityNotFoundException(
                    "No service order was found for id '" + orderId + "'.");
        }
        return order;
    }

    public void addWorkToOrder(UUID orderId, ServiceWork work) {
        findOrderById(orderId).addWork(work);
    }

    public void assignMechanic(UUID orderId, UUID mechanicId) {
        ServiceOrder order = findOrderById(orderId);
        Mechanic mechanic = findMechanicById(mechanicId);

        ensureMechanicAvailable(order, mechanic);
        order.assignMechanic(mechanic);
    }

    public void planOrder(UUID orderId, LocalDateTime start, Duration duration) {
        ServiceOrder order = findOrderById(orderId);
        TimeSlot plannedSlot = new TimeSlot(start, duration);
        ensureScheduleAvailable(order, plannedSlot);
        order.plan(plannedSlot);
    }

    public void diagnoseOrder(UUID orderId) {
        findOrderById(orderId).diagnose();
    }

    public void approveOrder(UUID orderId) {
        findOrderById(orderId).approve();
    }

    public void startOrder(UUID orderId) {
        ServiceOrder order = findOrderById(orderId);
        ensureMechanicAvailable(order, order.getMechanic());
        order.start();
    }

    public void completeWork(UUID orderId, UUID workId) {
        findOrderById(orderId).completeWork(workId);
    }

    public void completeOrder(UUID orderId) {
        findOrderById(orderId).complete();
    }



    private Customer findCustomerById(UUID customerId) {
        if (customerId == null) {
            throw new InvalidArgumentException("Customer id cannot be null.");
        }

        Customer customer = customers.get(customerId);
        if (customer == null) {
            throw new EntityNotFoundException(
                    "No registered customer was found for id '" + customerId + "'.");
        }
        return customer;
    }

    private Mechanic findMechanicById(UUID mechanicId) {
        if (mechanicId == null) {
            throw new InvalidArgumentException("Mechanic id cannot be null.");
        }

        Mechanic mechanic = mechanics.get(mechanicId);
        if (mechanic == null) {
            throw new EntityNotFoundException(
                    "No registered mechanic was found for id '" + mechanicId + "'.");
        }
        return mechanic;
    }

    private void ensureMechanicAvailable(ServiceOrder targetOrder, Mechanic mechanic) {
        if (mechanic == null) {
            return;
        }

        List<ServiceOrder> otherActiveOrders = orders.values()
                .stream()
                .filter(order -> !order.equals(targetOrder))
                .filter(this::isActive)
                .filter(order -> mechanic.equals(order.getMechanic()))
                .toList();

        for (ServiceOrder otherOrder : otherActiveOrders) {
            if (targetOrder.getPlannedSlot() == null || otherOrder.getPlannedSlot() == null) {
                throw new MechanicAlreadyAssignedException(
                        "Mechanic '" + mechanic.getId()
                                + "' is already assigned to another active order "
                                + "without enough scheduling information to prove availability.");
            }
        }

        if (targetOrder.getPlannedSlot() != null) {
            ensureScheduleAvailable(targetOrder, targetOrder.getPlannedSlot(), mechanic);
        }
    }

    private void ensureScheduleAvailable(ServiceOrder targetOrder, TimeSlot plannedSlot) {
        if (targetOrder.getMechanic() != null) {
            ensureScheduleAvailable(targetOrder, plannedSlot, targetOrder.getMechanic());
        }
    }

    private void ensureScheduleAvailable(
            ServiceOrder targetOrder,
            TimeSlot plannedSlot,
            Mechanic mechanic) {
        List<ServiceOrder> otherActiveOrders = orders.values().stream()
                .filter(order -> !order.equals(targetOrder))
                .filter(this::isActive)
                .filter(order -> mechanic.equals(order.getMechanic()))
                .toList();

        boolean unplannedOrderExists = otherActiveOrders.stream()
                .anyMatch(order -> order.getPlannedSlot() == null);
        if (unplannedOrderExists) {
            throw new MechanicAlreadyAssignedException(
                    "Mechanic '" + mechanic.getId()
                            + "' is already assigned to another active order without a planned slot.");
        }

        boolean conflictingOrderExists = otherActiveOrders.stream()
                .map(ServiceOrder::getPlannedSlot)
                .anyMatch(existingSlot -> existingSlot.overlaps(plannedSlot));

        if (conflictingOrderExists) {
            throw new ScheduleConflictException(
                    "The planned time slot conflicts with another order for mechanic '"
                            + mechanic.getId() + "'.");
        }
    }

    private boolean isActive(ServiceOrder order) {
        return !OrderStatus.COMPLETED.equals(order.getStatus())
                && !OrderStatus.CANCELLED.equals(order.getStatus());
    }
}
