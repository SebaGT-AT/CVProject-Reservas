package cl.reservas.auth;

import cl.reservas.common.exception.EmailNotVerifiedException;
import cl.reservas.security.JwtService;
import cl.reservas.user.Role;
import cl.reservas.user.User;
import cl.reservas.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock RefreshSessionRepository sessions;
    @Mock OneTimeTokenRepository oneTimeTokens;
    @Mock PasswordEncoder encoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock AuthAuditService audit;
    @Mock ApplicationEventPublisher events;
    private AuthService service;
    private final ClientRequestInfo client = new ClientRequestInfo("127.0.0.1", "test");

    @BeforeEach
    void setUp() {
        service = new AuthService(users, sessions, oneTimeTokens, encoder, authenticationManager,
                jwtService, new SecureTokenService(), audit, events, 30, 24, 30);
    }

    @Test
    void registrationCreatesAnInactiveUntilVerifiedIdentityAndSendsVerification() {
        when(encoder.encode("very-secure-password")).thenReturn("encoded");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = service.register(new RegisterRequest(
                "Ada Lovelace", " ADA@EXAMPLE.COM ", "very-secure-password", Role.PROFESSIONAL), client);

        assertThat(response.message()).contains("Revisa tu correo");
        verify(oneTimeTokens).save(any(OneTimeToken.class));
        verify(events).publishEvent(any(IdentityEmailEvent.class));
        verify(audit).record(null, "ada@example.com", "REGISTERED", client);
    }

    @Test
    void publicRegistrationCannotCreateAdministrators() {
        RegisterRequest request = new RegisterRequest("Admin", "admin@example.com", "very-secure-password", Role.ADMIN);
        assertThatThrownBy(() -> service.register(request, client))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(encoder, oneTimeTokens, events);
    }

    @Test
    void loginRequiresVerifiedEmailEvenWithValidCredentials() {
        User user = new User("Ada", "ada@example.com", "encoded", Role.CUSTOMER);
        when(users.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("ada@example.com", null));

        assertThatThrownBy(() -> service.login(new LoginRequest("ada@example.com", "password"), client))
                .isInstanceOf(EmailNotVerifiedException.class);
        verify(sessions, never()).save(any());
        verify(audit).record(user, "ada@example.com", "LOGIN_BLOCKED_UNVERIFIED", client);
    }

    @Test
    void refreshTokenIsRotatedAndPreviousSessionIsRevoked() {
        User user = new User("Ada", "ada@example.com", "encoded", Role.CUSTOMER);
        user.verifyEmail();
        SecureTokenService tokens = new SecureTokenService();
        String raw = tokens.generate();
        RefreshSession current = new RefreshSession(user, tokens.hash(raw),
                java.time.Instant.now().plusSeconds(60), client);
        when(sessions.findByTokenHash(tokens.hash(raw))).thenReturn(Optional.of(current));
        when(jwtService.generate(user)).thenReturn("access.jwt");
        when(jwtService.getExpirationSeconds()).thenReturn(900L);

        AuthTokens rotated = service.refresh(raw, client);

        assertThat(current.isActive()).isFalse();
        assertThat(rotated.refreshToken()).isNotEqualTo(raw);
        assertThat(rotated.response().accessToken()).isEqualTo("access.jwt");
        verify(sessions).save(any(RefreshSession.class));
    }
}

