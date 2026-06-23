package cl.reservas.integration.googlecalendar;

import java.time.Instant;

public record GoogleCalendarStatusResponse(
        boolean configured,
        boolean connected,
        boolean reauthorizationRequired,
        Instant connectedAt) {
}
