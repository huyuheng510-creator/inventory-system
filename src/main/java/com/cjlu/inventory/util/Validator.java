package com.cjlu.inventory.util;

import com.cjlu.inventory.exception.ValidationException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Stateless field-level validation shared by every service.
 *
 * <p>Console helpers in {@code ui.ConsoleIO} validate the <em>syntax</em> of an
 * input (it is a whole number, it is a date); this class validates the
 * <em>business</em> rules and formats so that the same rules also protect a
 * future GUI or any other caller (FR-15, NFR-7). All methods normalise the
 * value they return (trimmed, upper-case SKU, lower-case username).</p>
 */
public final class Validator {

    public static final int MAX_TEXT_LENGTH = 80;
    public static final int MAX_NOTE_LENGTH = 120;
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 64;

    private static final Pattern SKU_PATTERN = Pattern.compile("[A-Z0-9-]{3,20}");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-z0-9_]{3,20}");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("[0-9+\\- ]{6,20}");

    private Validator() {
        // utility class
    }

    /** Trims a required text field and checks its maximum length. */
    public static String requireText(String value, String fieldName) throws ValidationException {
        return requireText(value, fieldName, MAX_TEXT_LENGTH);
    }

    /** Trims a required text field and checks its maximum length. */
    public static String requireText(String value, String fieldName, int maxLength) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " must not be blank.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException(fieldName + " must be at most " + maxLength + " characters.");
        }
        return trimmed;
    }

    /** Trims an optional text field and enforces its maximum length. */
    public static String optionalText(String value, String fieldName, int maxLength) throws ValidationException {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException(fieldName + " must be at most " + maxLength + " characters.");
        }
        return trimmed;
    }

    /** Validates and normalises a product SKU: {@code [A-Z0-9-]{3,20}}. */
    public static String requireSku(String sku) throws ValidationException {
        if (sku == null || sku.isBlank()) {
            throw new ValidationException("SKU must not be blank.");
        }
        String normalised = sku.trim().toUpperCase();
        if (!SKU_PATTERN.matcher(normalised).matches()) {
            throw new ValidationException(
                    "SKU must be 3-20 characters using A-Z, 0-9 or '-' (for example ELE-001).");
        }
        return normalised;
    }

    /** Validates and normalises a username: {@code [a-z0-9_]{3,20}}. */
    public static String requireUsername(String username) throws ValidationException {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username must not be blank.");
        }
        String normalised = username.trim().toLowerCase();
        if (!USERNAME_PATTERN.matcher(normalised).matches()) {
            throw new ValidationException(
                    "Username must be 3-20 characters using lower-case letters, digits or '_'.");
        }
        return normalised;
    }

    /** Validates a raw password before it is hashed. */
    public static void requirePassword(String rawPassword) throws ValidationException {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ValidationException("Password must not be blank.");
        }
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }
        if (rawPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException(
                    "Password must be at most " + MAX_PASSWORD_LENGTH + " characters long.");
        }
    }

    public static void requireNonNegativeInt(int value, String fieldName) throws ValidationException {
        if (value < 0) {
            throw new ValidationException(fieldName + " must be zero or a positive whole number.");
        }
    }

    public static void requirePositiveInt(int value, String fieldName) throws ValidationException {
        if (value <= 0) {
            throw new ValidationException(fieldName + " must be a positive whole number.");
        }
    }

    public static void requireNonNegativeDecimal(BigDecimal value, String fieldName) throws ValidationException {
        if (value == null) {
            throw new ValidationException(fieldName + " must not be blank.");
        }
        if (value.signum() < 0) {
            throw new ValidationException(fieldName + " must be zero or positive.");
        }
        if (value.scale() > 2) {
            throw new ValidationException(fieldName + " must have at most 2 decimal places.");
        }
    }

    /** Validates a supplier e-mail address. */
    public static String requireEmail(String email) throws ValidationException {
        String trimmed = requireText(email, "E-mail", MAX_TEXT_LENGTH);
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("E-mail must be a valid address (for example sales@example.com).");
        }
        return trimmed;
    }

    /** Validates a supplier phone number (digits, spaces, '+', '-'). */
    public static String requirePhone(String phone) throws ValidationException {
        String trimmed = requireText(phone, "Phone", 20);
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Phone must be 6-20 characters using digits, spaces, '+' or '-'.");
        }
        return trimmed;
    }

    /** Validates a note/reason that may be optional. */
    public static String optionalNote(String note) throws ValidationException {
        return optionalText(note, "Note", MAX_NOTE_LENGTH);
    }
}
