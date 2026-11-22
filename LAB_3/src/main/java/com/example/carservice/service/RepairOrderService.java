package com.example.carservice.service;

import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface RepairOrderService {
    RepairOrder create(RepairOrder order);

    BigDecimal calculateBasePrice(long orderId);

    BigDecimal calculateCommercialPrice(long orderId);

    RepairOrder getById(long orderId);

    void addWork(long orderId, ServiceWork work);

    void addWorks(long orderId, Collection<ServiceWork> works);

    void removeWork(long orderId, long workId);

    void diagnose(long orderId);

    void approve(long orderId);

    void start(long orderId);

    void completeWork(long orderId, long workId);

    void complete(long orderId);

    void cancel(long orderId);

    void assignMechanic(long orderId, Mechanic mechanic);

    void plan(long orderId, Instant start, Duration duration);

    List<RepairOrder> findByCarVin(String vin);

    List<RepairOrder> findByMechanic(Mechanic mechanic);

    void persistChanges(RepairOrder order);
}
