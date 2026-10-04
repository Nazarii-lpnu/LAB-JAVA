package com.example.carservice.domain;

import com.example.carservice.exception.InvalidArgumentException;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Value object for planned order scheduling.
 */
public final class TimeSlot {

    private final LocalDateTime start;
    private final Duration duration;

    public TimeSlot(LocalDateTime start, Duration duration) {
        if (start == null) {
            throw new InvalidArgumentException("Time slot start cannot be null.");
        }
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new InvalidArgumentException("Time slot duration must be positive.");
        }

        this.start = start;
        this.duration = duration;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public Duration getDuration() {
        return duration;
    }

    public LocalDateTime getEnd() {
        return start.plus(duration);
    }

    public boolean overlaps(TimeSlot other) {
        if (other == null) {
            throw new InvalidArgumentException("Other time slot cannot be null.");
        }

        return start.isBefore(other.getEnd()) && other.getStart().isBefore(getEnd());
    }
}
