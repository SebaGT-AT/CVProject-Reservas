package cl.reservas.auth;

import cl.reservas.user.Role;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        long expiresInSeconds,
        UserSummary user
) {
    public record UserSummary(UUID id, String name, String email, Role role) {}
}

