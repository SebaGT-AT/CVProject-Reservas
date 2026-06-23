package cl.reservas.integration.googlecalendar;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/integrations/google-calendar")
public class GoogleCalendarController {
    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarController.class);
    private final GoogleCalendarOAuthService service;

    public GoogleCalendarController(GoogleCalendarOAuthService service) { this.service = service; }

    @GetMapping("/status")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public GoogleCalendarStatusResponse status(Authentication authentication) {
        return service.status(authentication.getName());
    }

    @GetMapping("/authorization-url")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public AuthorizationUrlResponse authorizationUrl(Authentication authentication) {
        return service.authorizationUrl(authentication.getName());
    }

    @GetMapping("/callback")
    public RedirectView callback(@RequestParam(required = false) String code,
                                 @RequestParam(required = false) String state,
                                 @RequestParam(required = false) String error) {
        boolean success = false;
        try {
            service.complete(code, state, error);
            success = true;
        } catch (RuntimeException exception) {
            log.warn("Google Calendar OAuth callback failed error={}", exception.toString());
            log.debug("Google Calendar OAuth callback stacktrace", exception);
        }
        return new RedirectView(service.resultUrl(success));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public void disconnect(Authentication authentication) {
        service.disconnect(authentication.getName());
    }
}
