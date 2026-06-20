package cl.reservas.auth;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IdentityEmailListener {
    private final IdentityEmailSender sender;

    public IdentityEmailListener(IdentityEmailSender sender) { this.sender = sender; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(IdentityEmailEvent event) {
        if (event.type() == OneTimeTokenType.EMAIL_VERIFICATION) {
            sender.sendVerification(event.email(), event.name(), event.token());
        } else {
            sender.sendPasswordReset(event.email(), event.name(), event.token());
        }
    }
}

