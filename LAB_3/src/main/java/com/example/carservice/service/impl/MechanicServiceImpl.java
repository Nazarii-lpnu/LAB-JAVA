package com.example.carservice.service.impl;

import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.repository.MechanicRepository;
import com.example.carservice.service.MechanicService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MechanicServiceImpl implements MechanicService {
    private final MechanicRepository mechanicRepository;

    @Override
    @Transactional
    public Mechanic registerMechanic(String name, MechanicSpecialization specialization) {
        try {
            return mechanicRepository.saveAndFlush(new Mechanic(name, specialization));
        } catch (DataIntegrityViolationException exception) {
            throw new EntityAlreadyExistsException("A mechanic with the supplied details already exists.");
        }
    }

    @Override
    public Mechanic findMechanicById(long id) {
        return mechanicRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("No registered mechanic was found for id '" + id + "'."));
    }
}
