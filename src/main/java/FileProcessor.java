import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads and saves one authenticated, encrypted vault file. */
public class FileProcessor {
    private static final String FILE_HEADER = "MARTA_PASS_V2";
    private static final int DATA_VERSION = 2;

    private String activeFileName = "";
    private final KeyInputDialog pwdDialogView;
    private final OpenFileDialog openDialogView;
    private final SaveFileDialog saveDialogView;
    private String lastError = "";

    public FileProcessor(KeyInputDialog pwdDialogView, OpenFileDialog openDialogView) {
        this.pwdDialogView = pwdDialogView;
        this.openDialogView = openDialogView;
        this.saveDialogView = null;
    }

    public FileProcessor(KeyInputDialog pwdDialogView, SaveFileDialog saveDialogView) {
        this.pwdDialogView = pwdDialogView;
        this.saveDialogView = saveDialogView;
        this.openDialogView = null;
    }

    /** Constructor used when saving to an already selected active file. */
    public FileProcessor(KeyInputDialog pwdDialogView) {
        this.pwdDialogView = pwdDialogView;
        this.openDialogView = null;
        this.saveDialogView = null;
    }

    public boolean savePwdTableModel(PwdTableModel pwdItems) {
        lastError = "";
        if (saveDialogView == null) {
            lastError = "Save dialog is not configured.";
            return false;
        }

        saveDialogView.ShowDialog();
        if (!saveDialogView.getDialogResult()) return false;
        return saveToFile(pwdItems, saveDialogView.getFileName());
    }

    public boolean saveToFile(PwdTableModel pwdItems, String fileName) {
        lastError = "";
        if (pwdItems == null || fileName == null || fileName.isBlank()) {
            lastError = "No vault or file name was provided.";
            return false;
        }

        pwdDialogView.ShowDialog();
        if (!pwdDialogView.getDialogResult()) return false;

        char[] password = pwdDialogView.getKeyChars();
        try {
            Path target = Path.of(fileName);
            verifyExistingPassword(target, password);
            String json = serializeVault(pwdItems);
            String encrypted = CryptProcessor.encryptMessage(json, password);
            writeAtomically(target, FILE_HEADER + System.lineSeparator() + encrypted + System.lineSeparator());
            activeFileName = fileName;
            pwdItems.markClean();
            return true;
        } catch (RuntimeException | IOException e) {
            lastError = "Could not save vault: " + e.getMessage();
            return false;
        } finally {
            Arrays.fill(password, '\0');
            pwdDialogView.clearKey();
        }
    }

    private PwdTableModel Load() {
        lastError = "";
        if (openDialogView == null) {
            lastError = "Open dialog is not configured.";
            return null;
        }

        openDialogView.ShowDialog();
        if (!openDialogView.getDialogResult()) return null;

        String selectedFile = openDialogView.getFileName();
        pwdDialogView.ShowDialog();
        if (!pwdDialogView.getDialogResult()) return null;

        char[] password = pwdDialogView.getKeyChars();
        try {
            String encrypted = readEncryptedVault(Path.of(selectedFile));
            String json = CryptProcessor.decryptMessage(encrypted, password);
            PwdTableModel model = deserializeVault(json);
            model.markClean();
            activeFileName = selectedFile;
            return model;
        } catch (SecurityException e) {
            lastError = e.getMessage();
            return null;
        } catch (IOException | RuntimeException e) {
            lastError = "Could not open vault: " + e.getMessage();
            return null;
        } finally {
            Arrays.fill(password, '\0');
            pwdDialogView.clearKey();
        }
    }

    public PwdTableModel getPwdTableModel() {
        return Load();
    }

    public String getActiveFileName() {
        return activeFileName;
    }

    public String getLastError() {
        return lastError;
    }

    static void verifyExistingPassword(Path path, char[] password) throws IOException {
        if (Files.exists(path)) {
            CryptProcessor.decryptMessage(readEncryptedVault(path), password);
        }
    }

    private static String readEncryptedVault(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        int firstBreak = content.indexOf('\n');
        if (firstBreak < 0 || !content.substring(0, firstBreak).trim().equals(FILE_HEADER)) {
            throw new SecurityException("Unsupported vault format.");
        }
        return content.substring(firstBreak + 1).trim();
    }

    static String serializeVault(PwdTableModel model) {
        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"version\":").append(DATA_VERSION).append(',');

        json.append("\"categories\":[");
        List<String> categories = model.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            if (i > 0) json.append(',');
            appendJsonString(json, categories.get(i));
        }
        json.append("],");

        json.append("\"entries\":[");
        List<PwdModel> entries = model.getData();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) json.append(',');
            PwdModel item = entries.get(i);
            json.append('{');
            appendProperty(json, "name", item.getName());
            json.append(',');
            appendProperty(json, "login", item.getLogin());
            json.append(',');
            appendProperty(json, "password", item.getPassword());
            json.append(',');
            appendProperty(json, "category", item.getCategory());
            json.append(',');
            appendProperty(json, "resource", item.getResource());
            json.append('}');
        }
        json.append("]}");
        return json.toString();
    }

    static PwdTableModel deserializeVault(String json) {
        Object parsed = new JsonParser(json).parse();
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("Vault payload must be a JSON object.");
        }

        Object versionValue = root.get("version");
        if (!(versionValue instanceof Number) || ((Number) versionValue).intValue() != DATA_VERSION) {
            throw new IllegalArgumentException("Unsupported vault data version.");
        }

        ArrayList<String> categories = new ArrayList<>();
        Object categoriesValue = root.get("categories");
        if (categoriesValue instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String category) categories.add(category);
            }
        }

        ArrayList<PwdModel> entries = new ArrayList<>();
        Object entriesValue = root.get("entries");
        if (entriesValue instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> entry)) continue;
                entries.add(new PwdModel(
                        stringValue(entry.get("name")),
                        stringValue(entry.get("login")),
                        stringValue(entry.get("password")),
                        stringValue(entry.get("category")),
                        stringValue(entry.get("resource"))
                ));
            }
        }

        return new PwdTableModel(entries, categories);
    }

    private static String stringValue(Object value) {
        return value instanceof String ? (String) value : "";
    }

    private static void appendProperty(StringBuilder json, String name, String value) {
        appendJsonString(json, name);
        json.append(':');
        appendJsonString(json, value);
    }

    private static void appendJsonString(StringBuilder json, String value) {
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        json.append(String.format("\\u%04x", (int) ch));
                    } else {
                        json.append(ch);
                    }
                }
            }
        }
        json.append('"');
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path absolute = target.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveNotSupported) {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /** Small JSON parser limited to standard JSON values; avoids adding a runtime dependency. */
    private static final class JsonParser {
        private final String input;
        private int index;

        private JsonParser(String input) {
            this.input = input == null ? "" : input;
        }

        Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != input.length()) error("Unexpected trailing data");
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= input.length()) error("Unexpected end of JSON");
            char ch = input.charAt(index);
            return switch (ch) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (ch == '-' || Character.isDigit(ch)) yield parseNumber();
                    error("Unexpected character: " + ch);
                    yield null;
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return list;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < input.length()) {
                char ch = input.charAt(index++);
                if (ch == '"') return result.toString();
                if (ch != '\\') {
                    result.append(ch);
                    continue;
                }
                if (index >= input.length()) error("Invalid escape sequence");
                char escaped = input.charAt(index++);
                switch (escaped) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        if (index + 4 > input.length()) error("Invalid unicode escape");
                        String hex = input.substring(index, index + 4);
                        try {
                            result.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            error("Invalid unicode escape");
                        }
                        index += 4;
                    }
                    default -> error("Invalid escape character: " + escaped);
                }
            }
            error("Unterminated string");
            return "";
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) index++;
            while (index < input.length() && Character.isDigit(input.charAt(index))) index++;
            if (peek('.')) {
                index++;
                while (index < input.length() && Character.isDigit(input.charAt(index))) index++;
            }
            String raw = input.substring(start, index);
            try {
                return raw.contains(".") ? Double.parseDouble(raw) : Long.parseLong(raw);
            } catch (NumberFormatException e) {
                error("Invalid number: " + raw);
                return 0;
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!input.startsWith(literal, index)) error("Expected " + literal);
            index += literal.length();
            return value;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= input.length() || input.charAt(index) != expected) {
                error("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < input.length() && input.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++;
        }

        private void error(String message) {
            throw new IllegalArgumentException(message + " at position " + index + ".");
        }
    }
}
