package com.cjlu.inventory.dao;

import com.cjlu.inventory.model.StockTransaction;

import java.time.LocalDateTime;
import java.util.List;

/** Data access for the auditable stock transaction log. */
public interface TransactionDao extends Dao<StockTransaction> {

    List<StockTransaction> findByProductId(String productId);

    /** Transactions with from &lt;= occurredAt &lt; to, oldest first. */
    List<StockTransaction> findBetween(LocalDateTime fromInclusive, LocalDateTime toExclusive);

    /** The most recent transactions, newest first. */
    List<StockTransaction> findRecent(int limit);
}
