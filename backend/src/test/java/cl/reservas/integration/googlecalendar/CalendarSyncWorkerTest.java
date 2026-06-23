package cl.reservas.integration.googlecalendar;

import cl.reservas.professional.ProfessionalProfile;
import cl.reservas.user.Role;
import cl.reservas.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarSyncWorkerTest {
    @Mock CalendarSyncStateService state;
    @Mock GoogleCalendarConnectionRepository connections;
    @Mock GoogleCalendarGateway gateway;
    @Mock SecretCipher cipher;

    @Test
    void refreshesAccessTokenAndUpsertsEvent() throws Exception {
        UUID professionalId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();
        CalendarSyncPayload payload = payload();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        when(state.claimBatch()).thenReturn(List.of(new CalendarSyncMessage(
                outboxId, professionalId, CalendarSyncOperation.UPSERT,
                objectMapper.writeValueAsString(payload), 0)));
        GoogleCalendarConnection connection = connection();
        when(connections.findById(professionalId)).thenReturn(Optional.of(connection));
        when(cipher.decrypt(connection.getEncryptedRefreshToken())).thenReturn("refresh");
        when(gateway.refreshAccessToken("refresh")).thenReturn("access");
        CalendarSyncWorker worker = new CalendarSyncWorker(state, connections, gateway, cipher, objectMapper);

        worker.dispatch();

        verify(gateway).upsertEvent("access", "primary", payload);
        verify(state).markSent(outboxId);
    }

    @Test
    void requestsReauthorizationWhenRefreshTokenIsRejected() throws Exception {
        UUID professionalId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        when(state.claimBatch()).thenReturn(List.of(new CalendarSyncMessage(
                outboxId, professionalId, CalendarSyncOperation.DELETE,
                objectMapper.writeValueAsString(payload()), 2)));
        GoogleCalendarConnection connection = connection();
        when(connections.findById(professionalId)).thenReturn(Optional.of(connection));
        when(cipher.decrypt(any())).thenReturn("refresh");
        when(gateway.refreshAccessToken("refresh")).thenThrow(new GoogleAuthorizationException("revocado"));
        CalendarSyncWorker worker = new CalendarSyncWorker(state, connections, gateway, cipher, objectMapper);

        worker.dispatch();

        verify(state).requireReauthorization(professionalId, "revocado");
        verify(state).markFailed(eq(outboxId), eq(2), any(GoogleAuthorizationException.class));
    }

    private GoogleCalendarConnection connection() {
        ProfessionalProfile profile = new ProfessionalProfile(
                new User("Ada", "ada@example.com", "hash", Role.PROFESSIONAL),
                "ada", null, null, "America/Santiago", true, Set.of());
        return new GoogleCalendarConnection(profile, "encrypted", Instant.parse("2026-06-23T12:00:00Z"));
    }

    private CalendarSyncPayload payload() {
        return new CalendarSyncPayload(UUID.randomUUID(), "revent12345", "Consulta", "Detalle",
                Instant.parse("2026-07-01T13:00:00Z"), Instant.parse("2026-07-01T14:00:00Z"),
                "America/Santiago");
    }
}
