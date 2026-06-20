package cl.reservas.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, UUID> {
    Optional<OneTimeToken> findByTokenHashAndType(String tokenHash, OneTimeTokenType type);
    void deleteAllByUserIdAndType(UUID userId, OneTimeTokenType type);
}

