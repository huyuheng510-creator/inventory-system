package com.cjlu.inventory.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Common identity behaviour for all domain entities (identity by id, not by
 * field values).
 */
public abstract class AbstractEntity implements Identifiable {

    private final String id;
    private final LocalDateTime createdAt;

    /** Creates a new entity with a fresh generated id and the current timestamp. */
    protected AbstractEntity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    /** Re-creates an entity that was loaded from persistent storage. */
    protected AbstractEntity(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Entity id must not be blank");
        }
        this.id = id;
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public final String getId() {
        return id;
    }

    /** Creation timestamp of this in-memory instance. */
    public final LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Short one-line representation used by console tables. */
    public abstract String toDisplayString();

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AbstractEntity entity)) {
            return false;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        return id.equals(entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}
