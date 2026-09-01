package tms.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() { }

    public static String hash(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(10));
    }

    public static boolean matches(String plaintext, String hash) {
        if (plaintext == null || hash == null) return false;
        try {
            return BCrypt.checkpw(plaintext, hash);
        } catch (IllegalArgumentException e) {
            // Malformed hash in the DB — treat as non-match rather than 500.
            return false;
        }
    }
}
