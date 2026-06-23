package cl.reservas.integration.googlecalendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class CalendarSyncStateService {
    private final CalendarSyncOutboxRepository outbox;
    private final GoogleCalendarConnectionRepository connections;
    private final Clock clock;
    private final int batchSize;
    private final int maximumAttempts;

    CalendarSyncStateService(CalendarSyncOutboxRepository outbox,
                             GoogleCalendarConnectionRepository connections, Clock clock,
                             @Value("${app.integrations.google-calendar.batch-size:20}") int batchSize,
                             @Value("${app.integrations.google-calendar.maximum-attempts:8}") int maximumAttempts) {
        this.outbox = outbox;
        this.connections = connections;
        this.clock = clock;
        this.batchSize = batchSize;
        this.maximumAttempts = maximumAttempts;
    }

    @Transactional
    List<CalendarSyncMessage> claimBatch() {
        Instant now = clock.instant();
        List<CalendarSyncOutbox> claimed = outbox.findClaimable(
                now, now.minus(Duration.ofMinutes(10)), batchSize);
        claimed.forEach(item -> item.claim(now));
        return claimed.stream().map(CalendarSyncMessage::from).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void markSent(UUID id) {
        outbox.findById(id).ifPresent(item -> item.markSent(clock.instant()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void markFailed(UUID id, int previousAttempts, Exception exception) {
        Instant now = clock.instant();
        long delaySeconds = Math.min(3600, 30L * (1L << Math.min(previousAttempts, 7)));
        outbox.findById(id).ifPresent(item -> item.markFailed(
                exception.getMessage(), now.plusSeconds(delaySeconds), maximumAttempts, now));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void requireReauthorization(UUID professionalId, String error) {
        connections.findById(professionalId)
                .ifPresent(connection -> connection.requireReauthorization(error, clock.instant()));
    }
}
