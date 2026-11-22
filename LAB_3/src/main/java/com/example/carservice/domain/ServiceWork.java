package com.example.carservice.domain;

import com.example.carservice.domain.converters.DurationAttributeConverter;
import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.domain.enumeration.WorkStatus;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.InvalidStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

@Entity
@Table(name = "service_works")
public class ServiceWork {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;
    @Column(name = "description", nullable = false, length = 500)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "specialization", nullable = false, length = 32)
    private MechanicSpecialization specialization;
    @Column(name = "parts_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal partsCost;
    @Column(name = "labor_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal laborCost;
    @Convert(converter = DurationAttributeConverter.class)
    @Column(name = "duration_seconds", nullable = false)
    private Duration duration;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private WorkStatus status = WorkStatus.PENDING;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repair_order_id", nullable = false)
    private RepairOrder order;

    protected ServiceWork() {
    }

    public ServiceWork(String description, MechanicSpecialization specialization,
                       BigDecimal partsCost, BigDecimal laborCost, Duration duration) {
        if (Objects.isNull(description) || description.isBlank()) throw new InvalidArgumentException("Work description cannot be null, empty, or blank.");
        if (Objects.isNull(specialization)) throw new InvalidArgumentException("Work specialization cannot be null.");
        if (Objects.isNull(partsCost) || partsCost.signum() < 0) throw new InvalidArgumentException("Parts cost cannot be null or negative.");
        if (Objects.isNull(laborCost) || laborCost.signum() < 0) throw new InvalidArgumentException("Labor cost cannot be null or negative.");
        if (Objects.isNull(duration) || duration.isNegative()) throw new InvalidArgumentException("Work duration cannot be null or negative.");
        this.description = description.trim();
        this.specialization = specialization;
        this.partsCost = partsCost;
        this.laborCost = laborCost;
        this.duration = duration;
    }

    public Long getId() { return id; }
    public MechanicSpecialization getSpecialization() { return specialization; }
    public BigDecimal getPartsCost() { return partsCost; }
    public BigDecimal getLaborCost() { return laborCost; }
    public Duration getDuration() { return duration; }
    public String getDescription() { return description; }
    public RepairOrder getOrder() { return order; }
    public boolean isCompleted() { return Objects.equals(status, WorkStatus.COMPLETED); }

    void assignTo(RepairOrder order) {
        this.order = order;
    }

    public void complete() {
        if (isCompleted()) throw new InvalidStateTransitionException("Service work is already completed.");
        status = WorkStatus.COMPLETED;
    }
}
