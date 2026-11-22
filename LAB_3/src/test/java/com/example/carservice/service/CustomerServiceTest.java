package com.example.carservice.service;

import com.example.carservice.domain.Customer;
import com.example.carservice.repository.CustomerRepository;
import com.example.carservice.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceTest {
    @Test
    void registersCustomer() {
        CustomerRepository repository = mock(CustomerRepository.class);
        Customer customer = new Customer("Owner");
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any(Customer.class)))
                .thenReturn(customer);
        CustomerService service = new CustomerServiceImpl(repository);

        assertSame(customer, service.registerCustomer("Owner"));
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any(Customer.class));
    }
}
