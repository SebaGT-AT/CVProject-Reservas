package cl.reservas.scheduling;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityDayResponse(LocalDate date, List<AvailabilitySlotResponse> slots) {}
