package com.example.carservice.service;

import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumeration.MechanicSpecialization;

public interface MechanicService {
    Mechanic registerMechanic(String name, MechanicSpecialization specialization);

    Mechanic findMechanicById(long id);
}
