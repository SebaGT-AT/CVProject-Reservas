package cl.reservas.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    protected User() {}

    public User(String name, String email, String passwordHash, Role role) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public boolean isEmailVerified() { return emailVerifiedAt != null; }

    public void verifyEmail() {
        if (emailVerifiedAt == null) emailVerifiedAt = Instant.now();
    }

    public void changePassword(String encodedPassword) {
        this.passwordHash = encodedPassword;
        this.passwordChangedAt = Instant.now();
    }
}
