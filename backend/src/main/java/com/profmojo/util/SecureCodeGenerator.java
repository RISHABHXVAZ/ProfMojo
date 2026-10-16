package com.profmojo.util;

import java.security.SecureRandom;

/**
 * Utility for generating cryptographically secure confirmation codes.
 */
public final class SecureCodeGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SecureCodeGenerator() {
        // Private constructor for utility class
    }

    /**
     * Generates a cryptographically secure 4-digit numeric code formatted as 0000..9999.
     *
     * @return 4-digit zero-padded numeric string
     */
    public static String generate4DigitCode() {
        return String.format("%04d", SECURE_RANDOM.nextInt(10000));
    }
}
