package com.example.carservice.repository.inmemory;

import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.repository.RepairOrderRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryRepairOrderRepository implements RepairOrderRepository {
    private Map<UUID, RepairOrder> orders = new HashMap<>();

    @Override
    public RepairOrder save(RepairOrder order) {
        requireOrder(order);
        if (orders.containsKey(order.getId())) {
            throw new EntityAlreadyExistsException("An order with id '" + order.getId() + "' already exists.");
        }
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<RepairOrder> findById(UUID orderId) {
        if (orderId == null) throw new InvalidArgumentException("Order id cannot be null.");
        return Optional.ofNullable(orders.get(orderId));
    }

    @Override
    public RepairOrder update(RepairOrder order) {
        requireOrder(order);
        if (!orders.containsKey(order.getId())) {
            throw new EntityNotFoundException("No service order was found for id '" + order.getId() + "'.");
        }
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public List<RepairOrder> findByCarVin(String vin) {
        if (vin == null || vin.isBlank()) throw new InvalidArgumentException("VIN cannot be null, empty, or blank.");
        return orders.values().stream().filter(order -> order.getCar().getVin().equals(vin.trim()))
                .sorted(Comparator.comparing(RepairOrder::getCreatedAt)).toList();
    }

    @Override
    public List<RepairOrder> findByMechanic(Mechanic mechanic) {
        if (mechanic == null) throw new InvalidArgumentException("Mechanic cannot be null.");
        return orders.values().stream().filter(order -> mechanic.equals(order.getMechanic()))
                .sorted(Comparator.comparing(RepairOrder::getCreatedAt)).toList();
    }

    @Override
    public List<RepairOrder> findAll() {
        return new ArrayList<>(orders.values());
    }

    private void requireOrder(RepairOrder order) {
        if (order == null) throw new InvalidArgumentException("Repair order cannot be null.");
    }
}
