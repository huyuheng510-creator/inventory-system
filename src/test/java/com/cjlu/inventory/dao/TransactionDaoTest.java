package com.cjlu.inventory.dao;

import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockTransaction;
import com.cjlu.inventory.model.TransactionType;
import com.cjlu.inventory.support.AbstractDatabaseTest;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Database-level tests for {@link TransactionDao}: storing audit entries,
 * loading the history of one product in chronological order, the half-open
 * date-range query ({@code from <= occurredAt < to}) and the newest-first
 * query with a row limit.
 */
public class TransactionDaoTest extends AbstractDatabaseTest {

    private static final LocalDateTime MARCH_1 = LocalDateTime.of(2026, 3, 1, 9, 30);
    private static final LocalDateTime MARCH_2 = LocalDateTime.of(2026, 3, 2, 9, 30);
    private static final LocalDateTime MARCH_3 = LocalDateTime.of(2026, 3, 3, 9, 30);
    private static final LocalDateTime MARCH_4 = LocalDateTime.of(2026, 3, 4, 9, 30);

    @Test
    public void saveAndFindByProductIdReturnsOldestFirst() {
        Product product = createProduct("ELE-001", "Wireless Mouse", null, null,
                "89.00", 42, 10);
        Product other = createProduct("ELE-002", "USB-C Cable", null, null,
                "25.50", 6, 15);

        StockTransaction third = recordTransaction(product, TransactionType.STOCK_IN, 10, MARCH_3);
        StockTransaction first = recordTransaction(product, TransactionType.STOCK_OUT, 3, MARCH_1);
        StockTransaction second = recordTransaction(product, TransactionType.ADJUSTMENT, -2, MARCH_2);
        recordTransaction(other, TransactionType.STOCK_IN, 7, MARCH_2);

        List<StockTransaction> history = transactionDao.findByProductId(product.getId());
        assertEquals("only the transactions of the requested product are returned",
                3, history.size());
        assertEquals(first.getId(), history.get(0).getId());
        assertEquals(second.getId(), history.get(1).getId());
        assertEquals(third.getId(), history.get(2).getId());

        StockTransaction reloaded = transactionDao.findById(second.getId()).orElseThrow();
        assertEquals(TransactionType.ADJUSTMENT, reloaded.getType());
        assertEquals(-2, reloaded.getQuantity());
        assertMoneyEquals("89.00", reloaded.getUnitPrice());
        assertEquals("admin", reloaded.getOperatorUsername());
        assertEquals(MARCH_2, reloaded.getOccurredAt());
        assertEquals(4, transactionDao.count());
    }

    @Test
    public void findBetweenUsesInclusiveStartAndExclusiveEnd() {
        Product product = createProduct("ELE-001", "Wireless Mouse", null, null,
                "89.00", 42, 10);
        recordTransaction(product, TransactionType.STOCK_IN, 10, MARCH_1);
        StockTransaction atStart = recordTransaction(product, TransactionType.STOCK_OUT, 1, MARCH_2);
        StockTransaction inRange = recordTransaction(product, TransactionType.STOCK_OUT, 2, MARCH_3);
        recordTransaction(product, TransactionType.STOCK_IN, 5, MARCH_4);

        List<StockTransaction> range = transactionDao.findBetween(MARCH_2, MARCH_4);

        assertEquals("the start of the range is inclusive, the end is exclusive",
                2, range.size());
        assertEquals(atStart.getId(), range.get(0).getId());
        assertEquals(inRange.getId(), range.get(1).getId());
        assertTrue("the range result is ordered oldest first",
                range.get(0).getOccurredAt().isBefore(range.get(1).getOccurredAt()));
    }

    @Test
    public void findRecentReturnsNewestFirstRespectingTheLimit() {
        Product product = createProduct("ELE-001", "Wireless Mouse", null, null,
                "89.00", 42, 10);
        recordTransaction(product, TransactionType.STOCK_IN, 10, MARCH_1);
        recordTransaction(product, TransactionType.STOCK_OUT, 3, MARCH_2);
        StockTransaction newest = recordTransaction(product, TransactionType.STOCK_IN, 4, MARCH_3);

        List<StockTransaction> recent = transactionDao.findRecent(2);

        assertEquals(2, recent.size());
        assertEquals("findRecent must return the newest transactions first",
                newest.getId(), recent.get(0).getId());
        assertEquals(MARCH_2, recent.get(1).getOccurredAt());
        assertEquals("the limit must not cap the result below the stored row count",
                3, transactionDao.findRecent(10).size());
    }
}
