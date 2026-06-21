package cl.reservas.scheduling;

import cl.reservas.professional.ProfessionalProfile;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "schedule_exceptions")
public class ScheduleException {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "professional_id")
    private ProfessionalProfile professional;
    @Column(name = "exception_date", nullable = false) private LocalDate date;
    @Enumerated(EnumType.STRING) @Column(name = "exception_type", nullable = false, length = 20)
    private ScheduleExceptionType type;
    @Column(name = "start_time") private LocalTime startTime;
    @Column(name = "end_time") private LocalTime endTime;
    @Column(length = 200) private String reason;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected ScheduleException() {}

    public ScheduleException(ProfessionalProfile professional, LocalDate date, ScheduleExceptionType type,
                             LocalTime startTime, LocalTime endTime, String reason) {
        this.id = UUID.randomUUID();
        this.professional = professional;
        this.date = date;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public LocalDate getDate() { return date; }
    public ScheduleExceptionType getType() { return type; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getReason() { return reason; }
}
