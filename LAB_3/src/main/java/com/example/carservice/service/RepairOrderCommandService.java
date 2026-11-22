package com.example.carservice.service;

import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.enumeration.OrderPriority;

import java.util.Collection;

public interface RepairOrderCommandService {
    RepairOrder createOrder(String vin);

    RepairOrder createOrder(String vin, OrderPriority priority);

    void addWorkToOrder(long orderId, ServiceWork work);

    void addWorksToOrder(long orderId, Collection<ServiceWork> works);

    void assignMechanic(long orderId, long mechanicId);

    void diagnoseOrder(long orderId);

    void approveOrder(long orderId);

    void startOrder(long orderId);

    void completeWork(long orderId, long workId);

    void completeOrder(long orderId);
}
