package com.example.carservice.domain;

import com.example.carservice.domain.enumeration.OrderPriority;
import com.example.carservice.domain.enumeration.OrderStatus;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.MechanicNotCompatibleException;
import com.example.carservice.exception.OrderModificationNotAllowedException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "repair_orders")
public class RepairOrder {
    private static final int MONEY_SCALE = 2;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_vin", nullable = false)
    private Car car;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private OrderPriority priority;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @OrderBy("id")
    private List<ServiceWork> works = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id")
    private Mechanic mechanic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OrderStatus status = OrderStatus.CREATED;

    @Embedded
    private TimeSlot plannedSlot;
    @Column(name = "final_commercial_price", precision = 19, scale = 2)
    private BigDecimal finalCommercialPrice;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected RepairOrder() {
    }

    public RepairOrder(Car car) {
        this(car, OrderPriority.NORMAL, Instant.now());
    }

    public RepairOrder(Car car, OrderPriority priority) {
        this(car, priority, Instant.now());
    }

    public RepairOrder(Car car, OrderPriority priority, Instant createdAt) {
        if (Objects.isNull(car)) throw new InvalidArgumentException("Order car cannot be null.");
        if (Objects.isNull(priority)) throw new InvalidArgumentException("Order priority cannot be null.");
        if (Objects.isNull(createdAt))
            throw new InvalidArgumentException("Order creation time cannot be null.");
        this.car = car;
        this.priority = priority;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Car getCar() {
        return car;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public OrderPriority getPriority() {
        return priority;
    }

    public Mechanic getMechanic() {
        return mechanic;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public TimeSlot getPlannedSlot() {
        return plannedSlot;
    }

    public List<ServiceWork> getWorks() {
        return List.copyOf(works);
    }

    public BigDecimal getFinalCommercialPrice() {
        return finalCommercialPrice;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Long getVersion() {
        return version;
    }

    public Optional<Gift> getGift() {
        return Gift.forOrderAmount(calculateBaseCost());
    }

    public BigDecimal calculateBaseCost() {
        BigDecimal parts = works.stream().map(ServiceWork::getPartsCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal labor = works.stream().map(ServiceWork::getLaborCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return parts.add(labor).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public void addWork(ServiceWork work) {
        ensureModifiable();
        if (Objects.isNull(work)) throw new InvalidArgumentException("Service work cannot be null.");
        if (works.stream().anyMatch(existing -> Objects.nonNull(existing.getId())
                && Objects.equals(existing.getId(), work.getId()))) {
            throw new InvalidArgumentException("A work with id '" + work.getId() + "' is already in the order.");
        }
        ensureMechanicCanPerform(work);
        works.add(work);
        work.assignTo(this);
    }

    public void removeWork(long workId) {
        ensureModifiable();
        ServiceWork work = findWork(workId);
        works.remove(work);
        work.assignTo(null);
    }

    public void assignMechanic(Mechanic mechanic) {
        ensureModifiable();
        if (Objects.isNull(mechanic)) throw new InvalidArgumentException("Mechanic cannot be null.");
        if (!mechanic.canPerformAll(works)) {
            throw new MechanicNotCompatibleException("Mechanic '" + mechanic.getId() + "' is not compatible with every work in the order.");
        }
        this.mechanic = mechanic;
    }

    public void plan(TimeSlot timeSlot) {
        ensureModifiable();
        if (Objects.isNull(timeSlot)) throw new InvalidArgumentException("Planned time slot cannot be null.");
        plannedSlot = timeSlot;
    }

    public void diagnose() {
        transitionTo(OrderStatus.DIAGNOSED);
    }

    public void approve() {
        ensureTransitionAllowed(OrderStatus.APPROVED);
        if (works.isEmpty())
            throw new InvalidStateTransitionException("An order cannot be approved without at least one work.");
        status = OrderStatus.APPROVED;
    }

    public void start() {
        ensureTransitionAllowed(OrderStatus.IN_PROGRESS);
        if (Objects.isNull(mechanic))
            throw new InvalidStateTransitionException("An order cannot start without an assigned mechanic.");
        status = OrderStatus.IN_PROGRESS;
    }

    public void completeWork(long workId) {
        if (!Objects.equals(status, OrderStatus.IN_PROGRESS)) {
            throw new InvalidStateTransitionException("Work can be completed only while the order is in progress.");
        }
        findWork(workId).complete();
    }

    public void complete() {
        if (!Objects.equals(status, OrderStatus.IN_PROGRESS)) {
            throw new InvalidStateTransitionException("An order can be completed only while it is in progress.");
        }
        if (works.stream().anyMatch(work -> !work.isCompleted())) {
            throw new InvalidStateTransitionException("An order cannot be completed while unfinished work remains.");
        }
        status = OrderStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    public void recordCommercialPrice(BigDecimal price) {
        if (Objects.isNull(price) || price.signum() < 0) {
            throw new InvalidArgumentException("Commercial price cannot be null or negative.");
        }
        finalCommercialPrice = price.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RepairOrder that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private void ensureMechanicCanPerform(ServiceWork work) {
        if (Objects.nonNull(mechanic) && !mechanic.canPerform(work)) {
            throw new MechanicNotCompatibleException("Assigned mechanic '" + mechanic.getId() + "' is not compatible with work '" + work.getId() + "'.");
        }
    }

    private ServiceWork findWork(long workId) {
        return works.stream()
                .filter(work -> Objects.equals(work.getId(), workId)
                        || (Objects.isNull(work.getId()) && workId == 0L))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Work with id '" + workId + "' is not part of this order."));
    }

    private void ensureModifiable() {
        if (Objects.equals(status, OrderStatus.COMPLETED) || Objects.equals(status, OrderStatus.CANCELLED)) {
            throw new OrderModificationNotAllowedException("Completed or cancelled orders cannot be modified.");
        }
    }

    private void transitionTo(OrderStatus target) {
        ensureTransitionAllowed(target);
        status = target;
    }

    private void ensureTransitionAllowed(OrderStatus target) {
        if (!isAllowedTransition(target)) {
            throw new InvalidStateTransitionException("Cannot change order status from " + status + " to " + target + ".");
        }
    }

    private boolean isAllowedTransition(OrderStatus target) {
        return (Objects.equals(status, OrderStatus.CREATED)
                && (Objects.equals(target, OrderStatus.DIAGNOSED) || Objects.equals(target, OrderStatus.CANCELLED)))
                || (Objects.equals(status, OrderStatus.DIAGNOSED)
                && (Objects.equals(target, OrderStatus.APPROVED) || Objects.equals(target, OrderStatus.CANCELLED)))
                || (Objects.equals(status, OrderStatus.APPROVED)
                && (Objects.equals(target, OrderStatus.IN_PROGRESS) || Objects.equals(target, OrderStatus.CANCELLED)))
                || (Objects.equals(status, OrderStatus.IN_PROGRESS) && Objects.equals(target, OrderStatus.COMPLETED));
    }
}
