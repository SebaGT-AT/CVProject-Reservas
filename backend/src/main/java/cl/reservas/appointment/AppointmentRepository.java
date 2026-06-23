package cl.reservas.appointment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    @EntityGraph(attributePaths = {"professional", "professional.user", "customer"})
    Optional<Appointment> findByCustomerIdAndIdempotencyKey(UUID customerId, UUID idempotencyKey);

    @EntityGraph(attributePaths = {"professional", "professional.user", "customer"})
    List<Appointment> findAllByCustomerIdOrderByStartAtDesc(UUID customerId);

    @EntityGraph(attributePaths = {"professional", "professional.user", "customer"})
    List<Appointment> findAllByProfessionalIdAndStartAtBetweenOrderByStartAtAsc(
            UUID professionalId, Instant from, Instant to);

    Optional<Appointment> findByIdAndCustomerId(UUID id, UUID customerId);
    Optional<Appointment> findByIdAndProfessionalId(UUID id, UUID professionalId);

    @Query("""
            select a from Appointment a
            where a.professional.id = :professionalId
              and a.status in :statuses
              and a.startAt < :rangeEnd and a.busyUntil > :rangeStart
            order by a.startAt
            """)
    List<Appointment> findActiveOverlappingRange(@Param("professionalId") UUID professionalId,
                                                  @Param("rangeStart") Instant rangeStart,
                                                  @Param("rangeEnd") Instant rangeEnd,
                                                  @Param("statuses") Collection<AppointmentStatus> statuses);

    List<Appointment> findAllByStatusAndStartAtBetweenOrderByStartAt(
            AppointmentStatus status, Instant from, Instant to);

    @EntityGraph(attributePaths = {"professional", "professional.user", "customer", "service"})
    List<Appointment> findAllByProfessionalIdAndStatusAndStartAtGreaterThanEqualOrderByStartAt(
            UUID professionalId, AppointmentStatus status, Instant from);

    long countByProfessionalIdAndStatusInAndStartAtGreaterThanEqualAndStartAtLessThan(
            UUID professionalId, Collection<AppointmentStatus> statuses, Instant from, Instant to);

    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT customer_id
                FROM appointments
                WHERE professional_id = :professionalId AND status <> 'CANCELLED'
                GROUP BY customer_id
                HAVING MIN(start_at) >= :from AND MIN(start_at) < :to
            ) first_appointments
            """, nativeQuery = true)
    long countNewCustomers(@Param("professionalId") UUID professionalId,
                           @Param("from") Instant from, @Param("to") Instant to);

    @EntityGraph(attributePaths = {"professional", "professional.user", "customer", "service"})
    List<Appointment> findTop5ByProfessionalIdAndStatusInAndStartAtGreaterThanEqualOrderByStartAtAsc(
            UUID professionalId, Collection<AppointmentStatus> statuses, Instant from);
}
