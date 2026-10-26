package com.cjlu.inventory.model;

/**
 * Supplier master data. Products reference a supplier by id, and a supplier
 * cannot be deleted while products point at it (FR-5).
 */
public class Supplier extends AbstractEntity {

    private String name;
    private String contactName;
    private String phone;
    private String email;

    public Supplier(String name, String contactName, String phone, String email) {
        super();
        this.name = requireName(name);
        this.contactName = safe(contactName);
        this.phone = safe(phone);
        this.email = safe(email);
    }

    public Supplier(String id, String name, String contactName, String phone, String email) {
        super(id);
        this.name = requireName(name);
        this.contactName = safe(contactName);
        this.phone = safe(phone);
        this.email = safe(email);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("supplier name must not be blank");
        }
        return name.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireName(name);
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = safe(contactName);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = safe(phone);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = safe(email);
    }

    @Override
    public String toDisplayString() {
        return name + (contactName.isEmpty() ? "" : " (" + contactName + ")");
    }
}
