package autograder.demo;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyntheticRosterTest {
    private static final Pattern USERNAME = Pattern.compile("^[a-z]+(?:\\.[a-z]+)*\\.\\d{4}$");
    private static final Set<String> REAL = Set.of("ping.lee.2023", "chee.teo.2022");

    @Test
    void producesUniqueNonCollidingIdentitiesInSheetFormat() {
        List<SyntheticRoster.Identity> roster = SyntheticRoster.generate(300, 102, REAL, 1400007, 8);

        Set<String> ids = new HashSet<>();
        Set<String> usernames = new HashSet<>(REAL);
        for (SyntheticRoster.Identity student : roster) {
            assertTrue(ids.add(student.id()));
            assertTrue(usernames.add(student.username()));
            assertTrue(USERNAME.matcher(student.username()).matches());
            assertEquals(student.username() + "@computing.smu.edu.sg", student.email());
            assertEquals(student.fullName(), student.fullName().toUpperCase());
        }
        assertEquals("#01400007", roster.get(0).id());
        assertEquals("#01400306", roster.get(299).id());
        assertFalse(ids.contains("#01400006"));
    }

    @Test
    void sameSeedGivesSameRosterAndDifferentSeedDiffers() {
        List<SyntheticRoster.Identity> first = SyntheticRoster.generate(20, 5, REAL, 1, 8);

        assertEquals(first, SyntheticRoster.generate(20, 5, REAL, 1, 8));
        assertFalse(first.equals(SyntheticRoster.generate(20, 6, REAL, 1, 8)));
    }

    @Test
    void rejectsMoreStudentsThanAvailableNames() {
        assertThrows(IllegalArgumentException.class,
                () -> SyntheticRoster.generate(401, 1, REAL, 1, 8));
    }
}
