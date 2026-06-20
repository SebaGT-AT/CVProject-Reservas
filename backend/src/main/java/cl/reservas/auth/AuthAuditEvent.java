package cl.reservas.auth;

import cl.reservas.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_audit_events")
public class AuthAuditEvent {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @Column(length = 254) private String email;
    @Column(name = "event_type", nullable = false, length = 50) private String eventType;
    @Column(name = "ip_address", length = 64) private String ipAddress;
    @Column(name = "user_agent", length = 500) private String userAgent;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;

    protected AuthAuditEvent() {}

    public AuthAuditEvent(User user, String email, String eventType, ClientRequestInfo client) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.email = email;
        this.eventType = eventType;
        this.ipAddress = client.ipAddress();
        this.userAgent = client.userAgent();
        this.occurredAt = Instant.now();
    }
}

