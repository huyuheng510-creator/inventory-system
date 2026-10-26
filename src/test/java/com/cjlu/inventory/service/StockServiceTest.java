package com.cjlu.inventory.service;

import com.cjlu.inventory.exception.AuthenticationException;
import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockStatus;
import com.cjlu.inventory.model.StockTransaction;
import com.cjlu.inventory.model.TransactionType;
import com.cjlu.inventory.support.AbstractDatabaseTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Business-rule tests for {@link StockService}: stock-in increases the
 * on-hand quantity and writes an audit transaction, stock-out decreases it,
 * an over-sized stock-out is rejected without changing anything, a stock
 * adjustment records the signed difference and is administrator-only, and
 * registered observers are notified about low-stock transitions.
 */
public class StockServiceTest extends AbstractDatabaseTest {

    /** Creates the product used by the stock tests: 10 on hand, reorder level 5. */
    private Product givenProductOnHand() {
        return createProduct("TST-001", "Test Widget", null, null, "2.50", 10, 5);
    }

    private Product reload(Product product) {
        return productDao.findById(product.getId()).orElseThrow();
    }

    @Test
    public void stockInIncreasesQuantityAndRecordsTransaction() throws Exception {
        Product product = givenProductOnHand();
        loginAsAdmin();

        StockTransaction transaction = stockService.stockIn("TST-001", 5, "Delivery from supplier");

        assertEquals("stock-in adds the quantity to the on-hand level",
                15, reload(product).getQuantity());
        assertEquals(TransactionType.STOCK_IN, transaction.getType());
        assertEquals(5, transaction.getQuantity());
        assertEquals(5, transaction.getSignedQuantity());
        assertEquals("admin", transaction.getOperatorUsername());
        assertEquals("Delivery from supplier", transaction.getNote());
        assertEquals("exactly one audit entry is written",
                1, transactionDao.count());
        assertEquals(transaction.getId(),
                transactionDao.findByProductId(product.getId()).get(0).getId());
    }

    @Test
    public void stockOutDecreasesQuantityAndRecordsTransaction() throws Exception {
        Product product = givenProductOnHand();
        loginAsStaff();

        StockTransaction transaction = stockService.stockOut("TST-001", 4, "Customer dispatch");

        assertEquals(6, reload(product).getQuantity());
        assertEquals(TransactionType.STOCK_OUT, transaction.getType());
        assertEquals(4, transaction.getQuantity());
        assertEquals("stock-out has a negative signed effect",
                -4, transaction.getSignedQuantity());
        assertEquals("staff", transaction.getOperatorUsername());
        assertEquals(1, transactionDao.findByProductId(product.getId()).size());
    }

    @Test
    public void stockOutBeyondOnHandIsRejectedWithoutWritingTransaction() throws Exception {
        Product product = givenProductOnHand();
        loginAsStaff();

        ValidationException failure = assertThrows(ValidationException.class,
                () -> stockService.stockOut("TST-001", 11, "Too much"));

        assertTrue("the error names the available stock: " + failure.getMessage(),
                failure.getMessage().contains("Insufficient stock"));
        assertEquals("the failed stock-out must not change the quantity",
                10, reload(product).getQuantity());
        assertEquals("the failed stock-out must not write an audit entry",
                0, transactionDao.count());

        stockService.stockOut("TST-001", 10, "Sell everything");
        assertEquals("stock-out may consume exactly the on-hand quantity",
                0, reload(product).getQuantity());
    }

    @Test
    public void adjustStockRecordsSignedDifferenceAndIsAdminOnly() throws Exception {
        Product product = givenProductOnHand();
        loginAsAdmin();

        StockTransaction missing = stockService.adjustStock("TST-001", 7, "Cycle count: 3 missing");
        assertEquals(7, reload(product).getQuantity());
        assertEquals(TransactionType.ADJUSTMENT, missing.getType());
        assertEquals("the adjustment stores the signed difference", -3, missing.getQuantity());
        assertEquals(-3, missing.getSignedQuantity());
        assertEquals("Cycle count: 3 missing", missing.getNote());

        StockTransaction found = stockService.adjustStock("TST-001", 12, "Cycle count: 5 found");
        assertEquals(12, reload(product).getQuantity());
        assertEquals(5, found.getQuantity());
        assertEquals(2, transactionDao.findByProductId(product.getId()).size());
        assertThrows("a count equal to the system quantity needs no adjustment",
                ValidationException.class,
                () -> stockService.adjustStock("TST-001", 12, "No change"));

        authService.logout();
        loginAsStaff();
        assertThrows("stock adjustments are reserved for administrators",
                AuthenticationException.class,
                () -> stockService.adjustStock("TST-001", 1, "Staff count"));
        assertEquals("the rejected adjustment must not change the quantity",
                12, reload(product).getQuantity());
        assertEquals(2, transactionDao.count());
    }

    @Test
    public void stockOperationsRequireASignedInUser() {
        Product product = givenProductOnHand();

        assertThrows("signed-out users must not move stock",
                AuthenticationException.class,
                () -> stockService.stockIn("TST-001", 5, "Anonymous"));
        assertThrows(AuthenticationException.class,
                () -> stockService.stockOut("TST-001", 1, "Anonymous"));
        assertEquals(10, reload(product).getQuantity());
        assertEquals(0, transactionDao.count());
    }

    @Test
    public void observersAreNotifiedAboutLowStockTransition() throws Exception {
        Product product = givenProductOnHand();
        List<String> transitions = new ArrayList<>();
        List<Integer> observedQuantities = new ArrayList<>();
        stockService.addObserver((changedProduct, previous, current) -> {
            transitions.add(previous + "->" + current);
            observedQuantities.add(changedProduct.getQuantity());
        });
        loginAsStaff();

        stockService.stockOut("TST-001", 6, "Sale");
        assertEquals("stock fell to 4, i.e. at or below the reorder level of 5",
                List.of("IN_STOCK->LOW_STOCK"), transitions);
        assertEquals(List.of(4), observedQuantities);
        assertEquals(StockStatus.LOW_STOCK, reload(product).getStockStatus());

        stockService.stockIn("TST-001", 10, "Replenishment");
        assertEquals(List.of("IN_STOCK->LOW_STOCK", "LOW_STOCK->IN_STOCK"), transitions);
        assertEquals(List.of(4, 14), observedQuantities);
        assertEquals(StockStatus.IN_STOCK, reload(product).getStockStatus());
    }
}
