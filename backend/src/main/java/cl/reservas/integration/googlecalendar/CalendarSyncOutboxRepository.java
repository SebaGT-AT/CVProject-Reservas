package cl.reservas.integration.googlecalendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CalendarSyncOutboxRepository extends JpaRepository<CalendarSyncOutbox, UUID> {
    @Modifying
    @Query(value = """
            INSERT INTO calendar_sync_outbox (
                id, appointment_id, professional_id, operation, deduplication_key, payload,
                status, attempts, next_attempt_at, created_at, updated_at
            ) VALUES (
                :id, :appointmentId, :professionalId, :operation, :deduplicationKey, CAST(:payload AS jsonb),
                'PENDING', 0, :now, :now, :now
            ) ON CONFLICT (deduplication_key) DO NOTHING
            """, nativeQuery = true)
    int insertEvent(@Param("id") UUID id,
                    @Param("appointmentId") UUID appointmentId,
                    @Param("professionalId") UUID professionalId,
                    @Param("operation") String operation,
                    @Param("deduplicationKey") String deduplicationKey,
                    @Param("payload") String payload,
                    @Param("now") Instant now);

    @Query(value = """
            SELECT * FROM calendar_sync_outbox
            WHERE (status IN ('PENDING', 'FAILED') AND next_attempt_at <= :now)
               OR (status = 'PROCESSING' AND locked_at < :staleBefore)
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<CalendarSyncOutbox> findClaimable(@Param("now") Instant now,
                                           @Param("staleBefore") Instant staleBefore,
                                           @Param("batchSize") int batchSize);
}
