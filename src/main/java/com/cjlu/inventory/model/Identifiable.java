package com.cjlu.inventory.model;

/**
 * Anything that can be addressed by a stable identifier.
 *
 * <p>Implemented by every domain entity; the generic repository layer only
 * needs this contract to index and look up entities.</p>
 */
public interface Identifiable {

    /** Immutable identifier of the entity (a UUID string in this system). */
    String getId();
}
