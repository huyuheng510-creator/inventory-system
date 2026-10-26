package com.cjlu.inventory.support;

import com.cjlu.inventory.dao.CategoryDao;
import com.cjlu.inventory.dao.ProductDao;
import com.cjlu.inventory.dao.PurchaseOrderDao;
import com.cjlu.inventory.dao.PurchaseOrderLineDao;
import com.cjlu.inventory.dao.SupplierDao;
import com.cjlu.inventory.dao.TransactionDao;
import com.cjlu.inventory.dao.UserDao;
import com.cjlu.inventory.dao.jdbc.JdbcCategoryDao;
import com.cjlu.inventory.dao.jdbc.JdbcProductDao;
import com.cjlu.inventory.dao.jdbc.JdbcPurchaseOrderDao;
import com.cjlu.inventory.dao.jdbc.JdbcPurchaseOrderLineDao;
import com.cjlu.inventory.dao.jdbc.JdbcSupplierDao;
import com.cjlu.inventory.dao.jdbc.JdbcTransactionDao;
import com.cjlu.inventory.dao.jdbc.JdbcUserDao;
import com.cjlu.inventory.db.DatabaseManager;
import com.cjlu.inventory.db.SchemaInitializer;
import com.cjlu.inventory.exception.AuthenticationException;
import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.PurchaseOrder;
import com.cjlu.inventory.model.PurchaseOrderLine;
import com.cjlu.inventory.model.PurchaseOrderStatus;
import com.cjlu.inventory.model.StockTransaction;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.model.TransactionType;
import com.cjlu.inventory.model.User;
import com.cjlu.inventory.model.UserRole;
import com.cjlu.inventory.service.AuthService;
import com.cjlu.inventory.service.CatalogService;
import com.cjlu.inventory.service.ProductService;
import com.cjlu.inventory.service.PurchaseOrderService;
import com.cjlu.inventory.service.ReportService;
import com.cjlu.inventory.service.StockService;
import com.cjlu.inventory.util.PasswordHasher;
import org.junit.After;
import org.junit.Before;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

/**
 * Shared fixture for the JUnit 4 test suite.
 *
 * <p>Every test method runs against its own, freshly created embedded Derby
 * <em>in-memory</em> database. The database name contains the test class name
 * and a nanosecond stamp, so parallel test classes or repeated runs can never
 * see each other's data, and no database files are written to disk. The
 * fixture builds the same object graph that
 * {@code app.ApplicationContext} builds for the real application: JDBC DAOs,
 * the business services and an {@link AuthService} with session state.</p>
 *
 * <p>Two accounts are seeded for every test: {@code admin/admin123} (ADMIN) and
 * {@code staff/staff123} (STAFF). Tests that need a session call
 * {@link #loginAsAdmin()} or {@link #loginAsStaff()}; the seeded accounts are
 * intentionally not logged in by default so that the "must be signed in"
 * behaviour can be tested too.</p>
 */
public abstract class AbstractDatabaseTest {

    protected static final String ADMIN_USERNAME = "admin";
    protected static final String ADMIN_PASSWORD = "admin123";
    protected static final String STAFF_USERNAME = "staff";
    protected static final String STAFF_PASSWORD = "staff123";

    protected DatabaseManager databaseManager;

    protected UserDao userDao;
    protected CategoryDao categoryDao;
    protected SupplierDao supplierDao;
    protected ProductDao productDao;
    protected TransactionDao transactionDao;
    protected PurchaseOrderDao purchaseOrderDao;
    protected PurchaseOrderLineDao purchaseOrderLineDao;

    protected AuthService authService;
    protected ProductService productService;
    protected CatalogService catalogService;
    protected StockService stockService;
    protected ReportService reportService;
    protected PurchaseOrderService purchaseOrderService;

    /**
     * Creates a private in-memory database for this test method, installs the
     * production schema and wires the DAOs and services exactly like
     * {@code ApplicationContext} does.
     */
    @Before
    public void setUpDatabase() {
        String databaseName = "test_" + getClass().getSimpleName() + "_" + System.nanoTime();
        databaseManager = new DatabaseManager("jdbc:derby:memory:" + databaseName + ";create=true");
        SchemaInitializer.ensureSchema(databaseManager);

        userDao = new JdbcUserDao(databaseManager);
        categoryDao = new JdbcCategoryDao(databaseManager);
        supplierDao = new JdbcSupplierDao(databaseManager);
        productDao = new JdbcProductDao(databaseManager);
        transactionDao = new JdbcTransactionDao(databaseManager);
        purchaseOrderDao = new JdbcPurchaseOrderDao(databaseManager);
        purchaseOrderLineDao = new JdbcPurchaseOrderLineDao(databaseManager);

        authService = new AuthService(userDao);
        productService = new ProductService(productDao, categoryDao, supplierDao,
                transactionDao, authService);
        catalogService = new CatalogService(categoryDao, supplierDao, productDao, authService);
        stockService = new StockService(productDao, transactionDao, authService);
        Path dataDirectory = Path.of(System.getProperty("java.io.tmpdir"),
                "inventory-junit", databaseName);
        reportService = new ReportService(productDao, categoryDao, transactionDao,
                stockService, dataDirectory);
        purchaseOrderService = new PurchaseOrderService(purchaseOrderDao, purchaseOrderLineDao,
                productDao, supplierDao, stockService, authService, databaseManager);

        userDao.save(new User(ADMIN_USERNAME, PasswordHasher.hash(ADMIN_PASSWORD),
                "Test Administrator", UserRole.ADMIN));
        userDao.save(new User(STAFF_USERNAME, PasswordHasher.hash(STAFF_PASSWORD),
                "Test Warehouse Staff", UserRole.STAFF));
    }

    /** Shuts the per-test in-memory database down so no state leaks between tests. */
    @After
    public void tearDownDatabase() {
        if (databaseManager != null) {
            databaseManager.shutdown();
            databaseManager = null;
        }
    }

    // ------------------------------------------------------------------ sessions

    /** Signs in the seeded administrator. */
    protected void loginAsAdmin() throws AuthenticationException {
        authService.login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    /** Signs in the seeded warehouse-staff user. */
    protected void loginAsStaff() throws AuthenticationException {
        authService.login(STAFF_USERNAME, STAFF_PASSWORD);
    }

    // -------------------------------------------------------------- test data

    /** Persists a user with a hashed password and returns it. */
    protected User createUser(String username, String rawPassword, UserRole role) {
        User user = new User(username, PasswordHasher.hash(rawPassword),
                "Test " + username, role);
        userDao.save(user);
        return user;
    }

    /** Persists a category and returns it. */
    protected Category createCategory(String name) {
        Category category = new Category(name, name + " description");
        categoryDao.save(category);
        return category;
    }

    /** Persists a supplier and returns it. */
    protected Supplier createSupplier(String name) {
        Supplier supplier = new Supplier(name, name + " Contact", "555-0100",
                "contact@example.com");
        supplierDao.save(supplier);
        return supplier;
    }

    /** Persists a product and returns it (repository-level insert, no authorisation). */
    protected Product createProduct(String sku, String name, Category category, Supplier supplier,
                                    String unitPrice, int quantity, int reorderLevel) {
        Product product = new Product(sku, name,
                category == null ? null : category.getId(),
                supplier == null ? null : supplier.getId(),
                new BigDecimal(unitPrice), quantity, reorderLevel, "piece");
        productDao.save(product);
        return product;
    }

    /** Persists an existing stock movement for a product (used by the report/DAO tests). */
    protected StockTransaction recordTransaction(Product product, TransactionType type,
                                                 int quantity, LocalDateTime occurredAt) {
        StockTransaction transaction = new StockTransaction(product.getId(), type, quantity,
                product.getUnitPrice(), "admin", "test history", occurredAt);
        transactionDao.save(transaction);
        return transaction;
    }

    /** Saves a purchase-order line and returns it. */
    protected PurchaseOrderLine saveOrderLine(PurchaseOrder order, Product product,
                                              int quantity, String unitCost) {
        PurchaseOrderLine line = new PurchaseOrderLine(order.getId(), product.getId(), quantity,
                new BigDecimal(unitCost));
        purchaseOrderLineDao.save(line);
        return line;
    }

    /** Creates and persists a purchase-order header with the given status. */
    protected PurchaseOrder createOrder(Supplier supplier, String orderNumber,
                                        PurchaseOrderStatus status) {
        PurchaseOrder order = new PurchaseOrder(orderNumber, supplier.getId(), ADMIN_USERNAME,
                LocalDate.now().plusDays(7), "Test purchase order");
        order.setStatus(status);
        purchaseOrderDao.save(order);
        return order;
    }

    /** Compares monetary values by numeric value (DECIMAL(12,2) adds a scale). */
    protected static void assertMoneyEquals(String expected, BigDecimal actual) {
        assertEquals("monetary value " + expected + " vs " + actual,
                0, new BigDecimal(expected).compareTo(actual));
    }
}
