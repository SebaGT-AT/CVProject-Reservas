package cl.reservas.scheduling;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record SchedulePeriodResponse(UUID id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    static SchedulePeriodResponse from(WeeklySchedulePeriod period) {
        return new SchedulePeriodResponse(period.getId(), period.getDayOfWeek(),
                period.getStartTime(), period.getEndTime());
    }
}
