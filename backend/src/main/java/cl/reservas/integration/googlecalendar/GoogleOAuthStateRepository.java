package cl.reservas.integration.googlecalendar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoogleOAuthStateRepository extends JpaRepository<GoogleOAuthState, UUID> {
    Optional<GoogleOAuthState> findByTokenHash(String tokenHash);
}
