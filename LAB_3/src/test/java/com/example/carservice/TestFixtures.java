package com.example.carservice;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;

import java.math.BigDecimal;
import java.time.Duration;

public class TestFixtures {
    private TestFixtures() {
    }

    public static Customer customer() {
        return new Customer("Test Customer");
    }

    public static Car car(Customer customer) {
        return new Car("VIN-" + System.nanoTime(), "Brand", "Model", 2020, customer);
    }

    public static RepairOrder order() {
        Customer customer = customer();
        return new RepairOrder(car(customer));
    }

    public static ServiceWork work() {
        return work(MechanicSpecialization.ENGINE, "10.00", "20.00", 1);
    }

    public static ServiceWork work(MechanicSpecialization specialization, String parts,
                                   String labor, long hours) {
        return new ServiceWork("Work-" + System.nanoTime(), specialization,
                new BigDecimal(parts), new BigDecimal(labor), Duration.ofHours(hours));
    }
}
