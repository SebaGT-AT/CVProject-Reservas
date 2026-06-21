package cl.reservas.scheduling;

import cl.reservas.common.exception.NotFoundException;
import cl.reservas.professional.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {
    private final ProfessionalProfileRepository profiles;
    private final ServiceOfferingRepository services;
    private final WeeklySchedulePeriodRepository periods;
    private final ScheduleExceptionRepository exceptions;
    private final BookingPolicyRepository policies;
    private final Clock clock;

    public AvailabilityService(ProfessionalProfileRepository profiles, ServiceOfferingRepository services,
                               WeeklySchedulePeriodRepository periods, ScheduleExceptionRepository exceptions,
                               BookingPolicyRepository policies, Clock clock) {
        this.profiles = profiles;
        this.services = services;
        this.periods = periods;
        this.exceptions = exceptions;
        this.policies = policies;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityDayResponse> availability(String slug, UUID serviceId,
                                                      LocalDate from, LocalDate to) {
        SchedulingManagementService.validateRange(from, to, 31);
        ProfessionalProfile profile = profiles.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new NotFoundException("Profesional no encontrado"));
        ServiceOffering offering = services.findByIdAndProfessionalIdAndActiveTrue(serviceId, profile.getId())
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
        BookingPolicy policy = policies.findByProfessionalId(profile.getId()).orElseGet(() -> new BookingPolicy(profile));
        ZoneId zone = ZoneId.of(profile.getTimeZone());
        Instant now = clock.instant();
        Instant earliest = now.plus(Duration.ofMinutes(policy.getMinimumNoticeMinutes()));
        Instant latest = now.plus(Duration.ofDays(policy.getBookingWindowDays()));

        Map<DayOfWeek, List<LocalInterval>> weekly = periods
                .findAllByProfessionalIdOrderByDayOfWeekAscStartTimeAsc(profile.getId()).stream()
                .collect(Collectors.groupingBy(WeeklySchedulePeriod::getDayOfWeek,
                        () -> new EnumMap<>(DayOfWeek.class),
                        Collectors.mapping(item -> new LocalInterval(item.getStartTime(), item.getEndTime()), Collectors.toList())));
        Map<LocalDate, List<ScheduleException>> byDate = exceptions
                .findAllByProfessionalIdAndDateBetweenOrderByDateAscStartTimeAsc(profile.getId(), from, to).stream()
                .collect(Collectors.groupingBy(ScheduleException::getDate));

        List<AvailabilityDayResponse> result = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<LocalInterval> available = intervalsFor(date, weekly, byDate.getOrDefault(date, List.of()));
            List<AvailabilitySlotResponse> slots = slotsFor(date, available, offering.getDurationMinutes(),
                    policy, zone, earliest, latest);
            result.add(new AvailabilityDayResponse(date, slots));
        }
        return result;
    }

    private List<LocalInterval> intervalsFor(LocalDate date,
                                             Map<DayOfWeek, List<LocalInterval>> weekly,
                                             List<ScheduleException> dateExceptions) {
        List<LocalInterval> additions = dateExceptions.stream()
                .filter(item -> item.getType() == ScheduleExceptionType.AVAILABLE)
                .map(item -> new LocalInterval(item.getStartTime(), item.getEndTime())).toList();
        List<LocalInterval> current = new ArrayList<>(additions.isEmpty()
                ? weekly.getOrDefault(date.getDayOfWeek(), List.of()) : additions);
        current = merge(current);
        for (ScheduleException exception : dateExceptions) {
            if (exception.getType() != ScheduleExceptionType.BLOCKED) continue;
            if (exception.getStartTime() == null) return List.of();
            current = subtract(current, new LocalInterval(exception.getStartTime(), exception.getEndTime()));
        }
        return current;
    }

    private List<AvailabilitySlotResponse> slotsFor(LocalDate date, List<LocalInterval> intervals,
                                                    int durationMinutes, BookingPolicy policy, ZoneId zone,
                                                    Instant earliest, Instant latest) {
        List<AvailabilitySlotResponse> slots = new ArrayList<>();
        for (LocalInterval interval : intervals) {
            for (LocalTime cursor = interval.start(); ; cursor = cursor.plusMinutes(policy.getSlotIntervalMinutes())) {
                long requiredMinutes = (long) durationMinutes + policy.getBufferAfterMinutes();
                if (Duration.between(cursor, interval.end()).toMinutes() >= requiredMinutes) {
                    ZonedDateTime start = ZonedDateTime.of(date, cursor, zone);
                    if (!start.toLocalDate().equals(date) || !start.toLocalTime().equals(cursor)) continue;
                    Instant startInstant = start.toInstant();
                    if (!startInstant.isBefore(earliest) && !startInstant.isAfter(latest)) {
                        ZonedDateTime end = start.plusMinutes(durationMinutes);
                        slots.add(new AvailabilitySlotResponse(startInstant, end.toInstant(),
                                start.toLocalTime(), end.toLocalTime()));
                    }
                } else break;
            }
        }
        return slots;
    }

    private List<LocalInterval> merge(List<LocalInterval> intervals) {
        if (intervals.isEmpty()) return List.of();
        List<LocalInterval> sorted = intervals.stream().sorted(Comparator.comparing(LocalInterval::start)).toList();
        List<LocalInterval> merged = new ArrayList<>();
        for (LocalInterval next : sorted) {
            if (merged.isEmpty() || next.start().isAfter(merged.getLast().end())) merged.add(next);
            else {
                LocalInterval previous = merged.removeLast();
                merged.add(new LocalInterval(previous.start(), previous.end().isAfter(next.end()) ? previous.end() : next.end()));
            }
        }
        return merged;
    }

    private List<LocalInterval> subtract(List<LocalInterval> source, LocalInterval blocked) {
        List<LocalInterval> result = new ArrayList<>();
        for (LocalInterval interval : source) {
            if (!blocked.start().isBefore(interval.end()) || !blocked.end().isAfter(interval.start())) {
                result.add(interval);
                continue;
            }
            if (blocked.start().isAfter(interval.start())) result.add(new LocalInterval(interval.start(), blocked.start()));
            if (blocked.end().isBefore(interval.end())) result.add(new LocalInterval(blocked.end(), interval.end()));
        }
        return result;
    }

    private record LocalInterval(LocalTime start, LocalTime end) {}
}
