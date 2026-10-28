import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PwdGeneratorTest {
    @Test
    void generatedPasswordHasRequestedLength() {
        assertEquals(20, PwdGenerator.generatePassword(20).length());
    }

    @RepeatedTest(10)
    void defaultGeneratorIncludesAllCharacterGroups() {
        String password = PwdGenerator.generatePassword(16);
        assertTrue(password.chars().anyMatch(Character::isUpperCase));
        assertTrue(password.chars().anyMatch(Character::isLowerCase));
        assertTrue(password.chars().anyMatch(Character::isDigit));
        assertTrue(password.chars().anyMatch(ch -> "!@#$%^&*()-_+=<>?".indexOf(ch) >= 0));
    }

    @Test
    void customGeneratorCanCreateDigitsOnly() {
        String password = PwdGenerator.generatePassword(24, false, false, true, false);
        assertTrue(password.chars().allMatch(Character::isDigit));
    }

    @Test
    void generatorRejectsNoCharacterGroups() {
        assertThrows(IllegalArgumentException.class,
                () -> PwdGenerator.generatePassword(12, false, false, false, false));
    }

    @Test
    void generatorRejectsTooShortLength() {
        assertThrows(IllegalArgumentException.class, () -> PwdGenerator.generatePassword(3));
    }

    @Test
    void longMixedPasswordIsRatedStrong() {
        assertEquals("Strong", PwdGenerator.estimateStrength("Abcd1234!Efgh5678?"));
    }
}
