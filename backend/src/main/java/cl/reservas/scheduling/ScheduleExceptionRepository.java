package cl.reservas.scheduling;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, UUID> {
    List<ScheduleException> findAllByProfessionalIdAndDateBetweenOrderByDateAscStartTimeAsc(
            UUID professionalId, LocalDate from, LocalDate to);
    Optional<ScheduleException> findByIdAndProfessionalId(UUID id, UUID professionalId);
}
