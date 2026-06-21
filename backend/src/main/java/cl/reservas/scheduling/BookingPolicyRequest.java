package cl.reservas.scheduling;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record BookingPolicyRequest(
        @Min(0) @Max(43200) int minimumNoticeMinutes,
        @Min(1) @Max(365) int bookingWindowDays,
        @Min(5) @Max(120) int slotIntervalMinutes,
        @Min(0) @Max(180) int bufferAfterMinutes
) {}
