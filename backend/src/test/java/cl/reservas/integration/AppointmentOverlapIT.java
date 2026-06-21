package cl.reservas.integration;

import cl.reservas.appointment.Appointment;
import cl.reservas.appointment.AppointmentRepository;
import cl.reservas.professional.ProfessionalProfile;
import cl.reservas.professional.ProfessionalProfileRepository;
import cl.reservas.professional.ServiceOffering;
import cl.reservas.professional.ServiceOfferingRepository;
import cl.reservas.user.Role;
import cl.reservas.user.User;
import cl.reservas.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Transactional
class AppointmentOverlapIT {
    @Autowired UserRepository users;
    @Autowired ProfessionalProfileRepository professionals;
    @Autowired ServiceOfferingRepository services;
    @Autowired AppointmentRepository appointments;

    @Test
    void databaseRejectsOverlappingActiveAppointmentsForSameProfessional() {
        String suffix = UUID.randomUUID().toString();
        User professionalUser = users.save(new User("Profesional CI", "pro-" + suffix + "@example.com",
                "not-used", Role.PROFESSIONAL));
        User customer = users.save(new User("Cliente CI", "client-" + suffix + "@example.com",
                "not-used", Role.CUSTOMER));
        ProfessionalProfile professional = professionals.save(new ProfessionalProfile(
                professionalUser, "pro-" + suffix, null, null, "America/Santiago", true, Set.of()));
        ServiceOffering service = services.save(new ServiceOffering(
                professional, "Consulta", null, 60, new BigDecimal("25000.00"), "CLP", true));

        Instant start = Instant.parse("2035-01-10T13:00:00Z");
        appointments.saveAndFlush(new Appointment(professional, customer, service, UUID.randomUUID(),
                start, start.plusSeconds(3600), start.plusSeconds(3600)));

        Appointment overlapping = new Appointment(professional, customer, service, UUID.randomUUID(),
                start.plusSeconds(1800), start.plusSeconds(5400), start.plusSeconds(5400));

        assertThatThrownBy(() -> appointments.saveAndFlush(overlapping))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
