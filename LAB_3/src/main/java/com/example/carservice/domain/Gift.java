package com.example.carservice.domain;

import com.example.carservice.domain.enumeration.GiftType;
import com.example.carservice.exception.InvalidArgumentException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public class Gift {
    private static final BigDecimal AIR_FRESHENER_THRESHOLD = new BigDecimal("100.00");
    private static final BigDecimal CAR_CARE_KIT_THRESHOLD = new BigDecimal("250.00");
    private static final BigDecimal FREE_DIAGNOSTICS_THRESHOLD = new BigDecimal("500.00");

    private GiftType type;
    private String description;
    private BigDecimal minimumOrderAmount;

    public Gift(GiftType type, String description, BigDecimal minimumOrderAmount) {
        if (Objects.isNull(type)) throw new InvalidArgumentException("Gift type cannot be null.");
        if (Objects.isNull(description) || description.isBlank()) {
            throw new InvalidArgumentException("Gift description cannot be null, empty, or blank.");
        }
        if (Objects.isNull(minimumOrderAmount) || minimumOrderAmount.signum() < 0) {
            throw new InvalidArgumentException("Gift threshold cannot be null or negative.");
        }
        this.type = type;
        this.description = description.trim();
        this.minimumOrderAmount = minimumOrderAmount;
    }

    public static Optional<Gift> forOrderAmount(BigDecimal amount) {
        if (Objects.isNull(amount) || amount.signum() < 0) {
            throw new InvalidArgumentException("Order amount cannot be null or negative.");
        }
        if (amount.compareTo(FREE_DIAGNOSTICS_THRESHOLD) >= 0) {
            return Optional.of(new Gift(GiftType.FREE_DIAGNOSTICS, "Free vehicle diagnostics", FREE_DIAGNOSTICS_THRESHOLD));
        }
        if (amount.compareTo(CAR_CARE_KIT_THRESHOLD) >= 0) {
            return Optional.of(new Gift(GiftType.CAR_CARE_KIT, "Car care kit", CAR_CARE_KIT_THRESHOLD));
        }
        if (amount.compareTo(AIR_FRESHENER_THRESHOLD) >= 0) {
            return Optional.of(new Gift(GiftType.AIR_FRESHENER, "Car air freshener", AIR_FRESHENER_THRESHOLD));
        }
        return Optional.empty();
    }

    public GiftType getType() { return type; }
}
