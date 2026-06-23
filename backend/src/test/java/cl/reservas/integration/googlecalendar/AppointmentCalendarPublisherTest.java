package cl.reservas.integration.googlecalendar;

import cl.reservas.appointment.Appointment;
import cl.reservas.appointment.AppointmentRepository;
import cl.reservas.professional.ProfessionalProfile;
import cl.reservas.professional.ServiceOffering;
import cl.reservas.user.Role;
import cl.reservas.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentCalendarPublisherTest {
    @Mock CalendarSyncOutboxRepository outbox;
    @Mock GoogleCalendarConnectionRepository connections;
    @Mock AppointmentRepository appointments;

    @Test
    void enqueuesDeterministicGoogleEventForConnectedProfessional() {
        User professionalUser = new User("Ada", "ada@example.com", "hash", Role.PROFESSIONAL);
        User customer = new User("Grace", "grace@example.com", "hash", Role.CUSTOMER);
        ProfessionalProfile professional = new ProfessionalProfile(professionalUser, "ada", null, null,
                "America/Santiago", true, Set.of());
        ServiceOffering offering = new ServiceOffering(professional, "Consulta", null, 60,
                new BigDecimal("25000"), "CLP", true);
        Instant start = Instant.parse("2026-07-01T13:00:00Z");
        Appointment appointment = new Appointment(professional, customer, offering, UUID.randomUUID(), start,
                start.plusSeconds(3600), start.plusSeconds(3600));
        GoogleCalendarConnection connection = new GoogleCalendarConnection(professional, "encrypted", start);
        when(connections.findById(professional.getId())).thenReturn(Optional.of(connection));
        var publisher = new AppointmentCalendarPublisher(outbox, connections, appointments,
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(start, ZoneOffset.UTC));

        publisher.confirmed(appointment);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outbox).insertEvent(any(), any(), any(), any(), any(), payload.capture(), any());
        assertThat(payload.getValue()).contains("r" + appointment.getId().toString().replace("-", ""))
                .contains("grace@example.com");
    }
}
