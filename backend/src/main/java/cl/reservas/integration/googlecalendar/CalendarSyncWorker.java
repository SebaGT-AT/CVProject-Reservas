package cl.reservas.integration.googlecalendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.integrations.google-calendar.enabled", havingValue = "true")
class CalendarSyncWorker {
    private static final Logger log = LoggerFactory.getLogger(CalendarSyncWorker.class);
    private final CalendarSyncStateService state;
    private final GoogleCalendarConnectionRepository connections;
    private final GoogleCalendarGateway gateway;
    private final SecretCipher cipher;
    private final ObjectMapper objectMapper;

    CalendarSyncWorker(CalendarSyncStateService state, GoogleCalendarConnectionRepository connections,
                       GoogleCalendarGateway gateway, SecretCipher cipher, ObjectMapper objectMapper) {
        this.state = state;
        this.connections = connections;
        this.gateway = gateway;
        this.cipher = cipher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.integrations.google-calendar.poll-delay-ms:5000}",
            initialDelayString = "${app.integrations.google-calendar.initial-delay-ms:10000}")
    void dispatch() {
        for (CalendarSyncMessage message : state.claimBatch()) dispatch(message);
    }

    private void dispatch(CalendarSyncMessage message) {
        try {
            var connection = connections.findById(message.professionalId());
            if (connection.isEmpty()) {
                state.markSent(message.id());
                return;
            }
            CalendarSyncPayload payload = objectMapper.readValue(message.payload(), CalendarSyncPayload.class);
            String refreshToken = cipher.decrypt(connection.get().getEncryptedRefreshToken());
            String accessToken = gateway.refreshAccessToken(refreshToken);
            if (message.operation() == CalendarSyncOperation.UPSERT) {
                gateway.upsertEvent(accessToken, connection.get().getCalendarId(), payload);
            } else {
                gateway.deleteEvent(accessToken, connection.get().getCalendarId(), payload.eventId());
            }
            state.markSent(message.id());
        } catch (GoogleAuthorizationException exception) {
            state.requireReauthorization(message.professionalId(), exception.getMessage());
            state.markFailed(message.id(), message.attempts(), exception);
            log.warn("Google Calendar authorization failed professionalId={} outboxId={}",
                    message.professionalId(), message.id());
        } catch (Exception exception) {
            state.markFailed(message.id(), message.attempts(), exception);
            log.warn("Google Calendar sync failed professionalId={} outboxId={} error={}",
                    message.professionalId(), message.id(), exception.toString());
            log.debug("Google Calendar sync stacktrace outboxId={}", message.id(), exception);
        }
    }
}
