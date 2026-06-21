package cl.reservas.scheduling;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleExceptionRequest(
        @NotNull LocalDate date,
        @NotNull ScheduleExceptionType type,
        LocalTime startTime,
        LocalTime endTime,
        @Size(max = 200) String reason
) {}
