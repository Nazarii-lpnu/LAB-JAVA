package com.example.carservice.domain;

import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.exception.InvalidArgumentException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Collection;
import java.util.Objects;

@Entity
@Table(name = "mechanics")
public class Mechanic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "specialization", nullable = false, length = 32)
    private MechanicSpecialization specialization;

    protected Mechanic() {
    }

    public Mechanic(String name, MechanicSpecialization specialization) {
        if (Objects.isNull(name) || name.isBlank()) throw new InvalidArgumentException("Mechanic name cannot be null, empty, or blank.");
        if (Objects.isNull(specialization)) throw new InvalidArgumentException("Mechanic specialization cannot be null.");
        this.name = name.trim();
        this.specialization = specialization;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public MechanicSpecialization getSpecialization() { return specialization; }
    public boolean canPerform(ServiceWork work) {
        if (Objects.isNull(work)) throw new InvalidArgumentException("Service work cannot be null.");
        return Objects.equals(specialization, MechanicSpecialization.GENERAL)
                || Objects.equals(specialization, work.getSpecialization());
    }

    public boolean canPerformAll(Collection<ServiceWork> works) {
        if (Objects.isNull(works)) throw new InvalidArgumentException("Works cannot be null.");
        return works.stream().allMatch(this::canPerform);
    }
}
