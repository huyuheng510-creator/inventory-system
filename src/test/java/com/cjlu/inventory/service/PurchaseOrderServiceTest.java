package com.cjlu.inventory.service;

import com.cjlu.inventory.exception.AuthenticationException;
import com.cjlu.inventory.exception.EntityNotFoundException;
import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.PurchaseOrder;
import com.cjlu.inventory.model.PurchaseOrderLine;
import com.cjlu.inventory.model.PurchaseOrderStatus;
import com.cjlu.inventory.model.StockTransaction;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.model.TransactionType;
import com.cjlu.inventory.service.PurchaseOrderService.OrderLineDraft;
import com.cjlu.inventory.support.AbstractDatabaseTest;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Workflow tests for {@link PurchaseOrderService}: creating a draft with
 * validation, the administrator-only submit/cancel decisions, partial
 * receiving into stock, and receiving the remaining quantities atomically
 * (order status, product quantities and the stock transaction log must all
 * agree).
 */
public class PurchaseOrderServiceTest extends AbstractDatabaseTest {

    private Supplier supplier;
    private Product mouse;
    private Product cable;

    /** Common catalogue: two products linked to one supplier. */
    private void givenCatalogue() {
        supplier = createSupplier("Shenzhen Electronics");
        mouse = createProduct("ELE-001", "Wireless Mouse", null, supplier, "5.00", 10, 10);
        cable = createProduct("ELE-002", "USB-C Cable", null, supplier, "3.00", 2, 2);
    }

    /** Creates a draft with one mouse line and one cable line as administrator. */
    private PurchaseOrder givenDraft(int mouseQuantity, int cableQuantity) throws Exception {
        loginAsAdmin();
        return purchaseOrderService.createDraft(supplier.getId(),
                LocalDate.now().plusDays(7), "Replenish low stock",
                List.of(new OrderLineDraft(mouse.getId(), mouseQuantity, null),
                        new OrderLineDraft(cable.getId(), cableQuantity, null)));
    }

    /** Finds the line of one order that belongs to the given product. */
    private PurchaseOrderLine lineFor(List<PurchaseOrderLine> lines, Product product) {
        return lines.stream()
                .filter(line -> product.getId().equals(line.getProductId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No order line for " + product.getSku()));
    }

    private int quantityOf(Product product) {
        return productDao.findById(product.getId()).orElseThrow().getQuantity();
    }

    @Test
    public void createDraftPersistsDraftWithLines() throws Exception {
        givenCatalogue();
        loginAsStaff();

        PurchaseOrder draft = purchaseOrderService.createDraft(supplier.getId(),
                LocalDate.now().plusDays(7), "Replenish low stock",
                List.of(new OrderLineDraft(mouse.getId(), 10, null),
                        new OrderLineDraft(cable.getId(), 5, new BigDecimal("2.00"))));

        assertTrue("order numbers follow PO-yyyyMMdd-NNN, was " + draft.getOrderNumber(),
                draft.getOrderNumber().matches("PO-\\d{8}-\\d{3}"));
        assertEquals(PurchaseOrderStatus.DRAFT, draft.getStatus());
        assertEquals("staff", draft.getCreatedBy());
        assertEquals(LocalDate.now().plusDays(7), draft.getExpectedDate());
        assertEquals("Replenish low stock", draft.getNote());
        assertEquals(2, draft.getLines().size());

        PurchaseOrder stored = purchaseOrderService.requireOrder(draft.getId());
        assertEquals(PurchaseOrderStatus.DRAFT, stored.getStatus());
        assertNull("a draft has no received timestamp", stored.getReceivedAt());
        List<PurchaseOrderLine> lines = stored.getLines();
        assertEquals(2, lines.size());
        assertEquals(10, lineFor(lines, mouse).getQuantity());
        assertEquals(5, lineFor(lines, cable).getQuantity());
        assertMoneyEquals("5.00", lineFor(lines, mouse).getUnitCost());
        assertMoneyEquals("2.00", lineFor(lines, cable).getUnitCost());
        assertEquals(1, purchaseOrderDao.count());
        assertEquals(2, purchaseOrderLineDao.count());
    }

    @Test
    public void createDraftValidatesItsInput() throws Exception {
        givenCatalogue();
        loginAsStaff();
        BigDecimal validCost = new BigDecimal("5.00");

        assertThrows("a purchase order needs at least one line",
                ValidationException.class,
                () -> purchaseOrderService.createDraft(supplier.getId(), null, null, List.of()));
        assertThrows("order quantities must be positive",
                ValidationException.class,
                () -> purchaseOrderService.createDraft(supplier.getId(), null, null,
                        List.of(new OrderLineDraft(mouse.getId(), 0, validCost))));
        assertThrows(ValidationException.class,
                () -> purchaseOrderService.createDraft(supplier.getId(), null, null,
                        List.of(new OrderLineDraft(mouse.getId(), -3, validCost))));
        assertThrows("unit costs must not be negative",
                ValidationException.class,
                () -> purchaseOrderService.createDraft(supplier.getId(), null, null,
                        List.of(new OrderLineDraft(mouse.getId(), 1, new BigDecimal("-1.00")))));
        assertThrows("the supplier must exist",
                EntityNotFoundException.class,
                () -> purchaseOrderService.createDraft("no-such-supplier", null, null,
                        List.of(new OrderLineDraft(mouse.getId(), 1, validCost))));

        assertEquals("invalid drafts must not be persisted", 0, purchaseOrderDao.count());
        assertEquals(0, purchaseOrderLineDao.count());
    }

    @Test
    public void administratorSubmitsDraftAndCancelsOrderedOrder() throws Exception {
        givenCatalogue();
        PurchaseOrder draft = givenDraft(10, 4);

        purchaseOrderService.submit(draft.getId());
        PurchaseOrder ordered = purchaseOrderService.requireOrder(draft.getId());
        assertEquals("submitting a draft moves it to ORDERED",
                PurchaseOrderStatus.ORDERED, ordered.getStatus());
        assertThrows("an already submitted order cannot be submitted again",
                ValidationException.class, () -> purchaseOrderService.submit(draft.getId()));

        purchaseOrderService.cancel(draft.getId());
        PurchaseOrder cancelled = purchaseOrderService.requireOrder(draft.getId());
        assertEquals(PurchaseOrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals("cancelled orders are no longer open",
                0, purchaseOrderService.openOrderCount());
        assertThrows("a cancelled order cannot be cancelled again",
                ValidationException.class, () -> purchaseOrderService.cancel(draft.getId()));
    }

    @Test
    public void staffCannotSubmitOrCancelOrders() throws Exception {
        givenCatalogue();
        loginAsStaff();
        PurchaseOrder draft = purchaseOrderService.createDraft(supplier.getId(), null, null,
                List.of(new OrderLineDraft(mouse.getId(), 10, null)));

        assertThrows("warehouse staff must not submit orders",
                AuthenticationException.class,
                () -> purchaseOrderService.submit(draft.getId()));
        assertEquals("the rejected submit must not change the status",
                PurchaseOrderStatus.DRAFT,
                purchaseOrderService.requireOrder(draft.getId()).getStatus());

        loginAsAdmin();
        purchaseOrderService.submit(draft.getId());
        loginAsStaff();
        assertThrows("warehouse staff must not cancel orders",
                AuthenticationException.class,
                () -> purchaseOrderService.cancel(draft.getId()));
        assertEquals("the rejected cancel must not change the status",
                PurchaseOrderStatus.ORDERED,
                purchaseOrderService.requireOrder(draft.getId()).getStatus());
    }

    @Test
    public void partialReceiptUpdatesOrderStockAndTransactionLog() throws Exception {
        givenCatalogue();
        PurchaseOrder draft = givenDraft(10, 4);
        purchaseOrderService.submit(draft.getId());
        PurchaseOrderLine mouseLine = lineFor(purchaseOrderService.linesOf(draft.getId()), mouse);

        purchaseOrderService.receive(draft.getId(), Map.of(mouseLine.getId(), 4));

        PurchaseOrder partiallyReceived = purchaseOrderService.requireOrder(draft.getId());
        assertEquals("receiving only some quantities keeps the order open",
                PurchaseOrderStatus.PARTIALLY_RECEIVED, partiallyReceived.getStatus());
        assertNull("the order is not fully received yet, so receivedAt is still empty",
                partiallyReceived.getReceivedAt());
        assertEquals(4, partiallyReceived.getTotalReceivedQuantity());

        List<PurchaseOrderLine> lines = purchaseOrderService.linesOf(draft.getId());
        assertEquals(4, lineFor(lines, mouse).getReceivedQuantity());
        assertEquals(6, lineFor(lines, mouse).getRemainingQuantity());
        assertEquals(0, lineFor(lines, cable).getReceivedQuantity());

        assertEquals("received units are added to the on-hand stock",
                14, quantityOf(mouse));
        assertEquals("products not received keep their quantity", 2, quantityOf(cable));

        List<StockTransaction> transactions = transactionDao.findByProductId(mouse.getId());
        assertEquals(1, transactions.size());
        assertEquals(TransactionType.STOCK_IN, transactions.get(0).getType());
        assertEquals(4, transactions.get(0).getQuantity());
        assertTrue("the stock transaction references the purchase order: "
                        + transactions.get(0).getNote(),
                transactions.get(0).getNote().contains(draft.getOrderNumber()));
        assertEquals(1, transactionDao.count());
    }

    @Test
    public void receiveRemainingMarksOrderReceivedAndRefusesFurtherReceipts() throws Exception {
        givenCatalogue();
        PurchaseOrder draft = givenDraft(10, 4);
        purchaseOrderService.submit(draft.getId());

        purchaseOrderService.receiveRemaining(draft.getId());

        PurchaseOrder received = purchaseOrderService.requireOrder(draft.getId());
        assertEquals(PurchaseOrderStatus.RECEIVED, received.getStatus());
        assertNotNull("a fully received order records when it was received",
                received.getReceivedAt());
        assertFalse("a received order is final", received.getStatus().isOpen());
        assertEquals(14, received.getTotalReceivedQuantity());
        assertTrue("every line is fully received",
                received.getLines().stream().allMatch(line -> line.getRemainingQuantity() == 0));

        assertEquals("all received quantities are added to stock", 20, quantityOf(mouse));
        assertEquals(6, quantityOf(cable));

        List<StockTransaction> mouseTransactions = transactionDao.findByProductId(mouse.getId());
        List<StockTransaction> cableTransactions = transactionDao.findByProductId(cable.getId());
        assertEquals(1, mouseTransactions.size());
        assertEquals(10, mouseTransactions.get(0).getQuantity());
        assertEquals(4, cableTransactions.get(0).getQuantity());
        assertEquals(2, transactionDao.count());
        assertEquals("no orders are open after the delivery",
                0, purchaseOrderService.openOrderCount());

        assertThrows("a fully received order cannot receive more stock",
                ValidationException.class,
                () -> purchaseOrderService.receiveRemaining(draft.getId()));
        PurchaseOrderLine mouseLine = lineFor(purchaseOrderService.linesOf(draft.getId()), mouse);
        assertThrows(ValidationException.class,
                () -> purchaseOrderService.receive(draft.getId(), Map.of(mouseLine.getId(), 1)));
        assertEquals("the refused receipts must not create transactions",
                2, transactionDao.count());
        assertEquals("the refused receipts must not change stock", 20, quantityOf(mouse));
    }
}
