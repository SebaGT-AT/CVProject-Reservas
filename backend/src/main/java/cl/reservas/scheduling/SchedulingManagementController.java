package cl.reservas.scheduling;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/professional/schedule")
@PreAuthorize("hasRole('PROFESSIONAL')")
public class SchedulingManagementController {
    private final SchedulingManagementService scheduling;

    public SchedulingManagementController(SchedulingManagementService scheduling) { this.scheduling = scheduling; }

    @GetMapping("/weekly")
    public List<SchedulePeriodResponse> weekly(Authentication authentication) {
        return scheduling.weeklySchedule(authentication.getName());
    }

    @PutMapping("/weekly")
    public List<SchedulePeriodResponse> replaceWeekly(Authentication authentication,
                                                       @Valid @RequestBody WeeklyScheduleRequest request) {
        return scheduling.replaceWeeklySchedule(authentication.getName(), request);
    }

    @GetMapping("/policy")
    public BookingPolicyResponse policy(Authentication authentication) {
        return scheduling.policy(authentication.getName());
    }

    @PutMapping("/policy")
    public BookingPolicyResponse updatePolicy(Authentication authentication,
                                               @Valid @RequestBody BookingPolicyRequest request) {
        return scheduling.updatePolicy(authentication.getName(), request);
    }

    @GetMapping("/exceptions")
    public List<ScheduleExceptionResponse> exceptions(Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return scheduling.exceptions(authentication.getName(), from, to);
    }

    @PostMapping("/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleExceptionResponse createException(Authentication authentication,
            @Valid @RequestBody ScheduleExceptionRequest request) {
        return scheduling.createException(authentication.getName(), request);
    }

    @DeleteMapping("/exceptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteException(Authentication authentication, @PathVariable UUID id) {
        scheduling.deleteException(authentication.getName(), id);
    }
}
