package cl.reservas.integration.googlecalendar;

import cl.reservas.appointment.Appointment;
import cl.reservas.appointment.AppointmentRepository;
import cl.reservas.appointment.AppointmentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class AppointmentCalendarPublisher {
    private final CalendarSyncOutboxRepository outbox;
    private final GoogleCalendarConnectionRepository connections;
    private final AppointmentRepository appointments;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AppointmentCalendarPublisher(CalendarSyncOutboxRepository outbox,
                                        GoogleCalendarConnectionRepository connections,
                                        AppointmentRepository appointments,
                                        ObjectMapper objectMapper, Clock clock) {
        this.outbox = outbox;
        this.connections = connections;
        this.appointments = appointments;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void confirmed(Appointment appointment) { enqueueIfConnected(appointment, CalendarSyncOperation.UPSERT); }

    @Transactional
    public void cancelled(Appointment appointment) { enqueueIfConnected(appointment, CalendarSyncOperation.DELETE); }

    @Transactional
    public void enqueueFutureAppointments(UUID professionalId) {
        appointments.findAllByProfessionalIdAndStatusAndStartAtGreaterThanEqualOrderByStartAt(
                        professionalId, AppointmentStatus.CONFIRMED, clock.instant())
                .forEach(appointment -> enqueueIfConnected(appointment, CalendarSyncOperation.UPSERT));
    }

    private void enqueueIfConnected(Appointment appointment, CalendarSyncOperation operation) {
        connections.findById(appointment.getProfessional().getId())
                .filter(connection -> connection.getStatus() == GoogleConnectionStatus.CONNECTED)
                .ifPresent(connection -> enqueue(appointment, operation, connection.getConnectionGeneration()));
    }

    private void enqueue(Appointment appointment, CalendarSyncOperation operation, UUID generation) {
        CalendarSyncPayload payload = payload(appointment);
        String deduplicationKey = "appointment:" + appointment.getId() + ":google:"
                + generation + ":" + operation.name().toLowerCase();
        try {
            outbox.insertEvent(UUID.randomUUID(), appointment.getId(), appointment.getProfessional().getId(),
                    operation.name(), deduplicationKey, objectMapper.writeValueAsString(payload), clock.instant());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible serializar la sincronizacion de calendario", exception);
        }
    }

    private CalendarSyncPayload payload(Appointment appointment) {
        String eventId = "r" + appointment.getId().toString().replace("-", "");
        String summary = appointment.getServiceName() + " - " + appointment.getCustomer().getName();
        String description = "Reserva gestionada por Reservas\nCliente: " + appointment.getCustomer().getName()
                + "\nCorreo: " + appointment.getCustomer().getEmail()
                + "\nID: " + appointment.getId();
        return new CalendarSyncPayload(appointment.getId(), eventId, summary, description,
                appointment.getStartAt(), appointment.getEndAt(), appointment.getProfessionalTimeZone());
    }
}
