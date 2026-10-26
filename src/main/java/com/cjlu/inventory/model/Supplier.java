package com.cjlu.inventory.model;

/**
 * Supplier master data. Products reference a supplier by id, and a supplier
 * cannot be deleted while products point at it (FR-5).
 *
 * <p>The {@code avgLeadDays} field tracks the rolling average of actual
 * delivery time (in calendar days) observed across all received purchase
 * orders for this supplier. It is updated each time a purchase order is fully
 * received and provides a quick lead-time reference in the replenishment
 * workflow.</p>
 */
public class Supplier extends AbstractEntity {

    private String name;
    private String contactName;
    private String phone;
    private String email;
    /** Rolling average actual delivery days (0 = no data yet). */
    private double avgLeadDays;
    /** Number of deliveries used to compute avgLeadDays. */
    private int deliveryCount;

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

    public Supplier(String id, String name, String contactName, String phone, String email,
                    double avgLeadDays, int deliveryCount) {
        super(id);
        this.name = requireName(name);
        this.contactName = safe(contactName);
        this.phone = safe(phone);
        this.email = safe(email);
        this.avgLeadDays = Math.max(0, avgLeadDays);
        this.deliveryCount = Math.max(0, deliveryCount);
    }

    /**
     * Updates the rolling average with a new delivery's actual lead time.
     *
     * @param actualDays calendar days between the order-submitted date and the
     *                   full-receipt date
     */
    public void recordDelivery(int actualDays) {
        if (actualDays < 0) {
            return;
        }
        // Incremental mean: newAvg = oldAvg + (x - oldAvg) / newCount
        deliveryCount++;
        avgLeadDays = avgLeadDays + (actualDays - avgLeadDays) / deliveryCount;
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

    public double getAvgLeadDays() {
        return avgLeadDays;
    }

    public int getDeliveryCount() {
        return deliveryCount;
    }

    /**
     * Human-readable lead-time summary, e.g. "~5.2 days (3 deliveries)" or
     * "No delivery data yet".
     */
    public String leadTimeSummary() {
        if (deliveryCount == 0) {
            return "No delivery data yet";
        }
        return String.format("~%.1f day(s) avg (%d delivery/ies)", avgLeadDays, deliveryCount);
    }

    @Override
    public String toDisplayString() {
        return name + (contactName.isEmpty() ? "" : " (" + contactName + ")");
    }
}
