package com.example.carservice.service;

import com.example.carservice.PostgresIntegrationTest;
import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.enumeration.OrderStatus;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.repository.CarRepository;
import com.example.carservice.repository.CustomerRepository;
import com.example.carservice.repository.RepairOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RepairOrderOptimisticLockingIntegrationTest extends PostgresIntegrationTest {
    @Autowired
    private RepairOrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void concurrentTransactionsKeepFirstUpdateAndRejectSecondUpdate() throws Exception {
        RepairOrder saved = createOrder();
        CountDownLatch bothLoaded = new CountDownLatch(2);
        CountDownLatch firstCommitted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<Void> first = executor.submit(() -> executeTransaction(saved.getId(), bothLoaded,
                firstCommitted, true));
        Future<Void> second = executor.submit(() -> executeTransaction(saved.getId(), bothLoaded,
                firstCommitted, false));

        first.get(10, TimeUnit.SECONDS);
        ExecutionException failure = org.junit.jupiter.api.Assertions.assertThrows(
                ExecutionException.class, () -> second.get(10, TimeUnit.SECONDS));
        assertInstanceOf(ObjectOptimisticLockingFailureException.class, failure.getCause());
        executor.shutdownNow();

        RepairOrder persisted = new TransactionTemplate(transactionManager).execute(status ->
                orderRepository.findById(saved.getId()).orElseThrow());
        assertEquals(OrderStatus.DIAGNOSED, persisted.getStatus());
    }

    private Void executeTransaction(long orderId, CountDownLatch bothLoaded,
                                    CountDownLatch firstCommitted, boolean first) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            RepairOrder order = orderRepository.findById(orderId).orElseThrow();
            order.diagnose();
            bothLoaded.countDown();
            await(bothLoaded);
            if (!first) await(firstCommitted);
            orderRepository.saveAndFlush(order);
            if (first) firstCommitted.countDown();
            return null;
        });
    }

    private RepairOrder createOrder() {
        Customer customer = customerRepository.saveAndFlush(
                new Customer("Locking Customer"));
        Car car = carRepository.saveAndFlush(new Car(
                "VIN-LOCK-" + System.nanoTime(), "Brand", "Model", 2020, customer));
        return orderRepository.saveAndFlush(new RepairOrder(car));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent transactions.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating concurrent transactions.", exception);
        }
    }
}
