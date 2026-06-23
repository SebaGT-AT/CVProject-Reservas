package cl.reservas.integration.googlecalendar;

import cl.reservas.auth.SecureTokenService;
import cl.reservas.common.exception.InvalidTokenException;
import cl.reservas.professional.ProfessionalProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
class GoogleOAuthStateService {
    private final GoogleOAuthStateRepository states;
    private final SecureTokenService tokens;
    private final Clock clock;

    GoogleOAuthStateService(GoogleOAuthStateRepository states, SecureTokenService tokens, Clock clock) {
        this.states = states;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Transactional
    String create(ProfessionalProfile professional) {
        String raw = tokens.generate();
        states.save(new GoogleOAuthState(professional, tokens.hash(raw), clock.instant()));
        return raw;
    }

    @Transactional
    UUID consume(String raw) {
        if (raw == null || raw.isBlank()) throw new InvalidTokenException("Estado OAuth invalido");
        GoogleOAuthState state = states.findByTokenHash(tokens.hash(raw))
                .orElseThrow(() -> new InvalidTokenException("Estado OAuth invalido"));
        if (!state.isUsableAt(clock.instant())) throw new InvalidTokenException("Estado OAuth expirado o utilizado");
        state.consume(clock.instant());
        return state.getProfessional().getId();
    }
}
