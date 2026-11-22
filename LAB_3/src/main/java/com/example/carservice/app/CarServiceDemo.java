package com.example.carservice.app;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.domain.enumeration.OrderStatus;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.exception.DomainException;
import com.example.carservice.exception.ConcurrentModificationException;
import com.example.carservice.service.CarService;
import com.example.carservice.service.CustomerService;
import com.example.carservice.service.MechanicService;
import com.example.carservice.service.RepairOrderCommandService;
import com.example.carservice.service.RepairOrderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true", matchIfMissing = true)
public class CarServiceDemo implements CommandLineRunner {
    private static final String VIN = "VIN-DEMO-001";
    private final CustomerService customerService;
    private final CarService carService;
    private final MechanicService mechanicService;
    private final RepairOrderCommandService repairOrderCommandService;
    private final RepairOrderService repairOrderService;

    public CarServiceDemo(CustomerService customerService,
                          CarService carService,
                          MechanicService mechanicService,
                          RepairOrderCommandService repairOrderCommandService,
                          RepairOrderService repairOrderService) {
        this.customerService = customerService;
        this.carService = carService;
        this.mechanicService = mechanicService;
        this.repairOrderCommandService = repairOrderCommandService;
        this.repairOrderService = repairOrderService;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== Car service demo ===");
        List<RepairOrder> existingOrders = repairOrderService.findByCarVin(VIN);
        RepairOrder order;

        if (existingOrders.isEmpty()) {
            System.out.println("[A] Creating customer, car, mechanic, order and works");
            order = createOrder();
            runBatchRollbackScenario(order);
        } else {
            System.out.println("[B] Loading persisted order after restart by VIN " + VIN);
            order = existingOrders.get(0);
            showPersistedOrder(order);
        }

        if (!OrderStatus.COMPLETED.equals(order.getStatus())) {
            completeOrder(order);
        } else {
            System.out.println("[C] Order is already completed; no duplicate completion is required");
        }

        System.out.println("[D] Invalid operation on completed order");
        demonstrateInvalidOperation(order);

        if (OrderStatus.COMPLETED.equals(order.getStatus()) && !existingOrders.isEmpty()) {
            System.out.println("[E] Batch scenario already applied; persisted work count is "
                    + repairOrderService.getById(order.getId()).getWorks().size());
        }

        demonstrateConcurrentModification(order);
    }

    private RepairOrder createOrder() {
        Customer customer = customerService.registerCustomer("Oleh Melnyk");
        Car car = carService.registerCar(VIN, "Toyota", "Corolla", 2021, customer.getId());
        Mechanic mechanic = mechanicService.registerMechanic("Andrii Melnyk", MechanicSpecialization.GENERAL);
        RepairOrder order = repairOrderCommandService.createOrder(car.getVin());
        ServiceWork diagnostics = work("Computer diagnostics", MechanicSpecialization.DIAGNOSTICS,
                "25.00", "40.00", 1);
        ServiceWork engineRepair = work("Engine oil and filter replacement",
                MechanicSpecialization.ENGINE, "60.00", "80.00", 2);

        repairOrderCommandService.addWorksToOrder(order.getId(), List.of(diagnostics, engineRepair));
        repairOrderService.assignMechanic(order.getId(), mechanic);
        RepairOrder persisted = repairOrderService.getById(order.getId());
        System.out.println("Created order " + persisted.getId() + " for mechanic " + mechanic.getId());
        showPersistedOrder(persisted);
        return persisted;
    }

    private void completeOrder(RepairOrder order) {
        System.out.println("[C] Completing order " + order.getId());
        repairOrderService.plan(order.getId(),
                Instant.parse("2026-10-01T09:00:00Z"), Duration.ofHours(3));
        repairOrderService.diagnose(order.getId());
        repairOrderService.approve(order.getId());
        repairOrderService.start(order.getId());

        order.getWorks().forEach(work -> repairOrderService.completeWork(order.getId(), work.getId()));
        repairOrderService.complete(order.getId());
        showPersistedOrder(repairOrderService.getById(order.getId()));
    }

    private void runBatchRollbackScenario(RepairOrder order) {
        System.out.println("[E] Batch addWorks with one invalid element");
        int before = repairOrderService.getById(order.getId()).getWorks().size();
        List<ServiceWork> batch = new ArrayList<>();
        batch.add(work("Temporary valid work", MechanicSpecialization.ENGINE, "1.00", "1.00", 1));
        batch.add(null);

        try {
            repairOrderCommandService.addWorksToOrder(order.getId(), batch);
        } catch (DomainException exception) {
            int after = repairOrderService.getById(order.getId()).getWorks().size();
            System.out.println("Expected batch failure: " + exception.getMessage());
            System.out.println("Works before: " + before + ", after: " + after);
        }
    }

    private void demonstrateInvalidOperation(RepairOrder order) {
        try {
            repairOrderCommandService.addWorkToOrder(order.getId(), work("Forbidden work",
                    MechanicSpecialization.GENERAL, "1.00", "1.00", 1));
        } catch (DomainException exception) {
            System.out.println("Expected failure: " + exception.getMessage());
            System.out.println("Execution continued after expected failure.");
        }
    }

    private void demonstrateConcurrentModification(RepairOrder order) {
        System.out.println("[F] Optimistic-lock conflict scenario");
        RepairOrder first = repairOrderService.getById(order.getId());
        RepairOrder second = repairOrderService.getById(order.getId());
        first.recordCommercialPrice(new BigDecimal("101.00"));
        second.recordCommercialPrice(new BigDecimal("202.00"));
        repairOrderService.persistChanges(first);
        try {
            repairOrderService.persistChanges(second);
        } catch (ConcurrentModificationException exception) {
            System.out.println("Expected concurrent modification: " + exception.getMessage());
        }
        System.out.println("Persisted price after conflict: "
                + repairOrderService.getById(order.getId()).getFinalCommercialPrice());
    }

    private void showPersistedOrder(RepairOrder order) {
        System.out.println("Order " + order.getId() + ": status=" + order.getStatus()
                + ", price=" + order.getFinalCommercialPrice()
                + ", works=" + order.getWorks().size());
        order.getWorks().forEach(work ->
                System.out.println("  - " + work.getDescription() + ", completed=" + work.isCompleted()));
    }

    private static ServiceWork work(String description, MechanicSpecialization specialization,
                                    String parts, String labor, long hours) {
        return new ServiceWork(description, specialization,
                new BigDecimal(parts), new BigDecimal(labor), Duration.ofHours(hours));
    }
}
