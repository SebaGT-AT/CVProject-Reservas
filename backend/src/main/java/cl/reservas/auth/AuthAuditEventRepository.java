package cl.reservas.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuthAuditEventRepository extends JpaRepository<AuthAuditEvent, UUID> {}

