package com.cjlu.inventory.model;

/**
 * The two roles supported by the system. The administrator can do everything a
 * warehouse staff member can do plus catalogue, stock-adjustment and
 * user-account administration (FR-1, FR-16).
 */
public enum UserRole {

    ADMIN("Administrator"),
    STAFF("Warehouse Staff");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
