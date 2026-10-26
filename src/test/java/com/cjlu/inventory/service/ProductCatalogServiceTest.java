package com.cjlu.inventory.service;

import com.cjlu.inventory.exception.InventoryException;
import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.model.TransactionType;
import com.cjlu.inventory.support.AbstractDatabaseTest;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Business-rule tests for {@link ProductService} and {@link CatalogService}:
 * creating a product with valid references, rejecting duplicate SKUs, refusing
 * to delete a product that still has audit history and refusing to delete
 * master data that products reference.
 */
public class ProductCatalogServiceTest extends AbstractDatabaseTest {

    @Test
    public void administratorCreatesProductAndReadsItBack() throws Exception {
        loginAsAdmin();
        Category category = catalogService.createCategory("Electronics",
                "Consumer electronics and computer accessories");
        Supplier supplier = catalogService.createSupplier("Shenzhen Electronics",
                "Li Wei", "13800138001", "sales@example.com");

        Product created = productService.createProduct("ELE-100", "Test Widget",
                category.getId(), supplier.getId(), new BigDecimal("12.50"), 7, 3, "piece");

        Product stored = productDao.findBySku("ELE-100").orElseThrow();
        assertEquals(created.getId(), stored.getId());
        assertEquals("Test Widget", stored.getName());
        assertEquals(category.getId(), stored.getCategoryId());
        assertEquals(supplier.getId(), stored.getSupplierId());
        assertMoneyEquals("12.50", stored.getUnitPrice());
        assertEquals(7, stored.getQuantity());
        assertEquals(3, stored.getReorderLevel());
        assertEquals("piece", stored.getUnit());

        assertEquals(1, productService.listProducts().size());
        assertEquals(1, productService.filterByCategory("Electronics").size());
        assertThrows("a product must reference an existing category",
                ValidationException.class,
                () -> productService.createProduct("ELE-101", "Orphan Product",
                        "no-such-category", supplier.getId(), new BigDecimal("1.00"),
                        1, 1, "piece"));
        assertEquals("the rejected product must not be persisted",
                1, productDao.count());
    }

    @Test
    public void createProductRejectsDuplicateSkuCaseInsensitively() throws Exception {
        loginAsAdmin();
        Category category = createCategory("Electronics");
        Supplier supplier = createSupplier("Shenzhen Electronics");
        productService.createProduct("TST-100", "Original Widget", category.getId(),
                supplier.getId(), new BigDecimal("10.00"), 1, 1, "piece");

        ValidationException failure = assertThrows(ValidationException.class,
                () -> productService.createProduct("tst-100", "Duplicate Widget",
                        category.getId(), supplier.getId(), new BigDecimal("10.00"),
                        1, 1, "piece"));

        assertTrue("the error must name the duplicate SKU: " + failure.getMessage(),
                failure.getMessage().contains("TST-100"));
        assertEquals("the duplicate must not be persisted", 1, productDao.count());
    }

    @Test
    public void deleteProductIsRejectedWhenTransactionHistoryExists() throws Exception {
        loginAsAdmin();
        Category category = createCategory("Electronics");
        Supplier supplier = createSupplier("Shenzhen Electronics");
        Product sold = productService.createProduct("TST-101", "Sold Widget",
                category.getId(), supplier.getId(), new BigDecimal("10.00"), 5, 1, "piece");
        recordTransaction(sold, TransactionType.STOCK_IN, 5, LocalDateTime.now());

        assertThrows("a product with audit history must not be deleted",
                InventoryException.class, () -> productService.deleteProduct(sold.getId()));
        assertTrue("the product is still present", productDao.existsById(sold.getId()));

        Product unused = productService.createProduct("TST-102", "Unused Widget",
                category.getId(), supplier.getId(), new BigDecimal("10.00"), 0, 1, "piece");
        productService.deleteProduct(unused.getId());
        assertFalse(productDao.existsById(unused.getId()));
        assertEquals("only the product without history was removed",
                1, productDao.count());
    }

    @Test
    public void deleteReferencedCategoryAndSupplierIsRejected() throws Exception {
        loginAsAdmin();
        Category category = catalogService.createCategory("Electronics", "Consumer electronics");
        Supplier supplier = catalogService.createSupplier("Shenzhen Electronics",
                "Li Wei", "13800138001", "sales@example.com");
        productService.createProduct("TST-103", "Referencing Widget", category.getId(),
                supplier.getId(), new BigDecimal("10.00"), 1, 1, "piece");

        assertThrows("a category that products reference must not be deleted",
                InventoryException.class, () -> catalogService.deleteCategory(category.getId()));
        assertThrows("a supplier that products reference must not be deleted",
                InventoryException.class, () -> catalogService.deleteSupplier(supplier.getId()));
        assertTrue(categoryDao.existsById(category.getId()));
        assertTrue(supplierDao.existsById(supplier.getId()));

        Category unusedCategory = catalogService.createCategory("Unused Category",
                "No products reference this");
        catalogService.deleteCategory(unusedCategory.getId());
        assertFalse("an unreferenced category can be deleted",
                categoryDao.existsById(unusedCategory.getId()));
        List<Category> remaining = catalogService.listCategories();
        assertEquals(1, remaining.size());
        assertEquals("Electronics", remaining.get(0).getName());
    }
}
