package cl.reservas.scheduling;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WeeklyScheduleRequest(
        @NotNull @Size(max = 35) List<@Valid SchedulePeriodRequest> periods
) {}
