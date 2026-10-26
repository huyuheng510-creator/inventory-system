package com.cjlu.inventory.dao;

import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.support.AbstractDatabaseTest;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Database-level tests for {@link ProductDao} (the JDBC implementation is
 * exercised against a real embedded Derby database).
 *
 * <p>Covered queries: insert, update, find by id / SKU, exists-by-SKU, the
 * case-insensitive search, the category filter, the low-stock query and
 * delete.</p>
 */
public class ProductDaoTest extends AbstractDatabaseTest {

    @Test
    public void saveAndFindBySkuRoundTripsAllFields() {
        Category electronics = createCategory("Electronics");
        Supplier shenzhen = createSupplier("Shenzhen Electronics");
        Product mouse = new Product("ELE-001", "Wireless Mouse", electronics.getId(),
                shenzhen.getId(), new BigDecimal("89.00"), 42, 10, "piece");

        productDao.save(mouse);

        Optional<Product> bySku = productDao.findBySku("ele-001");
        assertTrue("product must be found by its lower-case SKU", bySku.isPresent());
        Product loaded = bySku.orElseThrow();
        assertEquals(mouse.getId(), loaded.getId());
        assertEquals("ELE-001", loaded.getSku());
        assertEquals("Wireless Mouse", loaded.getName());
        assertEquals(electronics.getId(), loaded.getCategoryId());
        assertEquals(shenzhen.getId(), loaded.getSupplierId());
        assertMoneyEquals("89.00", loaded.getUnitPrice());
        assertEquals(42, loaded.getQuantity());
        assertEquals(10, loaded.getReorderLevel());
        assertEquals("piece", loaded.getUnit());

        assertEquals(loaded, productDao.findById(mouse.getId()).orElseThrow());
        assertTrue(productDao.existsById(mouse.getId()));
        assertTrue(productDao.existsBySku("ELE-001"));
        assertEquals(1, productDao.count());
        assertEquals("findAll must return the single saved product",
                1, productDao.findAll().size());
    }

    @Test
    public void saveUpdatesExistingProduct() throws Exception {
        Category electronics = createCategory("Electronics");
        Supplier shenzhen = createSupplier("Shenzhen Electronics");
        Product product = new Product("ELE-002", "USB-C Cable 2m", electronics.getId(),
                shenzhen.getId(), new BigDecimal("25.50"), 6, 15, "piece");
        productDao.save(product);

        product.setName("USB-C Cable 3m");
        product.setUnitPrice(new BigDecimal("29.90"));
        product.setQuantity(11);
        product.setReorderLevel(20);
        product.setUnit("cable");
        productDao.save(product);

        Product reloaded = productDao.findBySku("ELE-002").orElseThrow();
        assertEquals("USB-C Cable 3m", reloaded.getName());
        assertMoneyEquals("29.90", reloaded.getUnitPrice());
        assertEquals(11, reloaded.getQuantity());
        assertEquals(20, reloaded.getReorderLevel());
        assertEquals("cable", reloaded.getUnit());
        assertEquals("an update must not insert a second row", 1, productDao.count());
    }

    @Test
    public void searchMatchesSkuOrNameCaseInsensitively() {
        Category electronics = createCategory("Electronics");
        Supplier shenzhen = createSupplier("Shenzhen Electronics");
        createProduct("ELE-001", "Wireless Mouse", electronics, shenzhen, "89.00", 42, 10);
        createProduct("ELE-002", "USB-C Cable 2m", electronics, shenzhen, "25.50", 6, 15);
        createProduct("OFF-001", "A4 Copy Paper", null, null, "28.90", 120, 30);

        List<Product> byName = productDao.search("mouse");
        assertEquals("search must match the product name case-insensitively",
                1, byName.size());
        assertEquals("ELE-001", byName.get(0).getSku());

        List<Product> bySkuPrefix = productDao.search("ele-");
        assertEquals("search must match the SKU case-insensitively",
                2, bySkuPrefix.size());
        assertEquals("ELE-001", bySkuPrefix.get(0).getSku());
        assertEquals("ELE-002", bySkuPrefix.get(1).getSku());

        assertTrue("a search without matches must return an empty list",
                productDao.search("no-such-product").isEmpty());
    }

    @Test
    public void findByCategoryIdAndFindLowStockReturnExpectedProducts() {
        Category electronics = createCategory("Electronics");
        Supplier supplier = createSupplier("Shenzhen Electronics");
        createProduct("ELE-001", "Low Mouse", electronics, supplier, "10.00", 3, 5);
        createProduct("ELE-002", "Boundary Cable", electronics, supplier, "20.00", 5, 5);
        createProduct("OFF-001", "Plenty Paper", createCategory("Office"), supplier, "30.00", 100, 10);

        List<Product> electronicsProducts = productDao.findByCategoryId(electronics.getId());
        assertEquals("only products of the requested category are returned",
                2, electronicsProducts.size());
        assertTrue(electronicsProducts.stream()
                .allMatch(product -> electronics.getId().equals(product.getCategoryId())));

        List<Product> lowStock = productDao.findLowStock();
        assertEquals("products at or below the reorder level are low stock",
                2, lowStock.size());
        assertEquals("the low-stock query is ordered by SKU",
                "ELE-001", lowStock.get(0).getSku());
        assertEquals("ELE-002", lowStock.get(1).getSku());
    }

    @Test
    public void deleteByIdRemovesProduct() {
        Supplier supplier = createSupplier("Shenzhen Electronics");
        Product product = createProduct("ELE-003", "Bluetooth Keyboard", null, supplier,
                "159.00", 0, 8);

        productDao.deleteById(product.getId());

        assertFalse(productDao.existsById(product.getId()));
        assertTrue(productDao.findById(product.getId()).isEmpty());
        assertEquals(0, productDao.count());
    }
}
