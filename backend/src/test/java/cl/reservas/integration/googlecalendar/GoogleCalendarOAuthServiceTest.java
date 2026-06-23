package cl.reservas.integration.googlecalendar;

import cl.reservas.professional.ProfessionalProfile;
import cl.reservas.professional.ProfessionalProfileRepository;
import cl.reservas.user.Role;
import cl.reservas.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarOAuthServiceTest {
    @Mock ProfessionalProfileRepository professionals;
    @Mock GoogleOAuthStateService states;
    @Mock GoogleCalendarConnectionRepository connections;
    @Mock GoogleCalendarGateway gateway;
    @Mock SecretCipher cipher;
    @Mock AppointmentCalendarPublisher publisher;
    private GoogleCalendarOAuthService service;
    private ProfessionalProfile professional;

    @BeforeEach
    void setUp() {
        var properties = new GoogleCalendarProperties(true, "client-id", "client-secret",
                "https://api.example.com/api/v1/integrations/google-calendar/callback",
                "https://app.example.com/perfil-profesional", "unused");
        service = new GoogleCalendarOAuthService(properties, professionals, states, connections,
                gateway, cipher, publisher, Clock.fixed(Instant.parse("2026-06-23T12:00:00Z"), ZoneOffset.UTC));
        professional = new ProfessionalProfile(new User("Ada", "ada@example.com", "hash", Role.PROFESSIONAL),
                "ada", null, null, "America/Santiago", true, Set.of());
    }

    @Test
    void createsAuthorizationUrlWithOfflineConsentAndSingleUseState() {
        when(professionals.findByUserEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(professional));
        when(states.create(professional)).thenReturn("safe-state");

        String url = service.authorizationUrl("ada@example.com").authorizationUrl();

        assertThat(url).contains("access_type=offline", "prompt=consent", "state=safe-state")
                .contains("calendar.events");
    }

    @Test
    void exchangesCodeStoresOnlyEncryptedRefreshTokenAndBackfillsAppointments() {
        UUID professionalId = professional.getId();
        when(states.consume("safe-state")).thenReturn(professionalId);
        when(gateway.exchangeAuthorizationCode("authorization-code"))
                .thenReturn(new GoogleTokenResponse("access", "refresh", 3600, "Bearer"));
        when(cipher.encrypt("refresh")).thenReturn("encrypted-refresh");
        when(professionals.findById(professionalId)).thenReturn(Optional.of(professional));
        when(connections.findById(professionalId)).thenReturn(Optional.empty());

        service.complete("authorization-code", "safe-state", null);

        verify(connections).save(argThat(connection ->
                connection.getEncryptedRefreshToken().equals("encrypted-refresh")));
        verify(publisher).enqueueFutureAppointments(professionalId);
        verify(cipher, never()).encrypt("access");
    }
}
