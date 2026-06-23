package cl.reservas.integration.googlecalendar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoogleCalendarConnectionRepository extends JpaRepository<GoogleCalendarConnection, UUID> {
    Optional<GoogleCalendarConnection> findByProfessional_User_EmailIgnoreCase(String email);
}
