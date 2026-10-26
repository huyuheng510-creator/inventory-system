package com.cjlu.inventory.dao;

import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.PurchaseOrder;
import com.cjlu.inventory.model.PurchaseOrderLine;
import com.cjlu.inventory.model.PurchaseOrderStatus;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.support.AbstractDatabaseTest;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Database-level tests for {@link PurchaseOrderDao} and
 * {@link PurchaseOrderLineDao}: the order header round-trip, the
 * human-readable order-number generator ({@code PO-yyyyMMdd-NNN}) and the
 * status query, plus deleting the lines of one order without touching another.
 */
public class PurchaseOrderDaoTest extends AbstractDatabaseTest {

    @Test
    public void saveAndFindByIdRoundTripsHeaderAndLines() {
        Supplier supplier = createSupplier("Shenzhen Electronics");
        Product mouse = createProduct("ELE-001", "Wireless Mouse", null, supplier,
                "89.00", 42, 10);
        Product cable = createProduct("ELE-002", "USB-C Cable", null, supplier,
                "25.50", 6, 15);
        LocalDate expectedDate = LocalDate.of(2026, 4, 15);
        PurchaseOrder order = new PurchaseOrder("PO-20260301-001", supplier.getId(), "admin",
                expectedDate, "Replenish electronics");
        purchaseOrderDao.save(order);
        PurchaseOrderLine mouseLine = saveOrderLine(order, mouse, 10, "88.00");
        saveOrderLine(order, cable, 30, "25.50");

        PurchaseOrder reloaded = purchaseOrderDao.findById(order.getId()).orElseThrow();
        assertEquals("PO-20260301-001", reloaded.getOrderNumber());
        assertEquals(supplier.getId(), reloaded.getSupplierId());
        assertEquals(PurchaseOrderStatus.DRAFT, reloaded.getStatus());
        assertEquals("admin", reloaded.getCreatedBy());
        assertEquals(expectedDate, reloaded.getExpectedDate());
        assertEquals("Replenish electronics", reloaded.getNote());
        assertNull("a draft order has no received timestamp", reloaded.getReceivedAt());

        List<PurchaseOrderLine> lines = purchaseOrderLineDao.findByOrderId(order.getId());
        assertEquals(2, lines.size());
        assertEquals(2, purchaseOrderLineDao.countByOrderId(order.getId()));
        PurchaseOrderLine reloadedMouseLine = lines.stream()
                .filter(line -> mouse.getId().equals(line.getProductId()))
                .findFirst().orElseThrow();
        assertEquals(mouseLine.getId(), reloadedMouseLine.getId());
        assertEquals(10, reloadedMouseLine.getQuantity());
        assertEquals(0, reloadedMouseLine.getReceivedQuantity());
        assertMoneyEquals("88.00", reloadedMouseLine.getUnitCost());

        reloaded.setStatus(PurchaseOrderStatus.RECEIVED);
        reloaded.setReceivedAt(LocalDateTime.of(2026, 4, 1, 8, 15));
        purchaseOrderDao.save(reloaded);

        PurchaseOrder updated = purchaseOrderDao.findById(order.getId()).orElseThrow();
        assertEquals(PurchaseOrderStatus.RECEIVED, updated.getStatus());
        assertNotNull(updated.getReceivedAt());
        assertEquals(LocalDateTime.of(2026, 4, 1, 8, 15), updated.getReceivedAt());
        assertEquals(1, purchaseOrderDao.count());
    }

    @Test
    public void nextOrderNumberStartsAtOneAndIncrements() {
        Supplier supplier = createSupplier("Shenzhen Electronics");
        String prefix = "PO-20260301-";

        String first = purchaseOrderDao.nextOrderNumber(prefix);
        assertEquals(prefix + "001", first);
        purchaseOrderDao.save(createOrder(supplier, first, PurchaseOrderStatus.DRAFT));

        String second = purchaseOrderDao.nextOrderNumber(prefix);
        assertEquals(prefix + "002", second);
        purchaseOrderDao.save(createOrder(supplier, second, PurchaseOrderStatus.ORDERED));

        assertEquals(prefix + "003", purchaseOrderDao.nextOrderNumber(prefix));
        assertEquals("a different day prefix starts its own sequence",
                "PO-20260302-001", purchaseOrderDao.nextOrderNumber("PO-20260302-"));
    }

    @Test
    public void findByStatusReturnsOnlyMatchingOrders() {
        Supplier supplier = createSupplier("Shenzhen Electronics");
        createOrder(supplier, "PO-20260301-001", PurchaseOrderStatus.DRAFT);
        createOrder(supplier, "PO-20260301-002", PurchaseOrderStatus.ORDERED);
        createOrder(supplier, "PO-20260301-003", PurchaseOrderStatus.ORDERED);
        createOrder(supplier, "PO-20260301-004", PurchaseOrderStatus.CANCELLED);

        List<PurchaseOrder> ordered = purchaseOrderDao.findByStatus(PurchaseOrderStatus.ORDERED);
        assertEquals(2, ordered.size());
        assertTrue(ordered.stream()
                .allMatch(order -> order.getStatus() == PurchaseOrderStatus.ORDERED));
        assertEquals(1, purchaseOrderDao.findByStatus(PurchaseOrderStatus.DRAFT).size());
        assertTrue(purchaseOrderDao.findByStatus(PurchaseOrderStatus.RECEIVED).isEmpty());
    }

    @Test
    public void deleteByOrderIdRemovesOnlyTheLinesOfThatOrder() {
        Supplier supplier = createSupplier("Shenzhen Electronics");
        Product mouse = createProduct("ELE-001", "Wireless Mouse", null, supplier,
                "89.00", 42, 10);
        Product cable = createProduct("ELE-002", "USB-C Cable", null, supplier,
                "25.50", 6, 15);
        PurchaseOrder firstOrder = createOrder(supplier, "PO-20260301-001",
                PurchaseOrderStatus.DRAFT);
        PurchaseOrder secondOrder = createOrder(supplier, "PO-20260301-002",
                PurchaseOrderStatus.ORDERED);
        saveOrderLine(firstOrder, mouse, 10, "89.00");
        saveOrderLine(firstOrder, cable, 20, "25.50");
        saveOrderLine(secondOrder, mouse, 5, "89.00");
        assertEquals(3, purchaseOrderLineDao.count());

        purchaseOrderLineDao.deleteByOrderId(firstOrder.getId());

        assertEquals(0, purchaseOrderLineDao.countByOrderId(firstOrder.getId()));
        assertEquals(1, purchaseOrderLineDao.countByOrderId(secondOrder.getId()));
        assertEquals(1, purchaseOrderLineDao.count());
        assertTrue("deleting lines must not delete the order header",
                purchaseOrderDao.existsById(firstOrder.getId()));
        assertTrue(purchaseOrderLineDao.findByOrderId(firstOrder.getId()).isEmpty());
    }
}
