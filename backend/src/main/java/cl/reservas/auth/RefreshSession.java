package cl.reservas.auth;

import cl.reservas.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "ip_address", length = 64) private String ipAddress;
    @Column(name = "user_agent", length = 500) private String userAgent;

    protected RefreshSession() {}

    public RefreshSession(User user, String tokenHash, Instant expiresAt, ClientRequestInfo client) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.ipAddress = client.ipAddress();
        this.userAgent = client.userAgent();
    }

    public User getUser() { return user; }
    public boolean isActive() { return revokedAt == null && expiresAt.isAfter(Instant.now()); }
    public void revoke() { if (revokedAt == null) revokedAt = Instant.now(); }
}

