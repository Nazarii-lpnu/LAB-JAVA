package com.example.carservice.app;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.MechanicSpecialization;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.exception.DomainException;
import com.example.carservice.pricing.CommercialPricingPolicy;
import com.example.carservice.repository.inmemory.InMemoryRepairOrderRepository;
import com.example.carservice.service.RepairOrderService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public class CarServiceDemo {

    public static void main(String[] args) {
        InMemoryRepairOrderRepository repository = new InMemoryRepairOrderRepository();
        RepairOrderService service = new RepairOrderService(repository, new CommercialPricingPolicy());

        Customer customer = new Customer(UUID.randomUUID(), "Oleh Melnyk");
        Car car = new Car("VIN-DEMO-001", "Toyota", "Corolla", 2021, customer);
        customer.addCar(car);
        Mechanic mechanic = new Mechanic(UUID.randomUUID(), "Andrii Melnyk", MechanicSpecialization.GENERAL);
        RepairOrder order = new RepairOrder(UUID.randomUUID(), car);
        ServiceWork diagnostics = work("Computer diagnostics", MechanicSpecialization.DIAGNOSTICS, "25.00",
                "40.00", 1);
        ServiceWork engineRepair = work("Engine oil and filter replacement", MechanicSpecialization.ENGINE, "60.00", "80.00", 2);

        service.create(order);
        System.out.println("Saved order: " + order.getId());
        System.out.println("Retrieved order: " + service.getById(order.getId()).getId());
        service.addWork(order.getId(), diagnostics);
        service.addWork(order.getId(), engineRepair);
        System.out.println("Base price: " + service.calculateBasePrice(order.getId()));
        System.out.println("Commercial price: " + service.calculateCommercialPrice(order.getId()));

        service.plan(order.getId(), LocalDateTime.of(2026, 10, 1, 9, 0), Duration.ofHours(3));
        service.diagnose(order.getId());
        service.approve(order.getId());
        service.assignMechanic(order.getId(), mechanic);
        service.start(order.getId());
        service.completeWork(order.getId(), diagnostics.getId());
        service.completeWork(order.getId(), engineRepair.getId());
        service.complete(order.getId());
        System.out.println("Valid operation completed with status: " + order.getStatus());

        try {
            service.addWork(order.getId(), work("Forbidden work", MechanicSpecialization.GENERAL, "1.00", "1.00", 1));
        } catch (DomainException exception) {
            System.out.println("Expected failure: " + exception.getMessage());
        }
        System.out.println("Execution continued after expected failure.");
    }

    private static ServiceWork work(String description, MechanicSpecialization specialization,
                                    String parts, String labor, long hours) {
        return new ServiceWork(UUID.randomUUID(), description, specialization,
                new BigDecimal(parts), new BigDecimal(labor), Duration.ofHours(hours));
    }
}
