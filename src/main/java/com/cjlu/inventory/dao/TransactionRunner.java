package com.cjlu.inventory.dao;

/**
 * Executes a block of work inside one database transaction.
 *
 * <p>Services that change more than one row (for example receiving a purchase
 * order, which updates the order, its lines, every product's quantity and the
 * stock-transaction log) wrap the whole operation in this boundary so that a
 * failure rolls everything back. Nested calls join the existing transaction.</p>
 */
public interface TransactionRunner {

    /** Runs {@code work} atomically; a runtime exception rolls the transaction back. */
    void runInTransaction(Runnable work);
}
