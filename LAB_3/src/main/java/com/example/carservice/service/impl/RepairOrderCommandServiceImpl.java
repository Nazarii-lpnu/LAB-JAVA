package com.example.carservice.service.impl;

import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.enumeration.OrderPriority;
import com.example.carservice.service.CarService;
import com.example.carservice.service.MechanicService;
import com.example.carservice.service.RepairOrderCommandService;
import com.example.carservice.service.RepairOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@RequiredArgsConstructor
@Service
public class RepairOrderCommandServiceImpl implements RepairOrderCommandService {
    private final RepairOrderService repairOrderService;
    private final CarService carService;
    private final MechanicService mechanicService;

    @Override
    @Transactional
    public RepairOrder createOrder(String vin) {
        return createOrder(vin, OrderPriority.NORMAL);
    }

    @Override
    @Transactional
    public RepairOrder createOrder(String vin, OrderPriority priority) {
        return repairOrderService.create(new RepairOrder(carService.findCarByVin(vin), priority));
    }

    @Override
    public void addWorkToOrder(long orderId, ServiceWork work) {
        repairOrderService.addWork(orderId, work);
    }

    @Override
    public void addWorksToOrder(long orderId, Collection<ServiceWork> works) {
        repairOrderService.addWorks(orderId, works);
    }

    @Override
    public void assignMechanic(long orderId, long mechanicId) {
        repairOrderService.assignMechanic(orderId, mechanicService.findMechanicById(mechanicId));
    }

    @Override
    public void diagnoseOrder(long orderId) {
        repairOrderService.diagnose(orderId);
    }

    @Override
    public void approveOrder(long orderId) {
        repairOrderService.approve(orderId);
    }

    @Override
    public void startOrder(long orderId) {
        repairOrderService.start(orderId);
    }

    @Override
    public void completeWork(long orderId, long workId) {
        repairOrderService.completeWork(orderId, workId);
    }

    @Override
    public void completeOrder(long orderId) {
        repairOrderService.complete(orderId);
    }
}
