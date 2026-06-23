package cl.reservas.integration.googlecalendar;

public class GoogleAuthorizationException extends RuntimeException {
    public GoogleAuthorizationException(String message) { super(message); }
    public GoogleAuthorizationException(String message, Throwable cause) { super(message, cause); }
}
