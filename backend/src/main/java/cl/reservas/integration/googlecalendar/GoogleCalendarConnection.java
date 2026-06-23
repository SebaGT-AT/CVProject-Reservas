package cl.reservas.integration.googlecalendar;

import cl.reservas.professional.ProfessionalProfile;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "google_calendar_connections")
public class GoogleCalendarConnection {
    @Id @Column(name = "professional_id") private UUID professionalId;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "professional_id")
    private ProfessionalProfile professional;
    @Column(name = "encrypted_refresh_token", nullable = false) private String encryptedRefreshToken;
    @Column(name = "calendar_id", nullable = false, length = 255) private String calendarId;
    @Column(name = "connection_generation", nullable = false) private UUID connectionGeneration;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private GoogleConnectionStatus status;
    @Column(name = "connected_at", nullable = false) private Instant connectedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Version private long version;

    protected GoogleCalendarConnection() {}

    public GoogleCalendarConnection(ProfessionalProfile professional, String encryptedRefreshToken, Instant now) {
        this.professional = professional;
        this.professionalId = professional.getId();
        reconnect(encryptedRefreshToken, now);
        this.calendarId = "primary";
    }

    public void reconnect(String encryptedRefreshToken, Instant now) {
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.status = GoogleConnectionStatus.CONNECTED;
        this.connectionGeneration = UUID.randomUUID();
        this.connectedAt = now;
        this.lastError = null;
        this.updatedAt = now;
        if (this.calendarId == null) this.calendarId = "primary";
    }

    public void requireReauthorization(String error, Instant now) {
        this.status = GoogleConnectionStatus.REAUTH_REQUIRED;
        this.lastError = truncate(error);
        this.updatedAt = now;
    }

    private String truncate(String value) {
        if (value == null) return "Autorizacion rechazada por Google";
        return value.substring(0, Math.min(1000, value.length()));
    }

    public UUID getProfessionalId() { return professionalId; }
    public String getEncryptedRefreshToken() { return encryptedRefreshToken; }
    public String getCalendarId() { return calendarId; }
    public UUID getConnectionGeneration() { return connectionGeneration; }
    public GoogleConnectionStatus getStatus() { return status; }
    public Instant getConnectedAt() { return connectedAt; }
    public String getLastError() { return lastError; }
}
