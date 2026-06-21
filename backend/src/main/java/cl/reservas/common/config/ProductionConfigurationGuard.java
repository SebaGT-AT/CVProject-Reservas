package cl.reservas.common.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Component
@Profile("prod")
public class ProductionConfigurationGuard implements InitializingBean {
    private final String jwtSecret;
    private final boolean secureCookies;
    private final List<String> allowedOrigins;

    public ProductionConfigurationGuard(
            @Value("${app.security.jwt.secret}") String jwtSecret,
            @Value("${app.security.refresh.secure-cookie}") boolean secureCookies,
            @Value("${app.security.allowed-origins}") List<String> allowedOrigins) {
        this.jwtSecret = jwtSecret;
        this.secureCookies = secureCookies;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void afterPropertiesSet() {
        if (jwtSecret.length() < 32 || jwtSecret.startsWith("change-this")) {
            throw new IllegalStateException("JWT_SECRET debe ser aleatorio y tener al menos 32 caracteres");
        }
        if (!secureCookies) {
            throw new IllegalStateException("SECURE_COOKIES debe ser true en produccion");
        }
        if (allowedOrigins.isEmpty() || allowedOrigins.stream().anyMatch(this::isUnsafeOrigin)) {
            throw new IllegalStateException("ALLOWED_ORIGINS debe contener solo origenes HTTPS de produccion");
        }
    }

    private boolean isUnsafeOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            return !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
        } catch (IllegalArgumentException exception) {
            return true;
        }
    }
}
