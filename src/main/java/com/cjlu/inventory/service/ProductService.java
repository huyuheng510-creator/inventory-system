package com.cjlu.inventory.service;

import com.cjlu.inventory.exception.AuthenticationException;
import com.cjlu.inventory.exception.EntityNotFoundException;
import com.cjlu.inventory.exception.InventoryException;
import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockTransaction;
import com.cjlu.inventory.dao.CategoryDao;
import com.cjlu.inventory.dao.ProductDao;
import com.cjlu.inventory.dao.SupplierDao;
import com.cjlu.inventory.dao.TransactionDao;
import com.cjlu.inventory.util.Validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Product catalogue use cases (FR-2, FR-3): create, update, delete, list,
 * search, filter, sort and low-stock queries.
 *
 * <p>Mutating operations re-check administrator privileges even though the
 * console hides the corresponding menu items, so the business rules cannot be
 * bypassed by another client of the service layer.</p>
 */
public class ProductService {

    private final ProductDao ProductDao;
    private final CategoryDao CategoryDao;
    private final SupplierDao SupplierDao;
    private final TransactionDao TransactionDao;
    private final AuthService authService;

    public ProductService(ProductDao ProductDao,
                          CategoryDao CategoryDao,
                          SupplierDao SupplierDao,
                          TransactionDao TransactionDao,
                          AuthService authService) {
        this.ProductDao = ProductDao;
        this.CategoryDao = CategoryDao;
        this.SupplierDao = SupplierDao;
        this.TransactionDao = TransactionDao;
        this.authService = authService;
    }

    /** Creates a product after validating every field and both references (FR-2). */
    public Product createProduct(String sku, String name, String categoryId, String supplierId,
                                 BigDecimal unitPrice, int quantity, int reorderLevel, String unit)
            throws ValidationException, AuthenticationException {
        authService.requireAdmin();
        String normalisedSku = Validator.requireSku(sku);
        if (ProductDao.existsBySku(normalisedSku)) {
            throw new ValidationException("SKU '" + normalisedSku + "' already exists. SKUs must be unique.");
        }
        String normalisedName = Validator.requireText(name, "Product name", 60);
        requireReferencesExist(categoryId, supplierId);
        Validator.requireNonNegativeDecimal(unitPrice, "Unit price");
        Validator.requireNonNegativeInt(quantity, "Quantity");
        Validator.requireNonNegativeInt(reorderLevel, "Reorder level");
        String normalisedUnit = Validator.requireText(unit, "Unit", 20);
        Product product = new Product(normalisedSku, normalisedName, categoryId, supplierId,
                unitPrice, quantity, reorderLevel, normalisedUnit);
        ProductDao.save(product);
        return product;
    }

    /** Updates the editable master data of a product; the quantity only changes through stock movements. */
    public void updateProduct(String id, String name, String categoryId, String supplierId,
                              BigDecimal unitPrice, int reorderLevel, String unit)
            throws EntityNotFoundException, ValidationException, AuthenticationException {
        authService.requireAdmin();
        Product product = requireProduct(id);
        String normalisedName = Validator.requireText(name, "Product name", 60);
        requireReferencesExist(categoryId, supplierId);
        Validator.requireNonNegativeDecimal(unitPrice, "Unit price");
        Validator.requireNonNegativeInt(reorderLevel, "Reorder level");
        String normalisedUnit = Validator.requireText(unit, "Unit", 20);
        product.setName(normalisedName);
        product.setCategoryId(categoryId);
        product.setSupplierId(supplierId);
        product.setUnitPrice(unitPrice);
        product.setReorderLevel(reorderLevel);
        product.setUnit(normalisedUnit);
        ProductDao.save(product);
    }

    /** Deletes a product, refusing when the audit log still references it. */
    public void deleteProduct(String id) throws EntityNotFoundException, InventoryException,
            AuthenticationException {
        authService.requireAdmin();
        Product product = requireProduct(id);
        List<StockTransaction> history = TransactionDao.findByProductId(product.getId());
        if (!history.isEmpty()) {
            throw new InventoryException("Cannot delete " + product.getSku()
                    + ": " + history.size() + " transaction(s) exist in the audit log.");
        }
        ProductDao.deleteById(product.getId());
    }

    public List<Product> listProducts() {
        return ProductDao.findAll();
    }

    public List<Product> search(String keyword) {
        return ProductDao.search(keyword);
    }

    /**
     * Returns the products of one category (FR-3: filter by category).
     *
     * <p>The category may be given either as its internal id or as its name
     * (case-insensitive). The result is sorted by product name so the console
     * output and any future GUI table are stable.</p>
     *
     * @throws EntityNotFoundException when no category matches the supplied id or name
     */
    public List<Product> filterByCategory(String categoryIdOrName) throws EntityNotFoundException {
        if (categoryIdOrName == null || categoryIdOrName.isBlank()) {
            throw new EntityNotFoundException(
                    "Category not found: no category id or name was supplied.");
        }
        String key = categoryIdOrName.trim();
        Category category = CategoryDao.findById(key)
                .or(() -> CategoryDao.findByName(key))
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + key
                        + ". Use 'Filter by category' to see the available categories."));
        List<Product> products = new ArrayList<>(ProductDao.findByCategoryId(category.getId()));
        products.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Product::getSku));
        return products;
    }

    /** Returns a sorted copy of the catalogue (FR-3). */
    public List<Product> sortBy(SortField field) {
        List<Product> products = new ArrayList<>(ProductDao.findAll());
        Comparator<Product> comparator = switch (field) {
            case NAME -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            case QUANTITY -> Comparator.comparingInt(Product::getQuantity);
            case VALUE -> Comparator.comparing(Product::getInventoryValue);
        };
        products.sort(comparator.thenComparing(Product::getSku));
        return products;
    }

    /** Resolves a product by internal id or by SKU. */
    public Product requireProduct(String idOrSku) throws EntityNotFoundException {
        if (idOrSku == null || idOrSku.isBlank()) {
            throw new EntityNotFoundException("Product not found: no SKU or id supplied.");
        }
        String key = idOrSku.trim();
        Optional<Product> byId = ProductDao.findById(key);
        if (byId.isPresent()) {
            return byId.get();
        }
        Optional<Product> bySku = ProductDao.findBySku(key);
        if (bySku.isPresent()) {
            return bySku.get();
        }
        throw new EntityNotFoundException("Product not found: " + key
                + ". Use 'List All Products' to see the available SKUs.");
    }

    /** Products at or below their reorder level (FR-9). */
    public List<Product> lowStockProducts() {
        return ProductDao.findLowStock();
    }

    private void requireReferencesExist(String categoryId, String supplierId) throws ValidationException {
        if (categoryId == null || categoryId.isBlank()
                || CategoryDao.findById(categoryId).isEmpty()) {
            throw new ValidationException("Category not found: please select an existing category.");
        }
        if (supplierId == null || supplierId.isBlank()
                || SupplierDao.findById(supplierId).isEmpty()) {
            throw new ValidationException("Supplier not found: please select an existing supplier.");
        }
    }
}
