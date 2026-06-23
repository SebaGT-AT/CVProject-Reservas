package cl.reservas.integration.googlecalendar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleCalendarProductionGuardTest {
    @Test
    void acceptsDisabledIntegration() {
        var guard = new GoogleCalendarProductionGuard(properties(false, "", "", "", "", ""));
        assertThatCode(guard::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void rejectsLocalKeyWhenEnabledInProduction() {
        var guard = new GoogleCalendarProductionGuard(properties(true, "client", "secret",
                "https://api.example.com/callback", "https://app.example.com/profile",
                "bG9jYWwtZGV2LWdvb2dsZS1jYWxlbmRhci1rZXktMDE="));
        assertThatThrownBy(guard::afterPropertiesSet).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clave local");
    }

    private GoogleCalendarProperties properties(boolean enabled, String clientId, String clientSecret,
                                                String redirect, String frontend, String key) {
        return new GoogleCalendarProperties(enabled, clientId, clientSecret, redirect, frontend, key);
    }
}
