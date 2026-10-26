package com.cjlu.inventory.dao;

import com.cjlu.inventory.model.Identifiable;

import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object contract (DAO pattern).
 *
 * <p>Business services depend only on these interfaces, never on JDBC or
 * Derby. {@code dao.jdbc.Jdbc*Dao} implements them on top of the embedded
 * Apache Derby database, which keeps the persistence technology replaceable
 * and the service layer unit-testable.</p>
 *
 * @param <T> entity type handled by the DAO
 */
public interface Dao<T extends Identifiable> {

    /** Finds one entity by its immutable id. */
    Optional<T> findById(String id);

    /** All entities in a stable, user-facing order. */
    List<T> findAll();

    /** Inserts a new entity or updates the row with the same id. */
    void save(T entity);

    /** Deletes the row with the given id; a missing row is ignored. */
    void deleteById(String id);

    boolean existsById(String id);

    int count();
}
