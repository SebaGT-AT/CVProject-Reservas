package cl.reservas.integration.googlecalendar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretCipherTest {
    private static final String KEY = "bG9jYWwtZGV2LWdvb2dsZS1jYWxlbmRhci1rZXktMDE=";

    @Test
    void encryptsWithRandomNonceAndAuthenticatesCiphertext() {
        SecretCipher cipher = new SecretCipher(properties(KEY));

        String first = cipher.encrypt("refresh-token");
        String second = cipher.encrypt("refresh-token");

        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("refresh-token");
        String tampered = first.substring(0, first.length() - 2) + "AA";
        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsKeysThatAreNotAes256() {
        assertThatThrownBy(() -> new SecretCipher(properties("c2hvcnQ=")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 bytes");
    }

    private GoogleCalendarProperties properties(String key) {
        return new GoogleCalendarProperties(false, "", "", "", "", key);
    }
}
