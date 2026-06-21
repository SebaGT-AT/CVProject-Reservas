package cl.reservas.scheduling;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WeeklySchedulePeriodRepository extends JpaRepository<WeeklySchedulePeriod, UUID> {
    List<WeeklySchedulePeriod> findAllByProfessionalIdOrderByDayOfWeekAscStartTimeAsc(UUID professionalId);
    void deleteAllByProfessionalId(UUID professionalId);
}
