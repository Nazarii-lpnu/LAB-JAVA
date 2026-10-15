package com.example.carservice.app;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.MechanicSpecialization;
import com.example.carservice.domain.ServiceOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.exception.DomainException;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.MechanicAlreadyAssignedException;
import com.example.carservice.exception.OrderModificationNotAllowedException;
import com.example.carservice.exception.ScheduleConflictException;
import com.example.carservice.service.CarService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Console entry point for the automotive service application.
 */
public class CarServiceDemo {



    public static void main(String[] args) {
        CarService carService = new CarService();

        printHeader("AUTOMOTIVE SERVICE DOMAIN DEMO");

        Customer customer = carService.registerCustomer("Oleh Melnyk");
        System.out.println("Registered customer: " + customer.getName()
                + " (" + customer.getId() + ")");

        Car car = carService.registerCar(
                "VIN-DEMO-001",
                "Toyota",
                "Corolla",
                2021,
                customer.getId());
        System.out.println("Registered car: " + car.getBrand() + " " + car.getModel()
                + ", VIN " + car.getVin());

        Mechanic generalMechanic = carService.registerMechanic(
                "Andrii Melnyk",
                MechanicSpecialization.GENERAL);
        Mechanic engineMechanic = carService.registerMechanic(
                "Ihor Bondar",
                MechanicSpecialization.ENGINE);
        Mechanic schedulingMechanic = carService.registerMechanic(
                "Maksym Shevchenko",
                MechanicSpecialization.ENGINE);
        System.out.println("Registered mechanics: " + generalMechanic.getName()
                + " (" + generalMechanic.getSpecialization() + "), "
                + engineMechanic.getName() + " (" + engineMechanic.getSpecialization() + ")");

        ServiceOrder completedOrder = demonstratePositiveScenario(
                carService,
                car,
                generalMechanic);
        demonstrateNegativeScenarios(
                carService,
                car,
                engineMechanic,
                schedulingMechanic,
                completedOrder);

        printHeader("DEMO FINISHED");
    }

    private static ServiceOrder demonstratePositiveScenario(
            CarService carService,
            Car car,
            Mechanic mechanic) {
        printHeader("POSITIVE SCENARIO");

        ServiceOrder order = carService.createOrder(car.getVin());
        System.out.println("Created order " + order.getId()
                + " with status " + order.getStatus());

        ServiceWork diagnostics = new ServiceWork(
                UUID.randomUUID(),
                "Computer diagnostics",
                MechanicSpecialization.DIAGNOSTICS,
                new BigDecimal("25.00"),
                new BigDecimal("40.00"),
                Duration.ofHours(1));
        ServiceWork engineRepair = new ServiceWork(
                UUID.randomUUID(),
                "Engine oil and filter replacement",
                MechanicSpecialization.ENGINE,
                new BigDecimal("60.00"),
                new BigDecimal("80.00"),
                Duration.ofHours(2));

        carService.addWorkToOrder(order.getId(), diagnostics);
        System.out.println("Added work: " + diagnostics.getDescription());
        carService.addWorkToOrder(order.getId(), engineRepair);
        System.out.println("Added work: " + engineRepair.getDescription());

        carService.planOrder(
                order.getId(),
                LocalDateTime.of(2026, 10, 1, 9, 0),
                Duration.ofHours(3));
        System.out.println("Planned order: 09:00-12:00");

        carService.diagnoseOrder(order.getId());
        printStatus(order, "Diagnosed order");

        carService.approveOrder(order.getId());
        printStatus(order, "Approved order");

        carService.assignMechanic(order.getId(), mechanic.getId());
        System.out.println("Assigned mechanic: " + mechanic.getName());

        carService.startOrder(order.getId());
        printStatus(order, "Started order");

        carService.completeWork(order.getId(), diagnostics.getId());
        System.out.println("Completed work: " + diagnostics.getDescription());
        carService.completeWork(order.getId(), engineRepair.getId());
        System.out.println("Completed work: " + engineRepair.getDescription());

        carService.completeOrder(order.getId());
        printStatus(order, "Completed order");
        System.out.println("Final total cost: " + order.calculateTotalCost());
        order.getGift().ifPresentOrElse(
                gift -> System.out.println("Gift awarded: " + gift.getDescription()
                        + " (" + gift.getType() + ")"),
                () -> System.out.println("No gift awarded for this order."));

        return order;
    }

    private static void demonstrateNegativeScenarios(
            CarService carService,
            Car car,
            Mechanic engineMechanic,
            Mechanic schedulingMechanic,
            ServiceOrder completedOrder) {
        printHeader("NEGATIVE SCENARIOS");

        ServiceOrder emptyOrder = carService.createOrder(car.getVin());
        System.out.println("1. Testing approval of an empty order");
        try {
            carService.diagnoseOrder(emptyOrder.getId());
            carService.approveOrder(emptyOrder.getId());
        } catch (InvalidStateTransitionException exception) {
            printExpectedFailure(exception);
        }

        ServiceOrder orderWithoutMechanic = carService.createOrder(car.getVin());
        ServiceWork engineWork = createWork(
                "Engine inspection",
                MechanicSpecialization.ENGINE,
                "20.00",
                "30.00");
        carService.addWorkToOrder(orderWithoutMechanic.getId(), engineWork);
        carService.diagnoseOrder(orderWithoutMechanic.getId());
        carService.approveOrder(orderWithoutMechanic.getId());

        System.out.println("2. Testing start without an assigned mechanic");
        try {
            carService.startOrder(orderWithoutMechanic.getId());
        } catch (InvalidStateTransitionException exception) {
            printExpectedFailure(exception);
        }

        ServiceOrder activeOrder = carService.createOrder(car.getVin());
        carService.addWorkToOrder(activeOrder.getId(), createWork(
                "Active engine repair",
                MechanicSpecialization.ENGINE,
                "10.00",
                "50.00"));
        carService.assignMechanic(activeOrder.getId(), engineMechanic.getId());
        carService.planOrder(
                activeOrder.getId(),
                LocalDateTime.of(2026, 10, 2, 10, 0),
                Duration.ofHours(2));
        carService.diagnoseOrder(activeOrder.getId());
        carService.approveOrder(activeOrder.getId());
        carService.startOrder(activeOrder.getId());

        ServiceOrder overlappingOrder = carService.createOrder(car.getVin());
        carService.addWorkToOrder(overlappingOrder.getId(), createWork(
                "Overlapping engine repair",
                MechanicSpecialization.ENGINE,
                "10.00",
                "50.00"));

        System.out.println("3. Testing assignment to a mechanic already working on an active order");
        try {
            carService.assignMechanic(overlappingOrder.getId(), engineMechanic.getId());
        } catch (MechanicAlreadyAssignedException exception) {
            printExpectedFailure(exception);
        }

        ServiceOrder scheduledOrder = carService.createOrder(car.getVin());
        carService.addWorkToOrder(scheduledOrder.getId(), createWork(
                "Scheduled engine repair",
                MechanicSpecialization.ENGINE,
                "10.00",
                "50.00"));
        carService.assignMechanic(scheduledOrder.getId(), schedulingMechanic.getId());
        carService.planOrder(
                scheduledOrder.getId(),
                LocalDateTime.of(2026, 10, 3, 10, 0),
                Duration.ofHours(2));

        ServiceOrder overlappingScheduledOrder = carService.createOrder(car.getVin());
        carService.addWorkToOrder(overlappingScheduledOrder.getId(), createWork(
                "Overlapping scheduled repair",
                MechanicSpecialization.ENGINE,
                "10.00",
                "50.00"));
        carService.planOrder(
                overlappingScheduledOrder.getId(),
                LocalDateTime.of(2026, 10, 3, 11, 0),
                Duration.ofHours(2));

        System.out.println("4. Testing overlapping mechanic schedule");
        try {
            carService.assignMechanic(
                    overlappingScheduledOrder.getId(),
                    schedulingMechanic.getId());
        } catch (ScheduleConflictException exception) {
            printExpectedFailure(exception);
        }

        System.out.println("5. Testing modification of a completed order");
        try {
            carService.addWorkToOrder(completedOrder.getId(), createWork(
                    "Forbidden completed-order work",
                    MechanicSpecialization.GENERAL,
                    "1.00",
                    "1.00"));
        } catch (OrderModificationNotAllowedException exception) {
            printExpectedFailure(exception);
        }

        System.out.println("6. Testing an invalid status transition");
        try {
            carService.diagnoseOrder(completedOrder.getId());
        } catch (InvalidStateTransitionException exception) {
            printExpectedFailure(exception);
        }

        System.out.println("7. Testing duplicate VIN registration");
        try {
            carService.registerCar(
                    car.getVin(),
                    "Honda",
                    "Civic",
                    2022,
                    car.getOwner().getId());
        } catch (EntityAlreadyExistsException exception) {
            printExpectedFailure(exception);
        }

        System.out.println("8. Testing invalid monetary value");
        try {
            createWork(
                    "Invalid cost work",
                    MechanicSpecialization.GENERAL,
                    "-5.00",
                    "10.00");
        } catch (InvalidArgumentException exception) {
            printExpectedFailure(exception);
        }
    }

    private static ServiceWork createWork(
            String description,
            MechanicSpecialization specialization,
            String partsCost,
            String laborCost) {
        return new ServiceWork(
                UUID.randomUUID(),
                description,
                specialization,
                new BigDecimal(partsCost),
                new BigDecimal(laborCost),
                Duration.ofHours(1));
    }

    private static void printStatus(ServiceOrder order, String message) {
        System.out.println(message + ": " + order.getStatus());
    }

    private static void printExpectedFailure(DomainException exception) {
        System.out.println("Expected failure: " + exception.getMessage());
    }

    private static void printHeader(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
