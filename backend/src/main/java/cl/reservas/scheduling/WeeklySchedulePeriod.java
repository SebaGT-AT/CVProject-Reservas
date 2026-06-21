package cl.reservas.scheduling;

import cl.reservas.professional.ProfessionalProfile;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "weekly_schedule_periods")
public class WeeklySchedulePeriod {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "professional_id")
    private ProfessionalProfile professional;
    @Column(name = "day_of_week", nullable = false) private int dayOfWeek;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time", nullable = false) private LocalTime endTime;

    protected WeeklySchedulePeriod() {}

    public WeeklySchedulePeriod(ProfessionalProfile professional, DayOfWeek dayOfWeek,
                                LocalTime startTime, LocalTime endTime) {
        this.id = UUID.randomUUID();
        this.professional = professional;
        this.dayOfWeek = dayOfWeek.getValue();
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public UUID getId() { return id; }
    public DayOfWeek getDayOfWeek() { return DayOfWeek.of(dayOfWeek); }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}
