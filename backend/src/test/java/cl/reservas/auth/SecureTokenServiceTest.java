package cl.reservas.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecureTokenServiceTest {
    private final SecureTokenService tokens = new SecureTokenService();

    @Test
    void generatesUnpredictableUrlSafeTokensAndStableHashes() {
        String first = tokens.generate();
        String second = tokens.generate();

        assertThat(first).hasSizeGreaterThanOrEqualTo(43).doesNotContain("=", "+", "/");
        assertThat(second).isNotEqualTo(first);
        assertThat(tokens.hash(first)).hasSize(64).isEqualTo(tokens.hash(first));
        assertThat(tokens.hash(second)).isNotEqualTo(tokens.hash(first));
    }
}

