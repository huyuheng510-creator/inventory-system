package com.cjlu.inventory.service;

import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockAlert;
import com.cjlu.inventory.model.StockStatus;
import com.cjlu.inventory.support.AbstractDatabaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link AlertHistoryService}, the in-session Observer store that
 * backs the Dashboard "Stock Alert History" panel.
 *
 * <p>The service is registered with {@link StockService} as a
 * {@code StockLevelObserver}, so the tests drive it through real stock
 * movements rather than by calling the callback directly. This checks both
 * that the observer is notified and that the recorded entry describes the
 * product state at the moment of the transition.</p>
 */
public class AlertHistoryServiceTest extends AbstractDatabaseTest {

    private AlertHistoryService alertHistory;

    /** Product with 10 on hand and a reorder level of 5. */
    private Product givenProductOnHand() {
        return createProduct("ALR-001", "Alert Widget", null, null, "3.00", 10, 5);
    }

    @Before
    public void registerObserver() {
        alertHistory = new AlertHistoryService();
        stockService.addObserver(alertHistory);
    }

    @Test
    public void lowStockTransitionIsRecordedWithTheProductState() throws Exception {
        givenProductOnHand();
        loginAsStaff();

        stockService.stockOut("ALR-001", 6, "Sale to customer");

        List<StockAlert> alerts = alertHistory.getAlerts();
        assertEquals("exactly one status change was recorded", 1, alerts.size());
        StockAlert alert = alerts.get(0);
        assertEquals("ALR-001", alert.getProductSku());
        assertEquals("Alert Widget", alert.getProductName());
        assertEquals(StockStatus.LOW_STOCK, alert.getStatus());
        assertEquals("the alert carries the quantity after the movement", 4, alert.getQuantity());
        assertEquals(5, alert.getReorderLevel());
        assertEquals("piece", alert.getUnit());
        assertNotNull("the alert is timestamped", alert.getOccurredAt());
    }

    @Test
    public void alertsAreReturnedMostRecentFirst() throws Exception {
        givenProductOnHand();
        loginAsStaff();

        stockService.stockOut("ALR-001", 6, "First warning");
        stockService.stockIn("ALR-001", 10, "Restock");
        stockService.stockOut("ALR-001", 9, "Second warning");

        List<StockAlert> alerts = alertHistory.getAlerts();
        assertEquals("three status changes were recorded", 3, alerts.size());
        assertEquals("the newest alert comes first", StockStatus.LOW_STOCK,
                alerts.get(0).getStatus());
        assertEquals("the recovery is also visible", StockStatus.IN_STOCK,
                alerts.get(1).getStatus());
        assertEquals("the oldest alert comes last", StockStatus.LOW_STOCK,
                alerts.get(2).getStatus());
    }

    @Test
    public void recoveryAfterRestockIsRecordedAsAnAllClearEntry() throws Exception {
        givenProductOnHand();
        loginAsStaff();

        stockService.stockOut("ALR-001", 6, "Sale");
        stockService.stockIn("ALR-001", 10, "Restock");

        assertEquals("both the warning and the recovery are kept", 2, alertHistory.size());
        assertEquals("the recovery entry reports IN_STOCK", StockStatus.IN_STOCK,
                alertHistory.getAlerts().get(0).getStatus());
        assertEquals("only one of the two entries is a warning", 1,
                alertHistory.getWarningsOnly().size());
    }

    @Test
    public void movementsThatDoNotChangeTheStatusAreNotRecorded() throws Exception {
        givenProductOnHand();
        loginAsStaff();

        // 10 -> 9 stays above the reorder level of 5, so the status is unchanged.
        stockService.stockOut("ALR-001", 1, "Small sale");

        assertEquals("an unchanged status produces no alert", 0, alertHistory.size());
    }

    @Test
    public void outOfStockTransitionIsRecorded() throws Exception {
        createProduct("ALR-002", "Empty Widget", null, null, "1.00", 3, 1);
        loginAsStaff();

        stockService.stockOut("ALR-002", 3, "Sold out");

        List<StockAlert> alerts = alertHistory.getAlerts();
        assertEquals(1, alerts.size());
        assertEquals(StockStatus.OUT_OF_STOCK, alerts.get(0).getStatus());
        assertEquals(0, alerts.get(0).getQuantity());
    }

    @Test
    public void clearDiscardsEveryRecordedAlert() throws Exception {
        givenProductOnHand();
        loginAsStaff();
        stockService.stockOut("ALR-001", 6, "Sale");
        assertTrue("an alert was recorded before the clear", alertHistory.size() > 0);

        alertHistory.clear();

        assertEquals("the history is empty after the clear", 0, alertHistory.size());
        assertTrue(alertHistory.getAlerts().isEmpty());
        assertTrue(alertHistory.getWarningsOnly().isEmpty());
    }

    @Test
    public void theHistoryIsCappedSoLongSessionsCannotGrowWithoutBound() throws Exception {
        givenProductOnHand();
        loginAsStaff();

        // Alternate above and below the reorder level to generate many transitions.
        for (int round = 0; round < 120; round++) {
            stockService.stockOut("ALR-001", 6, "Warning");
            stockService.stockIn("ALR-001", 6, "Recovery");
        }

        assertTrue("the history is bounded at " + AlertHistoryService.MAX_ALERTS
                        + " entries, was " + alertHistory.size(),
                alertHistory.size() <= AlertHistoryService.MAX_ALERTS);
        assertEquals("the cap is actually reached", AlertHistoryService.MAX_ALERTS,
                alertHistory.size());
    }
}
