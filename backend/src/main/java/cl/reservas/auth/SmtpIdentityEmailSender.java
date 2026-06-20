package cl.reservas.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.delivery", havingValue = "smtp")
public class SmtpIdentityEmailSender implements IdentityEmailSender {
    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String from;

    public SmtpIdentityEmailSender(JavaMailSender mailSender,
                                   @Value("${app.frontend-url}") String frontendUrl,
                                   @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.from = from;
    }

    @Override
    public void sendVerification(String email, String name, String token) {
        send(email, "Verifica tu cuenta en Reservas",
                "Hola " + name + ",\n\nVerifica tu correo aquí:\n" + frontendUrl + "/verificar-correo?token=" + token);
    }

    @Override
    public void sendPasswordReset(String email, String name, String token) {
        send(email, "Restablece tu contraseña de Reservas",
                "Hola " + name + ",\n\nCrea una nueva contraseña aquí:\n" + frontendUrl + "/restablecer-contrasena?token=" + token
                        + "\n\nSi no solicitaste el cambio, ignora este correo.");
    }

    private void send(String recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}

