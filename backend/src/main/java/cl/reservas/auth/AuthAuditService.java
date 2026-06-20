package cl.reservas.auth;

import cl.reservas.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthAuditService {
    private final AuthAuditEventRepository events;

    public AuthAuditService(AuthAuditEventRepository events) { this.events = events; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User user, String email, String type, ClientRequestInfo client) {
        events.save(new AuthAuditEvent(user, email, type, client));
    }
}

