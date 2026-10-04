package com.example.carservice.domain;

import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.InvalidStateTransitionException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

public class ServiceWork {

    private final UUID id;
    private final String description;
    private final MechanicSpecialization specialization;
    private final BigDecimal partsCost;
    private final BigDecimal laborCost;
    private final Duration duration;
    private WorkStatus status;

    public ServiceWork(
            UUID id,
            String description,
            MechanicSpecialization specialization,
            BigDecimal partsCost,
            BigDecimal laborCost,
            Duration duration) {
        if (id == null) {
            throw new InvalidArgumentException("Service work id cannot be null.");
        }
        if (description == null || description.isBlank()) {
            throw new InvalidArgumentException("Work description cannot be null, empty, or blank.");
        }
        if (specialization == null) {
            throw new InvalidArgumentException("Work specialization cannot be null.");
        }
        if (partsCost == null || partsCost.signum() < 0) {
            throw new InvalidArgumentException("Parts cost cannot be null or negative.");
        }
        if (laborCost == null || laborCost.signum() < 0) {
            throw new InvalidArgumentException("Labor cost cannot be null or negative.");
        }
        if (duration == null || duration.isNegative()) {
            throw new InvalidArgumentException("Work duration cannot be null or negative.");
        }

        this.id = id;
        this.description = description.trim();
        this.specialization = specialization;
        this.partsCost = partsCost;
        this.laborCost = laborCost;
        this.duration = duration;
        this.status = WorkStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public MechanicSpecialization getSpecialization() {
        return specialization;
    }

    public BigDecimal getPartsCost() {
        return partsCost;
    }

    public BigDecimal getLaborCost() {
        return laborCost;
    }

    public Duration getDuration() {
        return duration;
    }

    public BigDecimal getTotalCost() {
        return partsCost.add(laborCost);
    }

    public boolean isCompleted() {
        return status == WorkStatus.COMPLETED;
    }

    public WorkStatus getStatus() {
        return status;
    }

    public void complete() {
        if (status == WorkStatus.COMPLETED) {
            throw new InvalidStateTransitionException("Service work is already completed.");
        }
        status = WorkStatus.COMPLETED;
    }
}
