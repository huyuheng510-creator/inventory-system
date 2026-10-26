package com.cjlu.inventory.dao;

import com.cjlu.inventory.model.Product;

import java.util.List;
import java.util.Optional;

/**
 * Data access for the product catalogue, including the search, filter and
 * reorder queries used by the GUI tables and reports.
 */
public interface ProductDao extends Dao<Product> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    /** Case-insensitive match of the keyword against SKU or product name. */
    List<Product> search(String keyword);

    List<Product> findByCategoryId(String categoryId);

    List<Product> findBySupplierId(String supplierId);

    /** Products at or below their reorder level, in SKU order. */
    List<Product> findLowStock();
}
