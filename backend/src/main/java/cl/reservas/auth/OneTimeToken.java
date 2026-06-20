package cl.reservas.auth;

import cl.reservas.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "one_time_tokens")
public class OneTimeToken {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private OneTimeTokenType type;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "used_at") private Instant usedAt;

    protected OneTimeToken() {}

    public OneTimeToken(User user, String tokenHash, OneTimeTokenType type, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.tokenHash = tokenHash;
        this.type = type;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public User getUser() { return user; }
    public boolean isUsable() { return usedAt == null && expiresAt.isAfter(Instant.now()); }
    public void use() { this.usedAt = Instant.now(); }
}

