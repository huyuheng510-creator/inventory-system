package com.cjlu.inventory.service;

import com.cjlu.inventory.exception.EntityNotFoundException;
import com.cjlu.inventory.exception.InventoryException;
import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockStatus;
import com.cjlu.inventory.model.StockTransaction;
import com.cjlu.inventory.model.User;
import com.cjlu.inventory.dao.ProductDao;
import com.cjlu.inventory.dao.TransactionDao;
import com.cjlu.inventory.service.observer.StockLevelObserver;
import com.cjlu.inventory.util.Validator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stock movement use cases (FR-6, FR-7, FR-8, FR-9, FR-10).
 *
 * <p>Every change follows the same path: resolve the product, validate the
 * request, let {@link TransactionFactory} build the auditable transaction, then
 * {@link #applyChange(Product, int, StockTransaction)} writes the new quantity,
 * persists both files and notifies the registered observers. The Observer
 * pattern keeps low-stock alerting out of the stock rules.</p>
 */
public class StockService {

    private final ProductDao ProductDao;
    private final TransactionDao TransactionDao;
    private final AuthService authService;
    private final List<StockLevelObserver> observers = new ArrayList<>();

    public StockService(ProductDao ProductDao,
                        TransactionDao TransactionDao,
                        AuthService authService) {
        this.ProductDao = ProductDao;
        this.TransactionDao = TransactionDao;
        this.authService = authService;
    }

    /** Registers an observer that is notified after every stock change. */
    public void addObserver(StockLevelObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    /** Records a stock-in movement (FR-6). */
    public StockTransaction stockIn(String productIdOrSku, int quantity, String note)
            throws InventoryException {
        User operator = authService.requireCurrentUser();
        Product product = resolveProduct(productIdOrSku);
        String normalisedNote = Validator.optionalNote(note);
        StockTransaction transaction = TransactionFactory.stockIn(product, quantity,
                operator.getUsername(), normalisedNote);
        applyChange(product, product.getQuantity() + quantity, transaction);
        return transaction;
    }

    /** Records a stock-out movement after checking availability (FR-7). */
    public StockTransaction stockOut(String productIdOrSku, int quantity, String note)
            throws InventoryException {
        User operator = authService.requireCurrentUser();
        Product product = resolveProduct(productIdOrSku);
        String normalisedNote = Validator.optionalNote(note);
        if (quantity > product.getQuantity()) {
            throw new ValidationException("Insufficient stock: only " + product.getQuantity()
                    + " " + product.getUnit() + " of " + product.getSku() + " available.");
        }
        StockTransaction transaction = TransactionFactory.stockOut(product, quantity,
                operator.getUsername(), normalisedNote);
        applyChange(product, product.getQuantity() - quantity, transaction);
        return transaction;
    }

    /** Sets the physically counted quantity and stores the signed difference (FR-8). */
    public StockTransaction adjustStock(String productIdOrSku, int countedQuantity, String reason)
            throws InventoryException {
        authService.requireAdmin();
        User operator = authService.requireCurrentUser();
        Product product = resolveProduct(productIdOrSku);
        Validator.requireNonNegativeInt(countedQuantity, "Counted quantity");
        String normalisedReason = Validator.requireText(reason, "Reason", Validator.MAX_NOTE_LENGTH);
        int delta = countedQuantity - product.getQuantity();
        StockTransaction transaction = TransactionFactory.adjustment(product, delta,
                countedQuantity, operator.getUsername(), normalisedReason);
        applyChange(product, countedQuantity, transaction);
        return transaction;
    }

    /** Full audit trail for one product, oldest first. */
    public List<StockTransaction> historyForProduct(String productIdOrSku)
            throws EntityNotFoundException {
        Product product = resolveProduct(productIdOrSku);
        return TransactionDao.findByProductId(product.getId());
    }

    /** Audit trail inside a half-open date range. */
    public List<StockTransaction> historyBetween(LocalDateTime fromInclusive, LocalDateTime toExclusive)
            throws ValidationException {
        if (fromInclusive == null || toExclusive == null || !fromInclusive.isBefore(toExclusive)) {
            throw new ValidationException("The start of the date range must be before the end.");
        }
        return TransactionDao.findBetween(fromInclusive, toExclusive);
    }

    /** Products at or below their reorder level (FR-9). */
    public List<Product> lowStockProducts() {
        return ProductDao.findLowStock();
    }

    /**
     * Writes the new quantity and the transaction, then notifies all observers.
     * This is the single place that knows how a stock change is committed.
     */
    private void applyChange(Product product, int newQuantity, StockTransaction transaction)
            throws ValidationException {
        StockStatus previous = product.getStockStatus();
        product.setQuantity(newQuantity);
        TransactionDao.save(transaction);
        ProductDao.save(product);
        StockStatus current = product.getStockStatus();
        for (StockLevelObserver observer : observers) {
            observer.onStockLevelChanged(product, previous, current);
        }
    }

    private Product resolveProduct(String productIdOrSku) throws EntityNotFoundException {
        if (productIdOrSku == null || productIdOrSku.isBlank()) {
            throw new EntityNotFoundException("Product not found: no SKU or id supplied.");
        }
        String key = productIdOrSku.trim();
        Optional<Product> byId = ProductDao.findById(key);
        if (byId.isPresent()) {
            return byId.get();
        }
        Optional<Product> bySku = ProductDao.findBySku(key);
        if (bySku.isPresent()) {
            return bySku.get();
        }
        throw new EntityNotFoundException("Product not found: " + key);
    }
}
