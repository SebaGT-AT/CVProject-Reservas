package cl.reservas.scheduling;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BookingPolicyRepository extends JpaRepository<BookingPolicy, UUID> {
    Optional<BookingPolicy> findByProfessionalId(UUID professionalId);
}
