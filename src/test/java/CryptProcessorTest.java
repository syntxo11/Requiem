import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CryptProcessorTest {
    @Test
    void encryptAndDecryptRoundTrip() {
        String encrypted = CryptProcessor.encryptMessage("github;user@example.com;P@ssw0rd!", "master-password");
        assertEquals("github;user@example.com;P@ssw0rd!",
                CryptProcessor.decryptMessage(encrypted, "master-password"));
    }

    @Test
    void unicodeRoundTrip() {
        String text = "Пароль 🔐 — Zażółć gęślą jaźń";
        assertEquals(text, CryptProcessor.decryptMessage(
                CryptProcessor.encryptMessage(text, "довгий пароль"), "довгий пароль"));
    }

    @Test
    void samePlaintextProducesDifferentCiphertext() {
        String first = CryptProcessor.encryptMessage("secret", "master");
        String second = CryptProcessor.encryptMessage("secret", "master");
        assertNotEquals(first, second);
    }

    @Test
    void wrongPasswordIsRejected() {
        String encrypted = CryptProcessor.encryptMessage("secret", "correct");
        assertThrows(SecurityException.class,
                () -> CryptProcessor.decryptMessage(encrypted, "wrong"));
    }

    @Test
    void tamperedCiphertextIsRejected() {
        String encrypted = CryptProcessor.encryptMessage("secret", "master");
        byte[] bytes = Base64.getDecoder().decode(encrypted);
        bytes[bytes.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(bytes);
        assertThrows(SecurityException.class,
                () -> CryptProcessor.decryptMessage(tampered, "master"));
    }

    @Test
    void emptyPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> CryptProcessor.encryptMessage("secret", ""));
    }
}
