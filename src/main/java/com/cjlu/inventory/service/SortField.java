package com.cjlu.inventory.service;

/**
 * Product sort orders offered by FR-3 ({@code 3) Sort} in the product menu).
 */
public enum SortField {

    NAME("Name"),
    QUANTITY("Quantity"),
    VALUE("Inventory Value");

    private final String label;

    SortField(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
