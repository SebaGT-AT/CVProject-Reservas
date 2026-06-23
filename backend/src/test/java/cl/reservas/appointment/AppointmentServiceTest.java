package cl.reservas.appointment;

import cl.reservas.common.exception.ConflictException;
import cl.reservas.professional.*;
import cl.reservas.scheduling.*;
import cl.reservas.user.Role;
import cl.reservas.user.User;
import cl.reservas.user.UserRepository;
import cl.reservas.integration.googlecalendar.AppointmentCalendarPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {
    @Mock AppointmentRepository appointments;
    @Mock AppointmentStatusHistoryRepository history;
    @Mock ProfessionalProfileRepository profiles;
    @Mock ServiceOfferingRepository services;
    @Mock BookingPolicyRepository policies;
    @Mock UserRepository users;
    @Mock AvailabilityService availability;
    @Mock AppointmentNotificationPublisher notifications;
    @Mock AppointmentCalendarPublisher calendar;
    private AppointmentService appointmentService;
    private User customer;
    private ProfessionalProfile professional;
    private ServiceOffering offering;
    private BookingPolicy policy;
    private final Instant now = Instant.parse("2026-06-21T12:00:00Z");
    private final Instant start = Instant.parse("2026-06-22T13:00:00Z");

    @BeforeEach
    void setUp() {
        customer = new User("Grace", "grace@example.com", "encoded", Role.CUSTOMER);
        User professionalUser = new User("Ada", "ada@example.com", "encoded", Role.PROFESSIONAL);
        professional = new ProfessionalProfile(professionalUser, "ada", null, null,
                "America/Santiago", true, Set.of());
        offering = new ServiceOffering(professional, "Consulta", null, 60,
                new BigDecimal("25000"), "CLP", true);
        policy = new BookingPolicy(professional);
        policy.update(0, 30, 30, 15, 60);
        appointmentService = new AppointmentService(appointments, history, profiles, services, policies, users,
                availability, notifications, calendar, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void booksAvailableSlotWithSnapshotsAndHistory() {
        UUID idempotencyKey = UUID.randomUUID();
        stubBookingDependencies(idempotencyKey);
        var slot = new AvailabilitySlotResponse(start, start.plusSeconds(3600),
                LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(availability.availability("ada", offering.getId(), LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 22)))
                .thenReturn(List.of(new AvailabilityDayResponse(LocalDate.of(2026, 6, 22), List.of(slot))));
        when(policies.findByProfessionalId(professional.getId())).thenReturn(Optional.of(policy));
        when(appointments.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentBookingResult result = appointmentService.book("grace@example.com",
                new BookAppointmentRequest("ada", offering.getId(), start, idempotencyKey));

        assertThat(result.created()).isTrue();
        assertThat(result.appointment().status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(result.appointment().serviceName()).isEqualTo("Consulta");
        assertThat(result.appointment().priceAmount()).isEqualByComparingTo("25000");
        verify(history).save(any(AppointmentStatusHistory.class));
        verify(notifications).confirmed(any(Appointment.class));
        verify(calendar).confirmed(any(Appointment.class));
    }

    @Test
    void repeatedIdempotencyKeyReturnsOriginalAppointmentWithoutBookingAgain() {
        UUID key = UUID.randomUUID();
        Appointment existing = appointment(key, start);
        when(users.findByEmailIgnoreCase("grace@example.com")).thenReturn(Optional.of(customer));
        when(appointments.findByCustomerIdAndIdempotencyKey(customer.getId(), key)).thenReturn(Optional.of(existing));

        AppointmentBookingResult result = appointmentService.book("grace@example.com",
                new BookAppointmentRequest("ada", offering.getId(), start, key));

        assertThat(result.created()).isFalse();
        assertThat(result.appointment().id()).isEqualTo(existing.getId());
        verifyNoInteractions(availability, history);
        verify(appointments, never()).saveAndFlush(any());
    }

    @Test
    void rejectsSlotThatIsNoLongerAvailable() {
        UUID key = UUID.randomUUID();
        stubBookingDependencies(key);
        when(availability.availability(anyString(), any(), any(), any()))
                .thenReturn(List.of(new AvailabilityDayResponse(LocalDate.of(2026, 6, 22), List.of())));

        assertThatThrownBy(() -> appointmentService.book("grace@example.com",
                new BookAppointmentRequest("ada", offering.getId(), start, key)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("ya no esta disponible");
        verify(appointments, never()).saveAndFlush(any());
    }

    @Test
    void customerCanCancelOutsideNoticeWindow() {
        Appointment appointment = appointment(UUID.randomUUID(), start);
        when(users.findByEmailIgnoreCase("grace@example.com")).thenReturn(Optional.of(customer));
        when(appointments.findByIdAndCustomerId(appointment.getId(), customer.getId())).thenReturn(Optional.of(appointment));
        when(policies.findByProfessionalId(professional.getId())).thenReturn(Optional.of(policy));

        AppointmentResponse cancelled = appointmentService.cancelByCustomer("grace@example.com", appointment.getId(),
                new CancelAppointmentRequest("Cambio de planes"));

        assertThat(cancelled.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelled.cancellationReason()).isEqualTo("Cambio de planes");
        verify(history).save(any(AppointmentStatusHistory.class));
        verify(notifications).cancelled(appointment);
        verify(calendar).cancelled(appointment);
    }

    @Test
    void professionalCannotCompleteFutureAppointment() {
        Appointment appointment = appointment(UUID.randomUUID(), start);
        when(profiles.findByUserEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(professional));
        when(appointments.findByIdAndProfessionalId(appointment.getId(), professional.getId()))
                .thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.updateByProfessional("ada@example.com", appointment.getId(),
                new UpdateAppointmentStatusRequest(AppointmentStatus.COMPLETED, null)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("aun no ha comenzado");
    }

    private void stubBookingDependencies(UUID key) {
        when(users.findByEmailIgnoreCase("grace@example.com")).thenReturn(Optional.of(customer));
        when(appointments.findByCustomerIdAndIdempotencyKey(customer.getId(), key)).thenReturn(Optional.empty());
        when(profiles.findBySlugAndPublishedTrue("ada")).thenReturn(Optional.of(professional));
        when(services.findByIdAndProfessionalIdAndActiveTrue(offering.getId(), professional.getId()))
                .thenReturn(Optional.of(offering));
    }

    private Appointment appointment(UUID key, Instant appointmentStart) {
        return new Appointment(professional, customer, offering, key, appointmentStart,
                appointmentStart.plusSeconds(3600), appointmentStart.plusSeconds(4500));
    }
}
