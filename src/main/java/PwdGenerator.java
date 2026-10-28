import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/** Secure password generator used by the password editor. */
public class PwdGenerator {
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()-_+=<>?";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PwdGenerator() {
    }

    /** Keeps the original public API: generate a password containing all character groups. */
    public static String generatePassword(int length) {
        return generatePassword(length, true, true, true, true);
    }

    public static String generatePassword(
            int length,
            boolean uppercase,
            boolean lowercase,
            boolean digits,
            boolean specialCharacters
    ) {
        if (length < 4 || length > 128) {
            throw new IllegalArgumentException("Password length must be between 4 and 128 characters.");
        }

        List<String> selectedSets = new ArrayList<>();
        if (uppercase) selectedSets.add(UPPERCASE);
        if (lowercase) selectedSets.add(LOWERCASE);
        if (digits) selectedSets.add(DIGITS);
        if (specialCharacters) selectedSets.add(SPECIAL_CHARACTERS);

        if (selectedSets.isEmpty()) {
            throw new IllegalArgumentException("Select at least one character group.");
        }
        if (length < selectedSets.size()) {
            throw new IllegalArgumentException("Password is too short for the selected character groups.");
        }

        StringBuilder allCharacters = new StringBuilder();
        List<Character> characters = new ArrayList<>(length);

        for (String set : selectedSets) {
            allCharacters.append(set);
            characters.add(set.charAt(RANDOM.nextInt(set.length())));
        }

        while (characters.size() < length) {
            characters.add(allCharacters.charAt(RANDOM.nextInt(allCharacters.length())));
        }

        for (int i = characters.size() - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            Character tmp = characters.get(i);
            characters.set(i, characters.get(j));
            characters.set(j, tmp);
        }

        StringBuilder password = new StringBuilder(length);
        characters.forEach(password::append);
        return password.toString();
    }

    public static String estimateStrength(String password) {
        if (password == null || password.isEmpty()) return "Empty";
        int score = 0;
        if (password.length() >= 12) score++;
        if (password.length() >= 16) score++;
        if (password.chars().anyMatch(Character::isUpperCase)) score++;
        if (password.chars().anyMatch(Character::isLowerCase)) score++;
        if (password.chars().anyMatch(Character::isDigit)) score++;
        if (password.chars().anyMatch(ch -> SPECIAL_CHARACTERS.indexOf(ch) >= 0)) score++;

        if (score <= 2) return "Weak";
        if (score <= 4) return "Medium";
        return "Strong";
    }
}
