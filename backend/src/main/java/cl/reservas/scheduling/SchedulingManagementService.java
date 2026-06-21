package cl.reservas.scheduling;

import cl.reservas.common.exception.NotFoundException;
import cl.reservas.professional.ProfessionalProfile;
import cl.reservas.professional.ProfessionalProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class SchedulingManagementService {
    private final ProfessionalProfileRepository profiles;
    private final WeeklySchedulePeriodRepository periods;
    private final BookingPolicyRepository policies;
    private final ScheduleExceptionRepository exceptions;

    public SchedulingManagementService(ProfessionalProfileRepository profiles,
                                       WeeklySchedulePeriodRepository periods,
                                       BookingPolicyRepository policies,
                                       ScheduleExceptionRepository exceptions) {
        this.profiles = profiles;
        this.periods = periods;
        this.policies = policies;
        this.exceptions = exceptions;
    }

    @Transactional(readOnly = true)
    public List<SchedulePeriodResponse> weeklySchedule(String email) {
        ProfessionalProfile profile = requireProfile(email);
        return periods.findAllByProfessionalIdOrderByDayOfWeekAscStartTimeAsc(profile.getId()).stream()
                .map(SchedulePeriodResponse::from).toList();
    }

    @Transactional
    public List<SchedulePeriodResponse> replaceWeeklySchedule(String email, WeeklyScheduleRequest request) {
        validatePeriods(request.periods());
        ProfessionalProfile profile = requireProfile(email);
        periods.deleteAllByProfessionalId(profile.getId());
        List<WeeklySchedulePeriod> replacements = request.periods().stream()
                .map(item -> new WeeklySchedulePeriod(profile, item.dayOfWeek(), item.startTime(), item.endTime()))
                .toList();
        return periods.saveAll(replacements).stream()
                .sorted(Comparator.comparing(WeeklySchedulePeriod::getDayOfWeek)
                        .thenComparing(WeeklySchedulePeriod::getStartTime))
                .map(SchedulePeriodResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BookingPolicyResponse policy(String email) {
        ProfessionalProfile profile = requireProfile(email);
        BookingPolicy policy = policies.findByProfessionalId(profile.getId())
                .orElseGet(() -> new BookingPolicy(profile));
        return BookingPolicyResponse.from(policy);
    }

    @Transactional
    public BookingPolicyResponse updatePolicy(String email, BookingPolicyRequest request) {
        ProfessionalProfile profile = requireProfile(email);
        BookingPolicy policy = policies.findByProfessionalId(profile.getId())
                .orElseGet(() -> new BookingPolicy(profile));
        policy.update(request.minimumNoticeMinutes(), request.bookingWindowDays(),
                request.slotIntervalMinutes(), request.bufferAfterMinutes());
        return BookingPolicyResponse.from(policies.save(policy));
    }

    @Transactional(readOnly = true)
    public List<ScheduleExceptionResponse> exceptions(String email, LocalDate from, LocalDate to) {
        validateRange(from, to, 366);
        ProfessionalProfile profile = requireProfile(email);
        return exceptions.findAllByProfessionalIdAndDateBetweenOrderByDateAscStartTimeAsc(
                profile.getId(), from, to).stream().map(ScheduleExceptionResponse::from).toList();
    }

    @Transactional
    public ScheduleExceptionResponse createException(String email, ScheduleExceptionRequest request) {
        validateException(request);
        ProfessionalProfile profile = requireProfile(email);
        ScheduleException exception = new ScheduleException(profile, request.date(), request.type(),
                request.startTime(), request.endTime(), clean(request.reason()));
        return ScheduleExceptionResponse.from(exceptions.save(exception));
    }

    @Transactional
    public void deleteException(String email, UUID id) {
        ProfessionalProfile profile = requireProfile(email);
        ScheduleException exception = exceptions.findByIdAndProfessionalId(id, profile.getId())
                .orElseThrow(() -> new NotFoundException("Excepcion de agenda no encontrada"));
        exceptions.delete(exception);
    }

    private void validatePeriods(List<SchedulePeriodRequest> requested) {
        Map<java.time.DayOfWeek, List<SchedulePeriodRequest>> byDay = new EnumMap<>(java.time.DayOfWeek.class);
        for (SchedulePeriodRequest period : requested) {
            if (!period.startTime().isBefore(period.endTime())) {
                throw new IllegalArgumentException("Cada horario debe terminar despues de comenzar");
            }
            byDay.computeIfAbsent(period.dayOfWeek(), ignored -> new ArrayList<>()).add(period);
        }
        byDay.values().forEach(dayPeriods -> {
            dayPeriods.sort(Comparator.comparing(SchedulePeriodRequest::startTime));
            for (int index = 1; index < dayPeriods.size(); index++) {
                if (dayPeriods.get(index).startTime().isBefore(dayPeriods.get(index - 1).endTime())) {
                    throw new IllegalArgumentException("Los horarios de un mismo dia no pueden superponerse");
                }
            }
        });
    }

    private void validateException(ScheduleExceptionRequest request) {
        boolean bothAbsent = request.startTime() == null && request.endTime() == null;
        boolean bothPresent = request.startTime() != null && request.endTime() != null;
        if (!bothAbsent && !bothPresent) throw new IllegalArgumentException("Indica ambas horas o ninguna");
        if (bothPresent && !request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException("La hora final debe ser posterior a la inicial");
        }
        if (request.type() == ScheduleExceptionType.AVAILABLE && !bothPresent) {
            throw new IllegalArgumentException("Una disponibilidad adicional requiere hora inicial y final");
        }
    }

    static void validateRange(LocalDate from, LocalDate to, int maximumDays) {
        if (from == null || to == null || to.isBefore(from)) throw new IllegalArgumentException("El rango de fechas no es valido");
        if (java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1 > maximumDays) {
            throw new IllegalArgumentException("El rango solicitado es demasiado amplio");
        }
    }

    private ProfessionalProfile requireProfile(String email) {
        return profiles.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Completa primero tu perfil profesional"));
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
