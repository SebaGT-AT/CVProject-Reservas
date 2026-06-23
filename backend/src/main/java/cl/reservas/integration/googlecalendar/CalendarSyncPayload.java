package cl.reservas.integration.googlecalendar;

import java.time.Instant;
import java.util.UUID;

public record CalendarSyncPayload(
        UUID appointmentId,
        String eventId,
        String summary,
        String description,
        Instant startAt,
        Instant endAt,
        String timeZone) {
}
