package cl.reservas.common.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationGuardTest {
    @Test
    void acceptsHardenedProductionSettings() {
        var guard = new ProductionConfigurationGuard(
                "a-strong-production-secret-with-entropy", true, List.of("https://app.example.com"));

        assertThatCode(guard::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void rejectsLocalOrInsecureSettings() {
        var guard = new ProductionConfigurationGuard(
                "change-this-local-secret-with-at-least-32-characters", false,
                List.of("http://localhost:5173"));

        assertThatThrownBy(guard::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }
}
