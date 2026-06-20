package cl.reservas.auth;

public interface IdentityEmailSender {
    void sendVerification(String email, String name, String token);
    void sendPasswordReset(String email, String name, String token);
}

