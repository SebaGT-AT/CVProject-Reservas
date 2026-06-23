package cl.reservas.integration.googlecalendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleCalendarProperties {
    private final boolean enabled;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String frontendResultUrl;
    private final String tokenEncryptionKey;

    public GoogleCalendarProperties(
            @Value("${app.integrations.google-calendar.enabled:false}") boolean enabled,
            @Value("${app.integrations.google-calendar.client-id:}") String clientId,
            @Value("${app.integrations.google-calendar.client-secret:}") String clientSecret,
            @Value("${app.integrations.google-calendar.redirect-uri:}") String redirectUri,
            @Value("${app.integrations.google-calendar.frontend-result-url:}") String frontendResultUrl,
            @Value("${app.integrations.google-calendar.token-encryption-key:}") String tokenEncryptionKey) {
        this.enabled = enabled;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.frontendResultUrl = frontendResultUrl;
        this.tokenEncryptionKey = tokenEncryptionKey;
    }

    public boolean enabled() { return enabled; }
    public String clientId() { return clientId; }
    public String clientSecret() { return clientSecret; }
    public String redirectUri() { return redirectUri; }
    public String frontendResultUrl() { return frontendResultUrl; }
    public String tokenEncryptionKey() { return tokenEncryptionKey; }
}
