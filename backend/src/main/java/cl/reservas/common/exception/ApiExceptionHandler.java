package cl.reservas.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Conflicto", exception.getMessage(), "conflict");
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail badCredentials() {
        return problem(HttpStatus.UNAUTHORIZED, "No autorizado", "Correo o contraseña incorrectos", "invalid-credentials");
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    ProblemDetail emailNotVerified(EmailNotVerifiedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Correo no verificado", exception.getMessage(), "email-not-verified");
    }

    @ExceptionHandler(InvalidTokenException.class)
    ProblemDetail invalidToken(InvalidTokenException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Token invalido", exception.getMessage(), "invalid-token");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitud invalida", exception.getMessage(), "bad-request");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Error de validacion",
                "Hay campos que requieren correccion", "validation-error");
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Valor invalido" : error.getDefaultMessage(),
                        (first, ignored) -> first));
        detail.setProperty("errors", errors);
        return detail;
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://reservas.local/problems/" + type));
        return problem;
    }
}
