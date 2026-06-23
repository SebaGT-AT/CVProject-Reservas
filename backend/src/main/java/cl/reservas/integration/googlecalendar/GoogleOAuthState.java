package cl.reservas.integration.googlecalendar;

import cl.reservas.professional.ProfessionalProfile;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "google_oauth_states")
public class GoogleOAuthState {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "professional_id")
    private ProfessionalProfile professional;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_at") private Instant consumedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected GoogleOAuthState() {}

    public GoogleOAuthState(ProfessionalProfile professional, String tokenHash, Instant now) {
        this.id = UUID.randomUUID();
        this.professional = professional;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.expiresAt = now.plusSeconds(600);
    }

    public boolean isUsableAt(Instant now) { return consumedAt == null && expiresAt.isAfter(now); }
    public void consume(Instant now) { this.consumedAt = now; }
    public ProfessionalProfile getProfessional() { return professional; }
}
