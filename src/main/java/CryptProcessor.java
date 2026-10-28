import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Authenticated vault encryption based on PBKDF2-HMAC-SHA256 + AES-256-GCM.
 *
 * The public method names are intentionally kept the same as in the university
 * version so the class remains familiar, while the insecure substitution
 * cipher has been fully replaced.
 */
public class CryptProcessor {
    private static final byte FORMAT_VERSION = 1;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CryptProcessor() {
    }

    public static String encryptMessage(String message, String keyword) {
        char[] password = keyword == null ? new char[0] : keyword.toCharArray();
        try {
            return encryptMessage(message, password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public static String decryptMessage(String ciphertext, String keyword) {
        char[] password = keyword == null ? new char[0] : keyword.toCharArray();
        try {
            return decryptMessage(ciphertext, password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public static String encryptMessage(String message, char[] password) {
        validatePassword(password);
        byte[] salt = randomBytes(SALT_LENGTH);
        byte[] iv = randomBytes(IV_LENGTH);
        byte[] keyBytes = null;

        try {
            keyBytes = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(1 + SALT_LENGTH + IV_LENGTH + encrypted.length);
            buffer.put(FORMAT_VERSION);
            buffer.put(salt);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to encrypt vault.", e);
        } finally {
            if (keyBytes != null) Arrays.fill(keyBytes, (byte) 0);
        }
    }

    public static String decryptMessage(String ciphertext, char[] password) {
        validatePassword(password);
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(ciphertext);
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Vault data is not valid Base64.", e);
        }

        if (decoded.length < 1 + SALT_LENGTH + IV_LENGTH + 16) {
            throw new SecurityException("Vault data is incomplete or corrupted.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(decoded);
        byte version = buffer.get();
        if (version != FORMAT_VERSION) {
            throw new SecurityException("Unsupported encrypted vault version: " + version);
        }

        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        byte[] encrypted = new byte[buffer.remaining() - SALT_LENGTH - IV_LENGTH];
        buffer.get(salt);
        buffer.get(iv);
        buffer.get(encrypted);

        byte[] keyBytes = null;
        try {
            keyBytes = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new SecurityException("Incorrect master password or corrupted vault.", e);
        } catch (GeneralSecurityException e) {
            throw new SecurityException("Unable to decrypt vault.", e);
        } finally {
            if (keyBytes != null) Arrays.fill(keyBytes, (byte) 0);
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static void validatePassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Master password cannot be empty.");
        }
    }
}
