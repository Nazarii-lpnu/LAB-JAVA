package com.example.carservice.service;

import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.repository.MechanicRepository;
import com.example.carservice.service.impl.MechanicServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MechanicServiceTest {
    @Test
    void registersMechanic() {
        MechanicRepository repository = mock(MechanicRepository.class);
        Mechanic mechanic = new Mechanic("Alex", MechanicSpecialization.GENERAL);
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any(Mechanic.class)))
                .thenReturn(mechanic);
        MechanicService service = new MechanicServiceImpl(repository);

        assertSame(mechanic, service.registerMechanic("Alex", MechanicSpecialization.GENERAL));
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any(Mechanic.class));
    }
}
