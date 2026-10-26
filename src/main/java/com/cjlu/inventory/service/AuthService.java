package com.cjlu.inventory.service;

import com.cjlu.inventory.exception.AuthenticationException;
import com.cjlu.inventory.exception.EntityNotFoundException;
import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.User;
import com.cjlu.inventory.model.UserRole;
import com.cjlu.inventory.dao.UserDao;
import com.cjlu.inventory.util.PasswordHasher;
import com.cjlu.inventory.util.Validator;

import java.util.List;
import java.util.Optional;

/**
 * Authentication and user-account administration (FR-1, FR-16).
 *
 * <p>The service owns the session state ({@code currentUser}) and both
 * authentication checks: credentials are verified against SHA-256 hashes and
 * admin-only operations call {@link #requireAdmin()}. Menus additionally hide
 * admin options, giving defence in depth as required by the spec.</p>
 */
public class AuthService {

    private final UserDao UserDao;
    private User currentUser;

    public AuthService(UserDao UserDao) {
        this.UserDao = UserDao;
    }

    /**
     * Verifies the credentials and starts a session.
     *
     * @return the signed-in user
     * @throws AuthenticationException when the username is unknown or the password is wrong
     */
    public Optional<User> login(String username, String rawPassword) throws AuthenticationException {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new AuthenticationException("Invalid username or password.");
        }
        Optional<User> found = UserDao.findByUsername(username);
        if (found.isEmpty() || !PasswordHasher.matches(rawPassword, found.get().getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password.");
        }
        currentUser = found.get();
        return found;
    }

    /** Ends the current session. */
    public void logout() {
        currentUser = null;
    }

    public User requireCurrentUser() throws AuthenticationException {
        if (currentUser == null) {
            throw new AuthenticationException("You must be signed in to perform this action.");
        }
        return currentUser;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }

    /** Rejects the call unless an administrator is signed in. */
    public void requireAdmin() throws AuthenticationException {
        requireCurrentUser();
        if (!isAdmin()) {
            throw new AuthenticationException(
                    "Access denied: this action requires administrator privileges.");
        }
    }

    /** All accounts, in creation order (admin function). */
    public List<User> listUsers() {
        return UserDao.findAll();
    }

    /** Creates a new warehouse-staff account (admin function, FR-16). */
    public User createStaffAccount(String username, String displayName, String rawPassword)
            throws ValidationException, AuthenticationException {
        requireAdmin();
        String normalisedUsername = Validator.requireUsername(username);
        if (UserDao.existsByUsername(normalisedUsername)) {
            throw new ValidationException("Username '" + normalisedUsername + "' is already in use.");
        }
        String normalisedDisplayName = Validator.requireText(displayName, "Display name", 60);
        Validator.requirePassword(rawPassword);
        User user = new User(normalisedUsername, PasswordHasher.hash(rawPassword),
                normalisedDisplayName, UserRole.STAFF);
        UserDao.save(user);
        return user;
    }

    /** Resets another account's password (admin function, FR-16). */
    public void resetPassword(String username, String newPassword)
            throws EntityNotFoundException, ValidationException, AuthenticationException {
        requireAdmin();
        Validator.requirePassword(newPassword);
        User user = UserDao.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        user.setPasswordHash(PasswordHasher.hash(newPassword));
        UserDao.save(user);
    }

    /** Changes the signed-in user's own password (FR-1). */
    public void changeOwnPassword(String oldPassword, String newPassword)
            throws AuthenticationException, ValidationException {
        User user = requireCurrentUser();
        if (!PasswordHasher.matches(oldPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect.");
        }
        Validator.requirePassword(newPassword);
        if (PasswordHasher.matches(newPassword, user.getPasswordHash())) {
            throw new ValidationException("The new password must be different from the current password.");
        }
        user.setPasswordHash(PasswordHasher.hash(newPassword));
        UserDao.save(user);
    }

    /** Current user, or {@code null} when signed out. */
    public User getCurrentUser() {
        return currentUser;
    }
}
