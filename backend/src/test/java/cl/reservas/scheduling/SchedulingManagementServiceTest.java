package cl.reservas.scheduling;

import cl.reservas.professional.ProfessionalProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SchedulingManagementServiceTest {
    @Mock ProfessionalProfileRepository profiles;
    @Mock WeeklySchedulePeriodRepository periods;
    @Mock BookingPolicyRepository policies;
    @Mock ScheduleExceptionRepository exceptions;

    @Test
    void rejectsOverlappingWeeklyPeriodsBeforeWriting() {
        var service = new SchedulingManagementService(profiles, periods, policies, exceptions);
        var request = new WeeklyScheduleRequest(List.of(
                new SchedulePeriodRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new SchedulePeriodRequest(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(14, 0))));

        assertThatThrownBy(() -> service.replaceWeeklySchedule("ada@example.com", request))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("superponerse");
        verifyNoInteractions(profiles, periods);
    }

    @Test
    void additionalAvailabilityRequiresStartAndEndTimes() {
        var service = new SchedulingManagementService(profiles, periods, policies, exceptions);
        var request = new ScheduleExceptionRequest(LocalDate.now(), ScheduleExceptionType.AVAILABLE,
                null, null, "Extra");

        assertThatThrownBy(() -> service.createException("ada@example.com", request))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("requiere hora");
        verifyNoInteractions(profiles, exceptions);
    }
}
