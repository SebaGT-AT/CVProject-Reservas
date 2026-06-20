package cl.reservas.auth;

import cl.reservas.common.exception.ConflictException;
import cl.reservas.common.exception.EmailNotVerifiedException;
import cl.reservas.common.exception.InvalidTokenException;
import cl.reservas.security.JwtService;
import cl.reservas.user.Role;
import cl.reservas.user.User;
import cl.reservas.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final RefreshSessionRepository sessions;
    private final OneTimeTokenRepository oneTimeTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SecureTokenService secureTokens;
    private final AuthAuditService audit;
    private final ApplicationEventPublisher events;
    private final Duration refreshLifetime;
    private final Duration verificationLifetime;
    private final Duration passwordResetLifetime;

    public AuthService(UserRepository users, RefreshSessionRepository sessions,
                       OneTimeTokenRepository oneTimeTokens, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       SecureTokenService secureTokens, AuthAuditService audit,
                       ApplicationEventPublisher events,
                       @Value("${app.security.refresh.expiration-days:30}") long refreshDays,
                       @Value("${app.security.one-time-token.verification-hours:24}") long verificationHours,
                       @Value("${app.security.one-time-token.password-reset-minutes:30}") long resetMinutes) {
        this.users = users;
        this.sessions = sessions;
        this.oneTimeTokens = oneTimeTokens;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.secureTokens = secureTokens;
        this.audit = audit;
        this.events = events;
        this.refreshLifetime = Duration.ofDays(refreshDays);
        this.verificationLifetime = Duration.ofHours(verificationHours);
        this.passwordResetLifetime = Duration.ofMinutes(resetMinutes);
    }

    @Transactional
    public MessageResponse register(RegisterRequest request, ClientRequestInfo client) {
        if (request.role() == Role.ADMIN) {
            throw new IllegalArgumentException("El rol ADMIN no admite registro publico");
        }
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ya existe una cuenta para este correo");
        }
        User user = users.save(new User(
                request.name().trim(), email, passwordEncoder.encode(request.password()), request.role()));
        issueOneTimeToken(user, OneTimeTokenType.EMAIL_VERIFICATION, verificationLifetime);
        audit.record(null, email, "REGISTERED", client);
        return new MessageResponse("Cuenta creada. Revisa tu correo para verificarla.");
    }

    @Transactional
    public AuthTokens login(LoginRequest request, ClientRequestInfo client) {
        String email = normalizeEmail(request.email());
        User user = users.findByEmailIgnoreCase(email).orElse(null);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException exception) {
            audit.record(user, email, "LOGIN_FAILED", client);
            throw exception;
        }
        if (user == null) throw new IllegalStateException("Authenticated user was not found");
        if (!user.isEmailVerified()) {
            audit.record(user, email, "LOGIN_BLOCKED_UNVERIFIED", client);
            throw new EmailNotVerifiedException();
        }
        audit.record(user, email, "LOGIN_SUCCEEDED", client);
        return createSession(user, client);
    }

    @Transactional
    public AuthTokens refresh(String rawToken, ClientRequestInfo client) {
        if (rawToken == null || rawToken.isBlank()) throw new InvalidTokenException("La sesion no es valida");
        RefreshSession current = sessions.findByTokenHash(secureTokens.hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("La sesion no es valida"));
        User user = current.getUser();
        if (!current.isActive()) {
            sessions.revokeAllByUserId(user.getId(), Instant.now());
            audit.record(user, user.getEmail(), "REFRESH_REUSE_OR_EXPIRED", client);
            throw new InvalidTokenException("La sesion expiro o fue revocada");
        }
        current.revoke();
        audit.record(user, user.getEmail(), "TOKEN_REFRESHED", client);
        return createSession(user, client);
    }

    @Transactional
    public void logout(String rawToken, ClientRequestInfo client) {
        if (rawToken == null || rawToken.isBlank()) return;
        sessions.findByTokenHash(secureTokens.hash(rawToken)).ifPresent(session -> {
            session.revoke();
            audit.record(session.getUser(), session.getUser().getEmail(), "LOGOUT", client);
        });
    }

    @Transactional
    public void logoutAll(String email, ClientRequestInfo client) {
        User user = requireUser(email);
        sessions.revokeAllByUserId(user.getId(), Instant.now());
        audit.record(user, user.getEmail(), "LOGOUT_ALL", client);
    }

    @Transactional
    public MessageResponse verifyEmail(String rawToken, ClientRequestInfo client) {
        OneTimeToken token = requireOneTimeToken(rawToken, OneTimeTokenType.EMAIL_VERIFICATION);
        token.getUser().verifyEmail();
        token.use();
        audit.record(token.getUser(), token.getUser().getEmail(), "EMAIL_VERIFIED", client);
        return new MessageResponse("Correo verificado. Ya puedes ingresar.");
    }

    @Transactional
    public MessageResponse resendVerification(ResendVerificationRequest request, ClientRequestInfo client) {
        String email = normalizeEmail(request.email());
        users.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> {
                    issueOneTimeToken(user, OneTimeTokenType.EMAIL_VERIFICATION, verificationLifetime);
                    audit.record(user, email, "EMAIL_VERIFICATION_RESENT", client);
                });
        return new MessageResponse("Si la cuenta requiere verificacion, enviaremos un nuevo enlace.");
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request, ClientRequestInfo client) {
        String email = normalizeEmail(request.email());
        users.findByEmailIgnoreCase(email).filter(User::isActive).ifPresent(user -> {
            issueOneTimeToken(user, OneTimeTokenType.PASSWORD_RESET, passwordResetLifetime);
            audit.record(user, email, "PASSWORD_RESET_REQUESTED", client);
        });
        return new MessageResponse("Si la cuenta existe, enviaremos instrucciones a su correo.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request, ClientRequestInfo client) {
        OneTimeToken token = requireOneTimeToken(request.token(), OneTimeTokenType.PASSWORD_RESET);
        User user = token.getUser();
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        token.use();
        sessions.revokeAllByUserId(user.getId(), Instant.now());
        audit.record(user, user.getEmail(), "PASSWORD_RESET_COMPLETED", client);
        return new MessageResponse("Contraseña actualizada. Ya puedes ingresar.");
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserSummary currentUser(String email) {
        return summary(requireUser(email));
    }

    private AuthTokens createSession(User user, ClientRequestInfo client) {
        String refreshToken = secureTokens.generate();
        sessions.save(new RefreshSession(user, secureTokens.hash(refreshToken),
                Instant.now().plus(refreshLifetime), client));
        AuthResponse response = new AuthResponse(jwtService.generate(user), jwtService.getExpirationSeconds(), summary(user));
        return new AuthTokens(response, refreshToken);
    }

    private void issueOneTimeToken(User user, OneTimeTokenType type, Duration lifetime) {
        oneTimeTokens.deleteAllByUserIdAndType(user.getId(), type);
        String rawToken = secureTokens.generate();
        oneTimeTokens.save(new OneTimeToken(user, secureTokens.hash(rawToken), type, Instant.now().plus(lifetime)));
        events.publishEvent(new IdentityEmailEvent(user.getEmail(), user.getName(), rawToken, type));
    }

    private OneTimeToken requireOneTimeToken(String rawToken, OneTimeTokenType type) {
        if (rawToken == null || rawToken.isBlank()) throw new InvalidTokenException("El enlace no es valido");
        OneTimeToken token = oneTimeTokens.findByTokenHashAndType(secureTokens.hash(rawToken), type)
                .orElseThrow(() -> new InvalidTokenException("El enlace no es valido"));
        if (!token.isUsable()) throw new InvalidTokenException("El enlace expiro o ya fue utilizado");
        return token;
    }

    private User requireUser(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidTokenException("El usuario ya no existe"));
    }

    private AuthResponse.UserSummary summary(User user) {
        return new AuthResponse.UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
