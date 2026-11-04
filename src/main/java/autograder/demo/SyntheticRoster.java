package autograder.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/** Deterministic fake student identities that never collide with existing usernames. */
final class SyntheticRoster {
    static final String EMAIL_DOMAIN = "computing.smu.edu.sg";
    private static final List<String> FIRST = List.of("aiden", "bella", "caleb", "dara", "ethan",
            "farah", "gavin", "hana", "isaac", "jia", "kian", "lena", "marcus", "nadia", "owen",
            "priya", "quinn", "rui", "sofia", "tariq");
    private static final List<String> LAST = List.of("abdul", "bose", "chua", "dsouza", "eng",
            "foo", "goh", "heng", "ibrahim", "jaya", "koh", "loh", "mohan", "ng", "ong", "pillai",
            "quek", "rahman", "soh", "tan");
    private static final int[] YEARS = {2019, 2020, 2021, 2022, 2023, 2024};
    static final int MAX_COUNT = FIRST.size() * LAST.size();

    record Identity(String id, String username, String fullName, String email) {
        String headerName() {
            String[] parts = username.split("\\.");
            return capitalize(parts[0]) + " " + capitalize(parts[1]);
        }
    }

    private SyntheticRoster() {
    }

    static List<Identity> generate(int count, long seed, Set<String> reservedUsernames,
            long firstId, int idWidth) {
        List<String> combos = new ArrayList<>();
        for (String first : FIRST) {
            for (String last : LAST) {
                combos.add(first + "." + last);
            }
        }
        if (count > combos.size()) {
            throw new IllegalArgumentException("at most " + combos.size() + " synthetic students");
        }
        Collections.shuffle(combos, new Random(seed));
        Random years = new Random(seed + 1);
        Set<String> used = new HashSet<>();
        reservedUsernames.forEach(name -> used.add(name.toLowerCase(Locale.ROOT)));
        List<Identity> identities = new ArrayList<>();
        for (String combo : combos) {
            if (identities.size() == count) {
                break;
            }
            String username = combo + "." + YEARS[years.nextInt(YEARS.length)];
            if (used.add(username)) {
                long id = firstId + identities.size();
                String padded = String.format("%0" + idWidth + "d", id);
                String fullName = combo.replace('.', ' ').toUpperCase(Locale.ROOT);
                identities.add(new Identity("#" + padded, username, fullName,
                        username + "@" + EMAIL_DOMAIN));
            }
        }
        return identities;
    }

    private static String capitalize(String word) {
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }
}
