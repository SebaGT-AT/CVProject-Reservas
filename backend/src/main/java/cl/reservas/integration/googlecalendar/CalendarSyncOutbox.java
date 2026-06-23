package cl.reservas.integration.googlecalendar;

import cl.reservas.notification.OutboxStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calendar_sync_outbox")
public class CalendarSyncOutbox {
    @Id private UUID id;
    @Column(name = "appointment_id", nullable = false) private UUID appointmentId;
    @Column(name = "professional_id", nullable = false) private UUID professionalId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CalendarSyncOperation operation;
    @Column(name = "deduplication_key", nullable = false, unique = true, length = 160) private String deduplicationKey;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private String payload;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OutboxStatus status;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "locked_at") private Instant lockedAt;
    @Column(name = "processed_at") private Instant processedAt;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected CalendarSyncOutbox() {}

    public void claim(Instant now) {
        status = OutboxStatus.PROCESSING;
        lockedAt = now;
        updatedAt = now;
    }

    public void markSent(Instant now) {
        status = OutboxStatus.SENT;
        processedAt = now;
        lockedAt = null;
        lastError = null;
        updatedAt = now;
    }

    public void markFailed(String error, Instant retryAt, int maximumAttempts, Instant now) {
        attempts++;
        status = attempts >= maximumAttempts ? OutboxStatus.DEAD : OutboxStatus.FAILED;
        nextAttemptAt = retryAt;
        lockedAt = null;
        lastError = error == null ? "Error desconocido" : error.substring(0, Math.min(1000, error.length()));
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getProfessionalId() { return professionalId; }
    public CalendarSyncOperation getOperation() { return operation; }
    public String getPayload() { return payload; }
    public int getAttempts() { return attempts; }
}
