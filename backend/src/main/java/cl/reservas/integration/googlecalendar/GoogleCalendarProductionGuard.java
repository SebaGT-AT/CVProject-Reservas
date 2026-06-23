package cl.reservas.integration.googlecalendar;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@Profile("prod")
public class GoogleCalendarProductionGuard implements InitializingBean {
    private static final String LOCAL_KEY = "bG9jYWwtZGV2LWdvb2dsZS1jYWxlbmRhci1rZXktMDE=";
    private final GoogleCalendarProperties properties;

    public GoogleCalendarProductionGuard(GoogleCalendarProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (!properties.enabled()) return;
        if (properties.clientId().isBlank() || properties.clientSecret().isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET son obligatorios");
        }
        if (LOCAL_KEY.equals(properties.tokenEncryptionKey())) {
            throw new IllegalStateException("GOOGLE_TOKEN_ENCRYPTION_KEY no puede usar la clave local en produccion");
        }
        requireHttps(properties.redirectUri(), "GOOGLE_REDIRECT_URI");
        requireHttps(properties.frontendResultUrl(), "GOOGLE_FRONTEND_RESULT_URL");
    }

    private void requireHttps(String value, String name) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(name + " debe ser una URL HTTPS valida", exception);
        }
    }
}
