package com.example.carservice.domain;

import com.example.carservice.exception.InvalidArgumentException;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Immutable gift awarded according to the order total.
 */
public final class Gift {

    private static BigDecimal AIR_FRESHENER_THRESHOLD = new BigDecimal("100.00");
    private static BigDecimal CAR_CARE_KIT_THRESHOLD = new BigDecimal("250.00");
    private static BigDecimal FREE_DIAGNOSTICS_THRESHOLD = new BigDecimal("500.00");

    private GiftType type;
    private String description;
    private BigDecimal minimumOrderAmount;

    public Gift(GiftType type, String description, BigDecimal minimumOrderAmount) {
        if (type == null) {
            throw new InvalidArgumentException("Gift type cannot be null.");
        }
        if (description == null || description.isBlank()) {
            throw new InvalidArgumentException("Gift description cannot be null, empty, or blank.");
        }
        if (minimumOrderAmount == null || minimumOrderAmount.signum() < 0) {
            throw new InvalidArgumentException("Gift threshold cannot be null or negative.");
        }

        this.type = type;
        this.description = description.strip();
        this.minimumOrderAmount = minimumOrderAmount;
    }

    /**
     * Selects the highest gift tier reached by the order amount.
     */
    public static Optional<Gift> forOrderAmount(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.signum() < 0) {
            throw new InvalidArgumentException("Order amount cannot be null or negative.");
        }

        if (orderAmount.compareTo(FREE_DIAGNOSTICS_THRESHOLD) >= 0) {
            return Optional.of(new Gift(
                    GiftType.FREE_DIAGNOSTICS,
                    "Free vehicle diagnostics",
                    FREE_DIAGNOSTICS_THRESHOLD));
        }
        if (orderAmount.compareTo(CAR_CARE_KIT_THRESHOLD) >= 0) {
            return Optional.of(new Gift(
                    GiftType.CAR_CARE_KIT,
                    "Car care kit",
                    CAR_CARE_KIT_THRESHOLD));
        }
        if (orderAmount.compareTo(AIR_FRESHENER_THRESHOLD) >= 0) {
            return Optional.of(new Gift(
                    GiftType.AIR_FRESHENER,
                    "Car air freshener",
                    AIR_FRESHENER_THRESHOLD));
        }

        return Optional.empty();
    }

    public GiftType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

}
