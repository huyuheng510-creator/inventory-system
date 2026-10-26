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

import com.cjlu.inventory.util.CsvUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
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

    /**
     * Result of one CSV import row: either a created product or an error message.
     *
     * @param rowNumber  1-based row number in the CSV file (excluding the header)
     * @param sku        the SKU value found in that row (may be blank on parse failure)
     * @param product    the created product, or {@code null} if the row failed
     * @param error      human-readable error description, or {@code null} on success
     */
    public record ImportResult(int rowNumber, String sku, Product product, String error) {
        public boolean isSuccess() { return product != null; }
    }

    /**
     * Bulk-imports products from a UTF-8 CSV file (FR-2 extension).
     *
     * <p>Expected header (case-insensitive): sku, name, category, supplier,
     * unit_price, quantity, reorder_level, unit. Rows with errors are skipped and
     * reported; valid rows are committed one by one so that a bad row never rolls
     * back a good one. Requires administrator privileges.</p>
     *
     * @param csvFile path to the CSV file to import
     * @return per-row results (both successes and failures)
     * @throws AuthenticationException when the signed-in user is not an administrator
     * @throws IOException             when the file cannot be read
     */
    public List<ImportResult> importFromCsv(Path csvFile)
            throws AuthenticationException, IOException {
        authService.requireAdmin();

        List<String[]> rows = CsvUtils.readAll(csvFile);
        if (rows.isEmpty()) {
            return List.of();
        }

        // Skip the header row if the first cell looks like a column name.
        int startRow = 0;
        if (rows.get(0).length > 0 && rows.get(0)[0].trim().equalsIgnoreCase("sku")) {
            startRow = 1;
        }

        List<ImportResult> results = new ArrayList<>();
        for (int index = startRow; index < rows.size(); index++) {
            int rowNumber = index - startRow + 1;
            String[] fields = rows.get(index);
            if (fields.length < 8) {
                results.add(new ImportResult(rowNumber, safeGet(fields, 0),
                        null, "Row has only " + fields.length + " column(s); 8 required."));
                continue;
            }
            String sku = safeGet(fields, 0);
            try {
                BigDecimal unitPrice = new BigDecimal(safeGet(fields, 4).replace(",", ""));
                int quantity = Integer.parseInt(safeGet(fields, 5).replace(",", ""));
                int reorderLevel = Integer.parseInt(safeGet(fields, 6).replace(",", ""));

                // Resolve category and supplier by name (more human-friendly in CSV).
                String categoryId = CategoryDao.findByName(safeGet(fields, 2))
                        .map(com.cjlu.inventory.model.Category::getId)
                        .orElseThrow(() -> new ValidationException(
                                "Category '" + safeGet(fields, 2) + "' not found."));
                String supplierId = SupplierDao.findByName(safeGet(fields, 3))
                        .map(com.cjlu.inventory.model.Supplier::getId)
                        .orElseThrow(() -> new ValidationException(
                                "Supplier '" + safeGet(fields, 3) + "' not found."));

                Product created = createProduct(sku, safeGet(fields, 1),
                        categoryId, supplierId,
                        unitPrice, quantity, reorderLevel, safeGet(fields, 7));
                results.add(new ImportResult(rowNumber, sku, created, null));
            } catch (InventoryException failure) {
                results.add(new ImportResult(rowNumber, sku, null, failure.getMessage()));
            } catch (NumberFormatException failure) {
                results.add(new ImportResult(rowNumber, sku, null,
                        "Invalid number in unit_price, quantity or reorder_level: "
                                + failure.getMessage()));
            }
        }
        return results;
    }

    /** Returns CSV template column headers for user reference. */
    public static String[] csvTemplateHeader() {
        return new String[]{"sku", "name", "category", "supplier",
                "unit_price", "quantity", "reorder_level", "unit"};
    }

    private static String safeGet(String[] fields, int index) {
        return (index < fields.length) ? fields[index].trim() : "";
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
