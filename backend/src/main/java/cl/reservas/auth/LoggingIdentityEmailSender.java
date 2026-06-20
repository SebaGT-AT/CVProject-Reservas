package cl.reservas.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.delivery", havingValue = "log", matchIfMissing = true)
public class LoggingIdentityEmailSender implements IdentityEmailSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingIdentityEmailSender.class);
    private final String frontendUrl;

    public LoggingIdentityEmailSender(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void sendVerification(String email, String name, String token) {
        log.info("DEV MAIL verification recipient={} name={} url={}/verificar-correo?token={}",
                email, name, frontendUrl, token);
    }

    @Override
    public void sendPasswordReset(String email, String name, String token) {
        log.info("DEV MAIL password-reset recipient={} name={} url={}/restablecer-contrasena?token={}",
                email, name, frontendUrl, token);
    }
}

