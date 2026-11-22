package com.example.carservice.repository;

import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.Mechanic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepairOrderRepository {
    RepairOrder save(RepairOrder order);
    Optional<RepairOrder> findById(UUID orderId);
    RepairOrder update(RepairOrder order);
    List<RepairOrder> findByCarVin(String vin);
    List<RepairOrder> findByMechanic(Mechanic mechanic);
    List<RepairOrder> findAll();
}
