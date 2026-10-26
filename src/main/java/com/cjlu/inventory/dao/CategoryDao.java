package com.cjlu.inventory.dao;

import com.cjlu.inventory.model.Category;

import java.util.Optional;

/** Data access for product categories. */
public interface CategoryDao extends Dao<Category> {

    Optional<Category> findByName(String name);
}
