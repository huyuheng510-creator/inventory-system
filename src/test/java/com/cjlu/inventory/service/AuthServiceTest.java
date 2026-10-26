package com.cjlu.inventory.service;

import com.cjlu.inventory.exception.AuthenticationException;
import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.User;
import com.cjlu.inventory.model.UserRole;
import com.cjlu.inventory.support.AbstractDatabaseTest;
import com.cjlu.inventory.util.PasswordHasher;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link com.cjlu.inventory.dao.UserDao} and {@link AuthService}:
 * credential storage (SHA-256, never plain text), sign-in success and failure,
 * and the role check that protects administrator-only use cases.
 */
public class AuthServiceTest extends AbstractDatabaseTest {

    @Test
    public void saveAndFindByUsernameIsCaseInsensitive() {
        createUser("Alice", "secret123", UserRole.STAFF);

        User loaded = userDao.findByUsername("ALICE").orElseThrow();
        assertEquals("usernames are stored in lower case", "alice", loaded.getUsername());
        assertEquals("Test Alice", loaded.getDisplayName());
        assertEquals(UserRole.STAFF, loaded.getRole());
        assertTrue(userDao.existsByUsername("Alice"));
        assertFalse(userDao.existsByUsername("bob"));
        assertEquals("two seeded users plus Alice", 3, userDao.count());

        List<User> users = userDao.findAll();
        assertEquals("users are listed in username order", "admin", users.get(0).getUsername());
        assertEquals("alice", users.get(1).getUsername());
        assertEquals("staff", users.get(2).getUsername());
    }

    @Test
    public void loginWithCorrectCredentialsStartsSession() throws Exception {
        User signedIn = authService.login("ADMIN", ADMIN_PASSWORD).orElseThrow();

        assertEquals(ADMIN_USERNAME, signedIn.getUsername());
        assertEquals(signedIn, authService.getCurrentUser());
        assertEquals(signedIn, authService.requireCurrentUser());
        assertTrue("the admin account must be recognised as administrator",
                authService.isAdmin());
    }

    @Test
    public void loginRejectsWrongPasswordAndUnknownUsername() {
        AuthenticationException wrongPassword = assertThrows(AuthenticationException.class,
                () -> authService.login(ADMIN_USERNAME, "not-the-password"));
        assertTrue("the error must not reveal whether the username exists",
                wrongPassword.getMessage().toLowerCase().contains("invalid"));
        assertNull("a failed sign-in must not create a session",
                authService.getCurrentUser());

        assertThrows(AuthenticationException.class,
                () -> authService.login("no-such-user", "whatever"));
        assertThrows(AuthenticationException.class,
                () -> authService.login(ADMIN_USERNAME, ""));
        assertNull(authService.getCurrentUser());
    }

    @Test
    public void passwordIsStoredAsSha256HashNotPlaintext() {
        createUser("bob", "secret123", UserRole.STAFF);

        User stored = userDao.findByUsername("bob").orElseThrow();
        String hash = stored.getPasswordHash();
        assertNotEquals("the raw password must never be persisted", "secret123", hash);
        assertEquals("the stored value must be the SHA-256 hash",
                PasswordHasher.hash("secret123"), hash);
        assertTrue("SHA-256 hex digests are 64 lower-case hex characters",
                hash.matches("[0-9a-f]{64}"));
        assertTrue(PasswordHasher.matches("secret123", hash));
        assertFalse(PasswordHasher.matches("secret124", hash));
    }

    @Test
    public void onlyAdministratorsCanManageUserAccounts() throws Exception {
        loginAsStaff();
        assertFalse(authService.isAdmin());
        assertThrows("a staff user must not pass the administrator check",
                AuthenticationException.class, authService::requireAdmin);
        assertThrows("a staff user must not create accounts",
                AuthenticationException.class,
                () -> authService.createStaffAccount("newstaff", "New Staff", "newpass1"));

        authService.logout();
        assertNull(authService.getCurrentUser());
        assertThrows("signed-out users have no session",
                AuthenticationException.class, authService::requireCurrentUser);

        loginAsAdmin();
        User created = authService.createStaffAccount("newstaff", "New Staff", "newpass1");
        assertEquals(UserRole.STAFF, created.getRole());
        assertThrows("duplicate usernames are rejected",
                ValidationException.class,
                () -> authService.createStaffAccount("newstaff", "Another Name", "somepass"));
        assertTrue("the new account can sign in",
                authService.login("newstaff", "newpass1").isPresent());
        assertEquals("newstaff", authService.getCurrentUser().getUsername());
    }
}
