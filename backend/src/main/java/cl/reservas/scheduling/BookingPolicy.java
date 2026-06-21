package cl.reservas.scheduling;

import cl.reservas.professional.ProfessionalProfile;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_policies")
public class BookingPolicy {
    @Id private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "professional_id", unique = true)
    private ProfessionalProfile professional;
    @Column(name = "minimum_notice_minutes", nullable = false) private int minimumNoticeMinutes;
    @Column(name = "booking_window_days", nullable = false) private int bookingWindowDays;
    @Column(name = "slot_interval_minutes", nullable = false) private int slotIntervalMinutes;
    @Column(name = "buffer_after_minutes", nullable = false) private int bufferAfterMinutes;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    protected BookingPolicy() {}

    public BookingPolicy(ProfessionalProfile professional) {
        this.id = UUID.randomUUID();
        this.professional = professional;
        update(120, 60, 15, 0);
    }

    public void update(int minimumNoticeMinutes, int bookingWindowDays,
                       int slotIntervalMinutes, int bufferAfterMinutes) {
        this.minimumNoticeMinutes = minimumNoticeMinutes;
        this.bookingWindowDays = bookingWindowDays;
        this.slotIntervalMinutes = slotIntervalMinutes;
        this.bufferAfterMinutes = bufferAfterMinutes;
        this.updatedAt = Instant.now();
    }

    public ProfessionalProfile getProfessional() { return professional; }
    public int getMinimumNoticeMinutes() { return minimumNoticeMinutes; }
    public int getBookingWindowDays() { return bookingWindowDays; }
    public int getSlotIntervalMinutes() { return slotIntervalMinutes; }
    public int getBufferAfterMinutes() { return bufferAfterMinutes; }
}
