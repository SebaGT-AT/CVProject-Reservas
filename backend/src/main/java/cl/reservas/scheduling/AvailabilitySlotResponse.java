package cl.reservas.scheduling;

import java.time.Instant;
import java.time.LocalTime;

public record AvailabilitySlotResponse(
        Instant startAt,
        Instant endAt,
        LocalTime localStartTime,
        LocalTime localEndTime
) {}
