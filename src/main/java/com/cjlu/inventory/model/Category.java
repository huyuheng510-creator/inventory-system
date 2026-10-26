package com.cjlu.inventory.model;

/**
 * Product category master data. Products reference a category by id, and a
 * category cannot be deleted while products point at it (FR-4).
 */
public class Category extends AbstractEntity {

    private String name;
    private String description;

    public Category(String name, String description) {
        super();
        this.name = requireName(name);
        this.description = description == null ? "" : description.trim();
    }

    public Category(String id, String name, String description) {
        super(id);
        this.name = requireName(name);
        this.description = description == null ? "" : description.trim();
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("category name must not be blank");
        }
        return name.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireName(name);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    @Override
    public String toDisplayString() {
        return name + (description.isEmpty() ? "" : " - " + description);
    }
}
