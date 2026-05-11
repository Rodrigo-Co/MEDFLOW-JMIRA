package com.medflow.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    public static boolean matches(String rawPassword, String hashedPassword) {
        return rawPassword != null
                && hashedPassword != null
                && BCrypt.checkpw(rawPassword, hashedPassword);
    }
}
