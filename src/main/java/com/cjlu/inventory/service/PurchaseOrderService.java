package com.cjlu.inventory.service;

import com.cjlu.inventory.dao.ProductDao;
import com.cjlu.inventory.dao.PurchaseOrderDao;
import com.cjlu.inventory.dao.PurchaseOrderLineDao;
import com.cjlu.inventory.dao.SupplierDao;
import com.cjlu.inventory.dao.TransactionRunner;
import com.cjlu.inventory.exception.AuthenticationException;
import com.cjlu.inventory.exception.EntityNotFoundException;
import com.cjlu.inventory.exception.InventoryException;
import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.PurchaseOrder;
import com.cjlu.inventory.model.PurchaseOrderLine;
import com.cjlu.inventory.model.PurchaseOrderStatus;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.model.User;
import com.cjlu.inventory.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Purchase-order use cases: the replenishment workflow added in Project 2.
 *
 * <p>Workflow: a draft is created manually or generated from the low-stock
 * products of one supplier, an administrator submits it ({@code ORDERED}), and
 * deliveries are received against it in one or more steps. Receiving updates
 * the purchase order, its lines, the product quantities and the stock
 * transaction log in a single database transaction.</p>
 */
public class PurchaseOrderService {

    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int DEFAULT_LEAD_TIME_DAYS = 7;

    private final PurchaseOrderDao purchaseOrderDao;
    private final PurchaseOrderLineDao purchaseOrderLineDao;
    private final ProductDao productDao;
    private final SupplierDao supplierDao;
    private final StockService stockService;
    private final AuthService authService;
    private final TransactionRunner transactionRunner;

    public PurchaseOrderService(PurchaseOrderDao purchaseOrderDao,
                                PurchaseOrderLineDao purchaseOrderLineDao,
                                ProductDao productDao,
                                SupplierDao supplierDao,
                                StockService stockService,
                                AuthService authService,
                                TransactionRunner transactionRunner) {
        this.purchaseOrderDao = purchaseOrderDao;
        this.purchaseOrderLineDao = purchaseOrderLineDao;
        this.productDao = productDao;
        this.supplierDao = supplierDao;
        this.stockService = stockService;
        this.authService = authService;
        this.transactionRunner = transactionRunner;
    }

    /** One requested product line while a draft is being created. */
    public record OrderLineDraft(String productId, int quantity, BigDecimal unitCost) {
    }

    /** All purchase orders, newest first, with their lines loaded. */
    public List<PurchaseOrder> listOrders() {
        return attachLines(purchaseOrderDao.findAll());
    }

    /** Purchase orders in one status, newest first. */
    public List<PurchaseOrder> listOrders(PurchaseOrderStatus status) {
        return attachLines(purchaseOrderDao.findByStatus(status));
    }

    /** Orders that still expect stock (draft, ordered or partially received). */
    public List<PurchaseOrder> openOrders() {
        List<PurchaseOrder> open = new ArrayList<>();
        open.addAll(purchaseOrderDao.findByStatus(PurchaseOrderStatus.DRAFT));
        open.addAll(purchaseOrderDao.findByStatus(PurchaseOrderStatus.ORDERED));
        open.addAll(purchaseOrderDao.findByStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED));
        return attachLines(open);
    }

    public int openOrderCount() {
        return openOrders().size();
    }

    /** Loads one order with its lines. */
    public PurchaseOrder requireOrder(String id) throws EntityNotFoundException {
        PurchaseOrder order = purchaseOrderDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + id));
        order.setLines(purchaseOrderLineDao.findByOrderId(order.getId()));
        return order;
    }

    public List<PurchaseOrderLine> linesOf(String orderId) {
        return purchaseOrderLineDao.findByOrderId(orderId);
    }

    /**
     * Creates a draft purchase order. Any signed-in user may prepare a draft;
     * submitting it remains an administrator decision.
     */
    public PurchaseOrder createDraft(String supplierId, LocalDate expectedDate, String note,
                                     List<OrderLineDraft> drafts)
            throws ValidationException, EntityNotFoundException, AuthenticationException {
        User user = authService.requireCurrentUser();
        Supplier supplier = supplierDao.findById(supplierId)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + supplierId));
        if (drafts == null || drafts.isEmpty()) {
            throw new ValidationException("A purchase order must contain at least one product line.");
        }
        List<ValidatedLine> validated = validateDrafts(drafts);
        String normalisedNote = Validator.optionalText(note, "Note", Validator.MAX_NOTE_LENGTH);
        String prefix = "PO-" + LocalDate.now().format(ORDER_DATE) + "-";
        PurchaseOrder order = new PurchaseOrder(purchaseOrderDao.nextOrderNumber(prefix),
                supplier.getId(), user.getUsername(), expectedDate, normalisedNote);

        List<PurchaseOrderLine> lines = new ArrayList<>();
        transactionRunner.runInTransaction(() -> {
            purchaseOrderDao.save(order);
            for (ValidatedLine line : validated) {
                PurchaseOrderLine entity = new PurchaseOrderLine(order.getId(),
                        line.product().getId(), line.quantity(), line.unitCost());
                purchaseOrderLineDao.save(entity);
                lines.add(entity);
            }
        });
        order.setLines(lines);
        return order;
    }

    /**
     * Creates a draft for every low-stock product of one supplier, ordering
     * enough to reach twice the reorder level (at least one unit).
     */
    public PurchaseOrder createReplenishmentDraft(String supplierId)
            throws ValidationException, EntityNotFoundException, AuthenticationException {
        Supplier supplier = supplierDao.findById(supplierId)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + supplierId));
        List<Product> lowStock = productDao.findBySupplierId(supplierId).stream()
                .filter(Product::isLowStock)
                .toList();
        if (lowStock.isEmpty()) {
            throw new ValidationException("No low-stock products are linked to supplier '"
                    + supplier.getName() + "'.");
        }
        List<OrderLineDraft> drafts = new ArrayList<>();
        for (Product product : lowStock) {
            int target = Math.max(product.getReorderLevel() * 2, product.getQuantity() + 1);
            int quantity = Math.max(target - product.getQuantity(), 1);
            drafts.add(new OrderLineDraft(product.getId(), quantity, product.getUnitPrice()));
        }
        return createDraft(supplierId, LocalDate.now().plusDays(DEFAULT_LEAD_TIME_DAYS),
                "Auto-generated from low-stock products", drafts);
    }

    /** Submits a draft to the supplier (administrator only). */
    public void submit(String orderId)
            throws EntityNotFoundException, ValidationException, AuthenticationException {
        authService.requireAdmin();
        PurchaseOrder order = requireOrder(orderId);
        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new ValidationException("Only draft purchase orders can be submitted.");
        }
        if (order.getLines().isEmpty()) {
            throw new ValidationException("A purchase order must contain at least one product line.");
        }
        order.setStatus(PurchaseOrderStatus.ORDERED);
        purchaseOrderDao.save(order);
    }

    /** Cancels a draft or ordered purchase order (administrator only). */
    public void cancel(String orderId)
            throws EntityNotFoundException, ValidationException, AuthenticationException {
        authService.requireAdmin();
        PurchaseOrder order = requireOrder(orderId);
        if (order.getStatus() != PurchaseOrderStatus.DRAFT
                && order.getStatus() != PurchaseOrderStatus.ORDERED) {
            throw new ValidationException("Only draft or ordered purchase orders can be cancelled.");
        }
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        purchaseOrderDao.save(order);
    }

    /** Receives every outstanding quantity in one delivery. */
    public void receiveRemaining(String orderId)
            throws EntityNotFoundException, ValidationException, InventoryException,
            AuthenticationException {
        PurchaseOrder order = requireOrder(orderId);
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (PurchaseOrderLine line : order.getLines()) {
            quantities.put(line.getId(), line.getRemainingQuantity());
        }
        receive(orderId, quantities);
    }

    /**
     * Receives a (possibly partial) delivery: each line may receive a quantity
     * between 0 and its remaining quantity. Receipts create stock-in
     * transactions and update the order status atomically.
     */
    public void receive(String orderId, Map<String, Integer> quantitiesByLineId)
            throws EntityNotFoundException, ValidationException, InventoryException,
            AuthenticationException {
        authService.requireCurrentUser();
        PurchaseOrder order = requireOrder(orderId);
        if (order.getStatus() != PurchaseOrderStatus.ORDERED
                && order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new ValidationException("Only ordered or partially received purchase orders can be received.");
        }
        Map<String, Integer> requested = quantitiesByLineId == null
                ? Map.of() : quantitiesByLineId;
        boolean anyReceived = false;
        for (PurchaseOrderLine line : order.getLines()) {
            int quantity = requested.getOrDefault(line.getId(), 0);
            if (quantity < 0) {
                throw new ValidationException("Received quantities must be zero or positive.");
            }
            if (quantity > line.getRemainingQuantity()) {
                throw new ValidationException("Cannot receive " + quantity
                        + " unit(s): only " + line.getRemainingQuantity() + " remain on this line.");
            }
            if (quantity > 0) {
                anyReceived = true;
            }
        }
        if (!anyReceived) {
            throw new ValidationException("Enter a received quantity for at least one line.");
        }

        try {
            transactionRunner.runInTransaction(() -> {
                for (PurchaseOrderLine line : order.getLines()) {
                    int quantity = requested.getOrDefault(line.getId(), 0);
                    if (quantity == 0) {
                        continue;
                    }
                    try {
                        stockService.stockIn(line.getProductId(), quantity,
                                "Received " + order.getOrderNumber());
                    } catch (InventoryException e) {
                        throw new StockChangeFailure(e);
                    }
                    line.addReceivedQuantity(quantity);
                    purchaseOrderLineDao.save(line);
                }
                boolean complete = order.getLines().stream()
                        .allMatch(line -> line.getRemainingQuantity() == 0);
                order.setStatus(complete
                        ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED);
                if (complete) {
                    order.setReceivedAt(LocalDateTime.now());
                }
                purchaseOrderDao.save(order);
            });
        } catch (StockChangeFailure failure) {
            throw failure.inventoryException;
        }
    }

    private List<PurchaseOrder> attachLines(List<PurchaseOrder> orders) {
        for (PurchaseOrder order : orders) {
            order.setLines(purchaseOrderLineDao.findByOrderId(order.getId()));
        }
        return orders;
    }

    private List<ValidatedLine> validateDrafts(List<OrderLineDraft> drafts)
            throws ValidationException, EntityNotFoundException {
        List<ValidatedLine> validated = new ArrayList<>();
        for (OrderLineDraft draft : drafts) {
            if (draft == null) {
                throw new ValidationException("A purchase order line is missing.");
            }
            Product product = productDao.findById(draft.productId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Product not found: " + draft.productId()));
            if (draft.quantity() <= 0) {
                throw new ValidationException("Order quantity for " + product.getSku()
                        + " must be a positive whole number.");
            }
            BigDecimal unitCost = draft.unitCost() == null
                    ? product.getUnitPrice() : draft.unitCost();
            if (unitCost.signum() < 0) {
                throw new ValidationException("Unit cost for " + product.getSku()
                        + " must be zero or positive.");
            }
            validated.add(new ValidatedLine(product, draft.quantity(), unitCost));
        }
        return validated;
    }

    private record ValidatedLine(Product product, int quantity, BigDecimal unitCost) {
    }

    /** Carries a checked business failure out of a transaction lambda. */
    private static final class StockChangeFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final transient InventoryException inventoryException;

        private StockChangeFailure(InventoryException inventoryException) {
            super(inventoryException.getMessage(), inventoryException);
            this.inventoryException = inventoryException;
        }
    }
}
