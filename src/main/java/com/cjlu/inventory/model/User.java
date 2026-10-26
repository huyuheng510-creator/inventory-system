package com.cjlu.inventory.model;

import java.util.Locale;
import java.util.Objects;

/**
 * A system user account. Passwords are stored only as SHA-256 hashes; the
 * username is the natural key and is kept in lower case (FR-1, FR-16).
 */
public class User extends AbstractEntity {

    private final String username;
    private String passwordHash;
    private String displayName;
    private UserRole role;

    public User(String username, String passwordHash, String displayName, UserRole role) {
        super();
        this.username = normaliseUsername(username);
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.role = Objects.requireNonNull(role, "role");
    }

    public User(String id, String username, String passwordHash, String displayName, UserRole role) {
        super(id);
        this.username = normaliseUsername(username);
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.role = Objects.requireNonNull(role, "role");
    }

    private static String normaliseUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        this.displayName = displayName.trim();
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = Objects.requireNonNull(role, "role");
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    @Override
    public String toDisplayString() {
        return username + " (" + displayName + ", " + role.getLabel() + ")";
    }
}
