package cl.reservas.appointment;

import cl.reservas.common.exception.ConflictException;
import cl.reservas.common.exception.NotFoundException;
import cl.reservas.professional.*;
import cl.reservas.scheduling.*;
import cl.reservas.user.Role;
import cl.reservas.user.User;
import cl.reservas.user.UserRepository;
import cl.reservas.integration.googlecalendar.AppointmentCalendarPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {
    private final AppointmentRepository appointments;
    private final AppointmentStatusHistoryRepository history;
    private final ProfessionalProfileRepository profiles;
    private final ServiceOfferingRepository services;
    private final BookingPolicyRepository policies;
    private final UserRepository users;
    private final AvailabilityService availability;
    private final AppointmentNotificationPublisher notifications;
    private final AppointmentCalendarPublisher calendar;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointments, AppointmentStatusHistoryRepository history,
                              ProfessionalProfileRepository profiles, ServiceOfferingRepository services,
                              BookingPolicyRepository policies, UserRepository users,
                              AvailabilityService availability, AppointmentNotificationPublisher notifications,
                              AppointmentCalendarPublisher calendar, Clock clock) {
        this.appointments = appointments;
        this.history = history;
        this.profiles = profiles;
        this.services = services;
        this.policies = policies;
        this.users = users;
        this.availability = availability;
        this.notifications = notifications;
        this.calendar = calendar;
        this.clock = clock;
    }

    @Transactional
    public AppointmentBookingResult book(String customerEmail, BookAppointmentRequest request) {
        User customer = requireUser(customerEmail);
        if (customer.getRole() != Role.CUSTOMER) throw new IllegalArgumentException("Solo una cuenta cliente puede reservar");
        var previous = appointments.findByCustomerIdAndIdempotencyKey(customer.getId(), request.idempotencyKey());
        if (previous.isPresent()) return new AppointmentBookingResult(AppointmentResponse.from(previous.get()), false);

        ProfessionalProfile professional = profiles.findBySlugAndPublishedTrue(request.professionalSlug())
                .orElseThrow(() -> new NotFoundException("Profesional no encontrado"));
        ServiceOffering service = services.findByIdAndProfessionalIdAndActiveTrue(request.serviceId(), professional.getId())
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
        ZoneId zone = ZoneId.of(professional.getTimeZone());
        LocalDate localDate = request.startAt().atZone(zone).toLocalDate();
        boolean isAvailable = availability.availability(professional.getSlug(), service.getId(), localDate, localDate)
                .stream().flatMap(day -> day.slots().stream())
                .anyMatch(slot -> slot.startAt().equals(request.startAt()));
        if (!isAvailable) throw new ConflictException("El horario ya no esta disponible");

        BookingPolicy policy = policies.findByProfessionalId(professional.getId())
                .orElseGet(() -> new BookingPolicy(professional));
        Instant endAt = request.startAt().plus(Duration.ofMinutes(service.getDurationMinutes()));
        Instant busyUntil = endAt.plus(Duration.ofMinutes(policy.getBufferAfterMinutes()));
        Appointment appointment = new Appointment(professional, customer, service, request.idempotencyKey(),
                request.startAt(), endAt, busyUntil);
        try {
            appointments.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("El horario acaba de ser reservado por otra persona");
        }
        history.save(new AppointmentStatusHistory(appointment, null, AppointmentStatus.CONFIRMED, customer,
                "Reserva creada por el cliente"));
        notifications.confirmed(appointment);
        calendar.confirmed(appointment);
        return new AppointmentBookingResult(AppointmentResponse.from(appointment), true);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> customerHistory(String email) {
        User customer = requireUser(email);
        return appointments.findAllByCustomerIdOrderByStartAtDesc(customer.getId()).stream()
                .map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> professionalAppointments(String email, Instant from, Instant to) {
        validateRange(from, to);
        ProfessionalProfile professional = requireProfessional(email);
        return appointments.findAllByProfessionalIdAndStartAtBetweenOrderByStartAtAsc(
                professional.getId(), from, to).stream().map(AppointmentResponse::from).toList();
    }

    @Transactional
    public AppointmentResponse cancelByCustomer(String email, UUID appointmentId, CancelAppointmentRequest request) {
        User customer = requireUser(email);
        Appointment appointment = appointments.findByIdAndCustomerId(appointmentId, customer.getId())
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
        ensureCancellable(appointment);
        BookingPolicy policy = policies.findByProfessionalId(appointment.getProfessional().getId())
                .orElseGet(() -> new BookingPolicy(appointment.getProfessional()));
        if (appointment.getStartAt().isBefore(clock.instant().plus(Duration.ofMinutes(policy.getCancellationNoticeMinutes())))) {
            throw new ConflictException("La cita ya no puede cancelarse en linea por la politica de anticipacion");
        }
        AppointmentStatus previous = appointment.getStatus();
        appointment.cancel(customer, clean(request.reason()));
        history.save(new AppointmentStatusHistory(appointment, previous, AppointmentStatus.CANCELLED,
                customer, clean(request.reason())));
        notifications.cancelled(appointment);
        calendar.cancelled(appointment);
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse updateByProfessional(String email, UUID appointmentId,
                                                    UpdateAppointmentStatusRequest request) {
        ProfessionalProfile professional = requireProfessional(email);
        Appointment appointment = appointments.findByIdAndProfessionalId(appointmentId, professional.getId())
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
        User actor = professional.getUser();
        AppointmentStatus previous = appointment.getStatus();
        validateTransition(previous, request.status(), appointment.getStartAt());
        String reason = clean(request.reason());
        if (request.status() == AppointmentStatus.CANCELLED) appointment.cancel(actor, reason);
        else appointment.transitionTo(request.status());
        history.save(new AppointmentStatusHistory(appointment, previous, request.status(), actor, reason));
        if (request.status() == AppointmentStatus.CONFIRMED) notifications.confirmed(appointment);
        if (request.status() == AppointmentStatus.CANCELLED) notifications.cancelled(appointment);
        if (request.status() == AppointmentStatus.CONFIRMED) calendar.confirmed(appointment);
        if (request.status() == AppointmentStatus.CANCELLED) calendar.cancelled(appointment);
        return AppointmentResponse.from(appointment);
    }

    private void ensureCancellable(Appointment appointment) {
        if (!EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED).contains(appointment.getStatus())) {
            throw new ConflictException("La cita ya no admite cancelacion");
        }
    }

    private void validateTransition(AppointmentStatus current, AppointmentStatus next, Instant startAt) {
        boolean allowed = switch (current) {
            case PENDING -> next == AppointmentStatus.CONFIRMED || next == AppointmentStatus.CANCELLED;
            case CONFIRMED -> next == AppointmentStatus.COMPLETED || next == AppointmentStatus.NO_SHOW
                    || next == AppointmentStatus.CANCELLED;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };
        if (!allowed) throw new ConflictException("La transicion de estado no esta permitida");
        if ((next == AppointmentStatus.COMPLETED || next == AppointmentStatus.NO_SHOW)
                && clock.instant().isBefore(startAt)) {
            throw new ConflictException("La cita aun no ha comenzado");
        }
    }

    private void validateRange(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) throw new IllegalArgumentException("El rango no es valido");
        if (ChronoUnit.DAYS.between(from, to) > 366) throw new IllegalArgumentException("El rango es demasiado amplio");
    }

    private ProfessionalProfile requireProfessional(String email) {
        return profiles.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Perfil profesional no encontrado"));
    }

    private User requireUser(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
