package com.example.carservice.service;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.enumeration.OrderPriority;
import com.example.carservice.service.impl.RepairOrderCommandServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepairOrderCommandServiceTest {
    @Test
    void createsOrderUsingNormalizedPriority() {
        Car car = new Car("VIN-1", "Brand", "Model", 2020, new Customer("Owner"));
        CarService carService = mock(CarService.class);
        MechanicService mechanicService = mock(MechanicService.class);
        RepairOrderService repairOrderService = mock(RepairOrderService.class);
        when(carService.findCarByVin("VIN-1")).thenReturn(car);
        RepairOrder created = new RepairOrder(car, OrderPriority.URGENT);
        when(repairOrderService.create(org.mockito.ArgumentMatchers.any(RepairOrder.class)))
                .thenReturn(created);
        RepairOrderCommandService service =
                new RepairOrderCommandServiceImpl(repairOrderService, carService, mechanicService);

        assertSame(created, service.createOrder("VIN-1", OrderPriority.URGENT));
        verify(repairOrderService).create(org.mockito.ArgumentMatchers.argThat(
                order -> order.getCar().equals(car) && order.getPriority() == OrderPriority.URGENT));
    }
}
