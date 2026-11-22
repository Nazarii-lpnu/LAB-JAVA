package com.example.carservice.domain;

import com.example.carservice.domain.converters.DurationAttributeConverter;
import com.example.carservice.exception.InvalidArgumentException;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Embeddable
public class TimeSlot {
    @Column(name = "planned_start")
    private Instant start;
    @Convert(converter = DurationAttributeConverter.class)
    @Column(name = "planned_duration_seconds")
    private Duration duration;

    protected TimeSlot() {
    }

    public TimeSlot(Instant start, Duration duration) {
        if (Objects.isNull(start)) throw new InvalidArgumentException("Time slot start cannot be null.");
        if (Objects.isNull(duration) || duration.isZero() || duration.isNegative()) {
            throw new InvalidArgumentException("Time slot duration must be positive.");
        }
        this.start = start;
        this.duration = duration;
    }

    public Instant getStart() { return start; }
    public Duration getDuration() { return duration; }
    public Instant getEnd() { return start.plus(duration); }

    public boolean overlaps(TimeSlot other) {
        if (Objects.isNull(other)) throw new InvalidArgumentException("Other time slot cannot be null.");
        return start.isBefore(other.getEnd()) && other.start.isBefore(getEnd());
    }
}
