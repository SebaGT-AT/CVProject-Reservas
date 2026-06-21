package cl.reservas.professional;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {
    List<ServiceOffering> findAllByProfessionalIdOrderByCreatedAtDesc(UUID professionalId);
    List<ServiceOffering> findAllByProfessionalIdAndActiveTrueOrderByName(UUID professionalId);
    Optional<ServiceOffering> findByIdAndProfessionalId(UUID id, UUID professionalId);
    Optional<ServiceOffering> findByIdAndProfessionalIdAndActiveTrue(UUID id, UUID professionalId);
    boolean existsByProfessionalIdAndActiveTrue(UUID professionalId);
}
