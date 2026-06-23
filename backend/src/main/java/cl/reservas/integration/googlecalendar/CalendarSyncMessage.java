package cl.reservas.integration.googlecalendar;

import java.util.UUID;

record CalendarSyncMessage(UUID id, UUID professionalId, CalendarSyncOperation operation,
                           String payload, int attempts) {
    static CalendarSyncMessage from(CalendarSyncOutbox item) {
        return new CalendarSyncMessage(item.getId(), item.getProfessionalId(), item.getOperation(),
                item.getPayload(), item.getAttempts());
    }
}
