package com.example.carservice.service;

import com.example.carservice.domain.Customer;

public interface CustomerService {
    Customer registerCustomer(String name);

    Customer findCustomerById(long id);
}
