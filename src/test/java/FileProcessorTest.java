import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class FileProcessorTest {
    @Test
    void existingVaultRequiresItsCurrentPassword(@TempDir Path folder) throws Exception {
        Path vault = folder.resolve("test.vault");
        String encrypted = CryptProcessor.encryptMessage("data", "old-password");
        Files.writeString(vault, "MARTA_PASS_V2\n" + encrypted + "\n", StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> FileProcessor.verifyExistingPassword(vault, "old-password".toCharArray()));
        assertThrows(SecurityException.class,
                () -> FileProcessor.verifyExistingPassword(vault, "different-password".toCharArray()));
    }

    @Test
    void vaultSerializationRoundTripPreservesData() {
        ArrayList<String> categories = new ArrayList<>();
        categories.add("Default Category");
        categories.add("Work;Private");

        ArrayList<PwdModel> entries = new ArrayList<>();
        entries.add(new PwdModel(
                "GitHub \"Main\"",
                "user@example.com",
                "p;a;s;s\\word\nline2",
                "Work;Private",
                "https://github.com/żółć"
        ));

        PwdTableModel original = new PwdTableModel(entries, categories);
        String json = FileProcessor.serializeVault(original);
        PwdTableModel restored = FileProcessor.deserializeVault(json);

        assertEquals(original.getCategories(), restored.getCategories());
        assertEquals(1, restored.getRowCount());
        assertEquals("GitHub \"Main\"", restored.getEntry(0).getName());
        assertEquals("p;a;s;s\\word\nline2", restored.getEntry(0).getPassword());
        assertEquals("https://github.com/żółć", restored.getEntry(0).getResource());
    }

    @Test
    void serializedJsonDoesNotUseSemicolonAsFieldDelimiter() {
        ArrayList<PwdModel> entries = new ArrayList<>();
        entries.add(new PwdModel("a;b", "c;d", "e;f", "Default Category", "g;h"));
        PwdTableModel model = new PwdTableModel(entries, new ArrayList<>());
        String json = FileProcessor.serializeVault(model);
        assertTrue(json.startsWith("{"));
        assertDoesNotThrow(() -> FileProcessor.deserializeVault(json));
    }

    @Test
    void encryptedSerializedVaultDoesNotExposePlaintext() {
        ArrayList<PwdModel> entries = new ArrayList<>();
        entries.add(new PwdModel("GitHub", "visible-login", "super-secret", "Default Category", "github.com"));
        String json = FileProcessor.serializeVault(new PwdTableModel(entries, new ArrayList<>()));
        String encrypted = CryptProcessor.encryptMessage(json, "master-password");
        assertNotEquals(json, encrypted);
        assertFalse(encrypted.startsWith("{"));
    }

    @Test
    void invalidDataVersionIsRejected() {
        String json = "{\"version\":999,\"categories\":[],\"entries\":[]}";
        assertThrows(IllegalArgumentException.class, () -> FileProcessor.deserializeVault(json));
    }

    @Test
    void malformedJsonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> FileProcessor.deserializeVault("{not-json}"));
    }
}
