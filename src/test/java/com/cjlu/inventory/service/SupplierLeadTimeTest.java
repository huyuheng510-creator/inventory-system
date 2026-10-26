package com.cjlu.inventory.service;

import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.PurchaseOrder;
import com.cjlu.inventory.model.PurchaseOrderStatus;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.support.AbstractDatabaseTest;
import org.junit.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the supplier lead-time tracking (FR-10 extension).
 *
 * <p>Two levels are covered: the pure arithmetic in
 * {@link Supplier#recordDelivery(int)} and the end-to-end behaviour of
 * {@link PurchaseOrderService}, where a fully received purchase order feeds
 * its actual delivery duration back into the supplier's rolling average.</p>
 */
public class SupplierLeadTimeTest extends AbstractDatabaseTest {

    private Supplier reload(Supplier supplier) {
        return supplierDao.findById(supplier.getId()).orElseThrow();
    }

    // ------------------------------------------------------- arithmetic

    @Test
    public void aNewSupplierHasNoDeliveryData() {
        Supplier supplier = new Supplier("NEW-1", "New Supplier", "contact", "555", "a@b.c");

        assertEquals(0.0, supplier.getAvgLeadDays(), 0.0001);
        assertEquals(0, supplier.getDeliveryCount());
        assertEquals("No delivery data yet", supplier.leadTimeSummary());
    }

    @Test
    public void theFirstDeliverySetsTheAverageExactly() {
        Supplier supplier = new Supplier("L1", "Lead One", "c", "555", "a@b.c");

        supplier.recordDelivery(6);

        assertEquals("the first delivery is the average", 6.0, supplier.getAvgLeadDays(), 0.0001);
        assertEquals(1, supplier.getDeliveryCount());
        assertEquals("~6.0 day(s) avg (1 delivery/ies)", supplier.leadTimeSummary());
    }

    @Test
    public void theAverageIsUpdatedIncrementallyAcrossDeliveries() {
        Supplier supplier = new Supplier("L2", "Lead Two", "c", "555", "a@b.c");

        supplier.recordDelivery(4);
        supplier.recordDelivery(8);

        assertEquals("the average of 4 and 8 is 6", 6.0, supplier.getAvgLeadDays(), 0.0001);
        assertEquals(2, supplier.getDeliveryCount());
    }

    @Test
    public void theAverageConvergesOnARecentFastDelivery() {
        Supplier supplier = new Supplier("L3", "Lead Three", "c", "555", "a@b.c");

        supplier.recordDelivery(10);
        supplier.recordDelivery(10);
        supplier.recordDelivery(2);

        assertEquals("the rolling mean of 10, 10 and 2 is 7.333...",
                22.0 / 3.0, supplier.getAvgLeadDays(), 0.0001);
        assertEquals(3, supplier.getDeliveryCount());
    }

    @Test
    public void aNegativeDeliveryIsIgnored() {
        Supplier supplier = new Supplier("L4", "Lead Four", "c", "555", "a@b.c");

        supplier.recordDelivery(5);
        supplier.recordDelivery(-3);

        assertEquals("a negative duration cannot move the average",
                5.0, supplier.getAvgLeadDays(), 0.0001);
        assertEquals("and it is not counted as a delivery", 1, supplier.getDeliveryCount());
    }

    @Test
    public void theAverageSurvivesARoundTripThroughTheDatabase() {
        Supplier supplier = createSupplier("Persisted Supplier");
        supplier.recordDelivery(3);
        supplier.recordDelivery(7);
        supplierDao.save(supplier);

        Supplier stored = reload(supplier);

        assertEquals("the average is persisted", 5.0, stored.getAvgLeadDays(), 0.0001);
        assertEquals("the delivery count is persisted", 2, stored.getDeliveryCount());
    }

    // ------------------------------------------------- purchase-order flow

    @Test
    public void aFullyReceivedOrderRecordsTheActualDeliveryTime() throws Exception {
        Supplier supplier = createSupplier("Fast Supplier");
        Product product = createProduct("LT-001", "Lead Widget", null, supplier,
                "5.00", 0, 10);
        loginAsAdmin();

        PurchaseOrder order = purchaseOrderService.createDraft(supplier.getId(),
                LocalDate.now().plusDays(3), "Lead-time test",
                List.of(new PurchaseOrderService.OrderLineDraft(
                        product.getId(), 10, new java.math.BigDecimal("5.00"))));
        purchaseOrderService.submit(order.getId());
        purchaseOrderService.receiveRemaining(order.getId());

        Supplier updated = reload(supplier);
        assertTrue("the fully received order produced a delivery record",
                updated.getDeliveryCount() >= 1);
        assertTrue("the recorded average is a sensible number of days",
                updated.getAvgLeadDays() >= 0.0 && updated.getAvgLeadDays() < 30.0);
    }

    @Test
    public void aPartiallyReceivedOrderDoesNotRecordADeliveryTime() throws Exception {
        Supplier supplier = createSupplier("Partial Supplier");
        Product product = createProduct("LT-002", "Partial Widget", null, supplier,
                "5.00", 0, 10);
        loginAsAdmin();

        PurchaseOrder order = purchaseOrderService.createDraft(supplier.getId(),
                LocalDate.now().plusDays(3), "Partial lead-time test",
                List.of(new PurchaseOrderService.OrderLineDraft(
                        product.getId(), 10, new java.math.BigDecimal("5.00"))));
        purchaseOrderService.submit(order.getId());

        // Receive only part of the order.
        PurchaseOrder reloaded = purchaseOrderService.requireOrder(order.getId());
        String lineId = reloaded.getLines().get(0).getId();
        purchaseOrderService.receive(order.getId(), Map.of(lineId, 4));

        assertEquals("a partial receipt is not a completed delivery",
                0, reload(supplier).getDeliveryCount());
    }

    @Test
    public void theRecordedLeadTimeReachesTheAdministrationScreen() throws Exception {
        Supplier supplier = createSupplier("Visible Supplier");
        supplier.recordDelivery(9);
        supplierDao.save(supplier);

        // The administration table reads the summary through the table model.
        com.cjlu.inventory.ui.table.SupplierTableModel model =
                new com.cjlu.inventory.ui.table.SupplierTableModel();
        model.setSuppliers(supplierDao.findAll());

        assertEquals(1, model.getRowCount());
        assertEquals("Avg Lead Time", model.getColumnName(4));
        assertTrue("the summary is shown in the table: " + model.getValueAt(0, 4),
                model.getValueAt(0, 4).toString().contains("9.0"));
    }
}
