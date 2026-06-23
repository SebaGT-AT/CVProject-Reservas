package cl.reservas.integration.googlecalendar;

import cl.reservas.common.exception.ConflictException;
import cl.reservas.common.exception.NotFoundException;
import cl.reservas.professional.ProfessionalProfile;
import cl.reservas.professional.ProfessionalProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.util.UUID;

@Service
public class GoogleCalendarOAuthService {
    private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String CALENDAR_EVENTS_SCOPE = "https://www.googleapis.com/auth/calendar.events";
    private final GoogleCalendarProperties properties;
    private final ProfessionalProfileRepository professionals;
    private final GoogleOAuthStateService stateService;
    private final GoogleCalendarConnectionRepository connections;
    private final GoogleCalendarGateway gateway;
    private final SecretCipher cipher;
    private final AppointmentCalendarPublisher calendarPublisher;
    private final Clock clock;

    public GoogleCalendarOAuthService(GoogleCalendarProperties properties,
                                      ProfessionalProfileRepository professionals,
                                      GoogleOAuthStateService stateService,
                                      GoogleCalendarConnectionRepository connections,
                                      GoogleCalendarGateway gateway, SecretCipher cipher,
                                      AppointmentCalendarPublisher calendarPublisher, Clock clock) {
        this.properties = properties;
        this.professionals = professionals;
        this.stateService = stateService;
        this.connections = connections;
        this.gateway = gateway;
        this.cipher = cipher;
        this.calendarPublisher = calendarPublisher;
        this.clock = clock;
    }

    public GoogleCalendarStatusResponse status(String email) {
        var connection = connections.findByProfessional_User_EmailIgnoreCase(email);
        return connection.map(item -> new GoogleCalendarStatusResponse(properties.enabled(),
                        item.getStatus() == GoogleConnectionStatus.CONNECTED,
                        item.getStatus() == GoogleConnectionStatus.REAUTH_REQUIRED, item.getConnectedAt()))
                .orElseGet(() -> new GoogleCalendarStatusResponse(properties.enabled(), false, false, null));
    }

    public AuthorizationUrlResponse authorizationUrl(String email) {
        requireConfigured();
        ProfessionalProfile professional = professionals.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Perfil profesional no encontrado"));
        String state = stateService.create(professional);
        String url = UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", CALENDAR_EVENTS_SCOPE)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "true")
                .queryParam("state", state)
                .build().encode().toUriString();
        return new AuthorizationUrlResponse(url);
    }

    public void complete(String code, String state, String error) {
        requireConfigured();
        UUID professionalId = stateService.consume(state);
        if (error != null || code == null || code.isBlank()) {
            throw new GoogleAuthorizationException("La autorizacion fue cancelada o rechazada");
        }
        GoogleTokenResponse tokens = gateway.exchangeAuthorizationCode(code);
        String encryptedRefreshToken = cipher.encrypt(tokens.refreshToken());
        ProfessionalProfile professional = professionals.findById(professionalId)
                .orElseThrow(() -> new NotFoundException("Perfil profesional no encontrado"));
        GoogleCalendarConnection connection = connections.findById(professionalId)
                .orElseGet(() -> new GoogleCalendarConnection(professional, encryptedRefreshToken, clock.instant()));
        connection.reconnect(encryptedRefreshToken, clock.instant());
        connections.save(connection);
        calendarPublisher.enqueueFutureAppointments(professionalId);
    }

    public void disconnect(String email) {
        GoogleCalendarConnection connection = connections.findByProfessional_User_EmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Google Calendar no esta conectado"));
        gateway.revoke(cipher.decrypt(connection.getEncryptedRefreshToken()));
        connections.delete(connection);
    }

    public String resultUrl(boolean success) {
        return UriComponentsBuilder.fromUriString(properties.frontendResultUrl())
                .queryParam("googleCalendar", success ? "connected" : "error")
                .build().encode().toUriString();
    }

    private void requireConfigured() {
        if (!properties.enabled() || properties.clientId().isBlank() || properties.clientSecret().isBlank()) {
            throw new ConflictException("La integracion con Google Calendar no esta configurada");
        }
    }
}
