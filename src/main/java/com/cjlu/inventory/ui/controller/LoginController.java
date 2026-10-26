package com.cjlu.inventory.ui.controller;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.exception.InventoryException;

import java.util.Objects;
import java.util.Optional;

/**
 * Login use case controller: verifies credentials through {@code AuthService}
 * and returns a user-facing error message instead of letting an exception
 * escape into the view.
 */
public final class LoginController {

    private final ApplicationContext context;

    public LoginController(ApplicationContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * Attempts to sign in.
     *
     * @return empty when the credentials are valid, otherwise the error text
     *         that the login dialog shows in red
     */
    public Optional<String> login(String username, String password) {
        try {
            context.getAuthService().login(username, password);
            return Optional.empty();
        } catch (InventoryException failure) {
            return Optional.of(failure.getMessage());
        } catch (RuntimeException failure) {
            return Optional.of("Sign-in failed: "
                    + (failure.getMessage() == null ? "unexpected error." : failure.getMessage()));
        }
    }

    /** Ends the current session. */
    public void logout() {
        context.getAuthService().logout();
    }
}
