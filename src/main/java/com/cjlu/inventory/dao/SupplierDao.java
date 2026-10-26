package com.cjlu.inventory.dao;

import com.cjlu.inventory.model.Supplier;

import java.util.Optional;

/** Data access for suppliers. */
public interface SupplierDao extends Dao<Supplier> {

    Optional<Supplier> findByName(String name);
}
