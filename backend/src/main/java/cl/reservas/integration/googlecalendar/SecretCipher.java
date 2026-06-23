package cl.reservas.integration.googlecalendar;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecretCipher {
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final byte FORMAT_VERSION = 1;
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SecretCipher(GoogleCalendarProperties properties) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(properties.tokenEncryptionKey());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("GOOGLE_TOKEN_ENCRYPTION_KEY debe estar codificada en Base64", exception);
        }
        if (decoded.length != 32) {
            throw new IllegalStateException("GOOGLE_TOKEN_ENCRYPTION_KEY debe decodificar exactamente 32 bytes");
        }
        this.key = new SecretKeySpec(decoded, "AES");
    }

    public String encrypt(String plaintext) {
        byte[] nonce = new byte[NONCE_LENGTH];
        random.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(1 + nonce.length + encrypted.length)
                    .put(FORMAT_VERSION).put(nonce).put(encrypted).array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No fue posible cifrar la credencial", exception);
        }
    }

    public String decrypt(String encoded) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encoded));
            if (buffer.get() != FORMAT_VERSION || buffer.remaining() <= NONCE_LENGTH) {
                throw new IllegalArgumentException("Formato de credencial no soportado");
            }
            byte[] nonce = new byte[NONCE_LENGTH];
            buffer.get(nonce);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("No fue posible descifrar la credencial", exception);
        }
    }
}
