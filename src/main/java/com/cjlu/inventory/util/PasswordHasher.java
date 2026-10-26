package com.cjlu.inventory.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * SHA-256 password hashing helper.
 *
 * <p>Passwords are never stored in plain text: {@code users.csv} keeps only the
 * lower-case hexadecimal digest produced here (FR-1, FR-16).</p>
 */
public final class PasswordHasher {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private PasswordHasher() {
        // utility class
    }

    /** Returns the lower-case SHA-256 hex digest of the raw password. */
    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            char[] characters = new char[hashed.length * 2];
            for (int i = 0; i < hashed.length; i++) {
                int value = hashed[i] & 0xff;
                characters[i * 2] = HEX_DIGITS[value >>> 4];
                characters[i * 2 + 1] = HEX_DIGITS[value & 0x0f];
            }
            return new String(characters);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandatory in every Java SE 17 implementation.
            throw new IllegalStateException("SHA-256 is not available in this JVM", e);
        }
    }

    /** Compares a raw password with a stored hash (case-insensitive on the hash). */
    public static boolean matches(String rawPassword, String expectedHash) {
        if (rawPassword == null || expectedHash == null) {
            return false;
        }
        return hash(rawPassword).equalsIgnoreCase(expectedHash);
    }
}
