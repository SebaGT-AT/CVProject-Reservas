package cl.reservas.scheduling;

import cl.reservas.professional.*;
import cl.reservas.user.Role;
import cl.reservas.user.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {
    @Mock ProfessionalProfileRepository profiles;
    @Mock ServiceOfferingRepository services;
    @Mock WeeklySchedulePeriodRepository periods;
    @Mock ScheduleExceptionRepository exceptions;
    @Mock BookingPolicyRepository policies;
    private AvailabilityService availability;
    private ProfessionalProfile profile;
    private ServiceOffering offering;
    private BookingPolicy policy;
    private final LocalDate monday = LocalDate.of(2026, 6, 22);

    @BeforeEach
    void setUp() {
        User user = new User("Ada", "ada@example.com", "encoded", Role.PROFESSIONAL);
        profile = new ProfessionalProfile(user, "ada", null, null, "America/Santiago", true, Set.of());
        offering = new ServiceOffering(profile, "Consulta", null, 60, new BigDecimal("25000"), "CLP", true);
        policy = new BookingPolicy(profile);
        policy.update(0, 30, 30, 0);
        Clock clock = Clock.fixed(Instant.parse("2026-06-22T00:00:00Z"), ZoneOffset.UTC);
        availability = new AvailabilityService(profiles, services, periods, exceptions, policies, clock);

        when(profiles.findBySlugAndPublishedTrue("ada")).thenReturn(Optional.of(profile));
        when(services.findByIdAndProfessionalIdAndActiveTrue(offering.getId(), profile.getId()))
                .thenReturn(Optional.of(offering));
        when(policies.findByProfessionalId(profile.getId())).thenReturn(Optional.of(policy));
        when(periods.findAllByProfessionalIdOrderByDayOfWeekAscStartTimeAsc(profile.getId()))
                .thenReturn(List.of(new WeeklySchedulePeriod(profile, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(12, 0))));
    }

    @Test
    void createsSlotsUsingServiceDurationAndConfiguredInterval() {
        when(exceptions.findAllByProfessionalIdAndDateBetweenOrderByDateAscStartTimeAsc(profile.getId(), monday, monday))
                .thenReturn(List.of());

        var result = availability.availability("ada", offering.getId(), monday, monday);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().slots()).extracting(AvailabilitySlotResponse::localStartTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0),
                        LocalTime.of(10, 30), LocalTime.of(11, 0));
        assertThat(result.getFirst().slots().getFirst().startAt())
                .isEqualTo(Instant.parse("2026-06-22T13:00:00Z"));
    }

    @Test
    void partialBlockIsSubtractedFromWeeklyAvailability() {
        ScheduleException blocked = new ScheduleException(profile, monday, ScheduleExceptionType.BLOCKED,
                LocalTime.of(10, 0), LocalTime.of(11, 0), "Reunion");
        when(exceptions.findAllByProfessionalIdAndDateBetweenOrderByDateAscStartTimeAsc(profile.getId(), monday, monday))
                .thenReturn(List.of(blocked));

        var slots = availability.availability("ada", offering.getId(), monday, monday).getFirst().slots();

        assertThat(slots).extracting(AvailabilitySlotResponse::localStartTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(11, 0));
    }

    @Test
    void specialAvailabilityOverridesUsualClosedDay() {
        LocalDate tuesday = monday.plusDays(1);
        ScheduleException special = new ScheduleException(profile, tuesday, ScheduleExceptionType.AVAILABLE,
                LocalTime.of(14, 0), LocalTime.of(16, 0), "Horario especial");
        when(exceptions.findAllByProfessionalIdAndDateBetweenOrderByDateAscStartTimeAsc(profile.getId(), tuesday, tuesday))
                .thenReturn(List.of(special));

        var slots = availability.availability("ada", offering.getId(), tuesday, tuesday).getFirst().slots();

        assertThat(slots).extracting(AvailabilitySlotResponse::localStartTime)
                .containsExactly(LocalTime.of(14, 0), LocalTime.of(14, 30), LocalTime.of(15, 0));
    }

    @Test
    void fullDayBlockRemovesEverySlot() {
        ScheduleException blocked = new ScheduleException(profile, monday, ScheduleExceptionType.BLOCKED,
                null, null, "Feriado");
        when(exceptions.findAllByProfessionalIdAndDateBetweenOrderByDateAscStartTimeAsc(profile.getId(), monday, monday))
                .thenReturn(List.of(blocked));

        assertThat(availability.availability("ada", offering.getId(), monday, monday).getFirst().slots()).isEmpty();
    }

    @Test
    void minimumNoticeCanExcludeOtherwiseValidSlots() {
        policy.update(18 * 60, 30, 30, 0);
        when(exceptions.findAllByProfessionalIdAndDateBetweenOrderByDateAscStartTimeAsc(profile.getId(), monday, monday))
                .thenReturn(List.of());

        assertThat(availability.availability("ada", offering.getId(), monday, monday).getFirst().slots()).isEmpty();
    }
}
