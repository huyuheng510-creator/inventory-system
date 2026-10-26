package com.cjlu.inventory.dao;

import com.cjlu.inventory.model.User;

import java.util.Optional;

/** Data access for user accounts (authentication, user administration). */
public interface UserDao extends Dao<User> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
