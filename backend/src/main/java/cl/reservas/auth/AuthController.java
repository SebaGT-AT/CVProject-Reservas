package cl.reservas.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    static final String REFRESH_COOKIE = "reservas_refresh";
    private final AuthService authService;
    private final Duration refreshLifetime;
    private final boolean secureCookie;

    public AuthController(AuthService authService,
                          @Value("${app.security.refresh.expiration-days:30}") long refreshDays,
                          @Value("${app.security.refresh.secure-cookie:false}") boolean secureCookie) {
        this.authService = authService;
        this.refreshLifetime = Duration.ofDays(refreshDays);
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        return authService.register(request, ClientRequestInfo.from(http));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        AuthTokens tokens = authService.login(request, ClientRequestInfo.from(http));
        return withRefreshCookie(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest http) {
        return withRefreshCookie(authService.refresh(refreshToken, ClientRequestInfo.from(http)));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest http) {
        authService.logout(refreshToken, ClientRequestInfo.from(http));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie().toString()).build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(Authentication authentication, HttpServletRequest http) {
        authService.logoutAll(authentication.getName(), ClientRequestInfo.from(http));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie().toString()).build();
    }

    @GetMapping("/verify-email")
    public MessageResponse verifyEmail(@RequestParam String token, HttpServletRequest http) {
        return authService.verifyEmail(token, ClientRequestInfo.from(http));
    }

    @PostMapping("/verify-email/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request,
                                              HttpServletRequest http) {
        return authService.resendVerification(request, ClientRequestInfo.from(http));
    }

    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        return authService.forgotPassword(request, ClientRequestInfo.from(http));
    }

    @PostMapping("/password/reset")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        return authService.resetPassword(request, ClientRequestInfo.from(http));
    }

    @GetMapping("/me")
    public AuthResponse.UserSummary me(Authentication authentication) {
        return authService.currentUser(authentication.getName());
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthTokens tokens) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString())
                .body(tokens.response());
    }

    private ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true).secure(secureCookie).sameSite("Strict")
                .path("/api/v1/auth").maxAge(refreshLifetime).build();
    }

    private ResponseCookie clearCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true).secure(secureCookie).sameSite("Strict")
                .path("/api/v1/auth").maxAge(Duration.ZERO).build();
    }
}
