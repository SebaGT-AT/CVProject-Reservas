package cl.reservas.scheduling;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleExceptionResponse(
        UUID id,
        LocalDate date,
        ScheduleExceptionType type,
        LocalTime startTime,
        LocalTime endTime,
        String reason
) {
    static ScheduleExceptionResponse from(ScheduleException exception) {
        return new ScheduleExceptionResponse(exception.getId(), exception.getDate(), exception.getType(),
                exception.getStartTime(), exception.getEndTime(), exception.getReason());
    }
}
