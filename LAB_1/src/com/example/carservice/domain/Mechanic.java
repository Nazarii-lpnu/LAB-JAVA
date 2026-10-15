package com.example.carservice.domain;

import com.example.carservice.exception.InvalidArgumentException;

import java.util.Collection;
import java.util.UUID;

public class Mechanic {

    private UUID id;
    private String name;
    private MechanicSpecialization specialization;

    public Mechanic(UUID id, String name, MechanicSpecialization specialization) {
        if (id == null) {
            throw new InvalidArgumentException("Mechanic id cannot be null.");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidArgumentException("Mechanic name cannot be null, empty, or blank.");
        }
        if (specialization == null) {
            throw new InvalidArgumentException("Mechanic specialization cannot be null.");
        }

        this.id = id;
        this.name = name.trim();
        this.specialization = specialization;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MechanicSpecialization getSpecialization() {
        return specialization;
    }

    public boolean canPerform(ServiceWork work) {
        if (work == null) {
            throw new InvalidArgumentException("Service work cannot be null.");
        }

        return MechanicSpecialization.GENERAL.equals(specialization)
                || specialization.equals(work.getSpecialization());
    }

    /**
     * A mechanic assigned to an order must be compatible with every work in it.
     */
    public boolean canPerformAll(Collection<ServiceWork> works) {
        if (works == null) {
            throw new InvalidArgumentException("Works cannot be null.");
        }

        return works.stream().allMatch(this::canPerform);
    }
}
