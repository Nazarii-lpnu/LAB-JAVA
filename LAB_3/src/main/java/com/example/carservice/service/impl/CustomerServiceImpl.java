package com.example.carservice.service.impl;

import com.example.carservice.domain.Customer;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.repository.CustomerRepository;
import com.example.carservice.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public Customer registerCustomer(String name) {
        try {
            return customerRepository.saveAndFlush(new Customer(name));
        } catch (DataIntegrityViolationException exception) {
            throw new EntityAlreadyExistsException("A customer with the supplied details already exists.");
        }
    }

    @Override
    public Customer findCustomerById(long id) {
        return customerRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("No registered customer was found for id '" + id + "'."));
    }
}
