package cl.reservas.common.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() { super("Debes verificar tu correo antes de ingresar"); }
}

