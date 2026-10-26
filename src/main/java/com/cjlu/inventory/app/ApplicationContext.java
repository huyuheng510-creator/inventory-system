package com.cjlu.inventory.app;

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
import com.cjlu.inventory.service.AlertHistoryService;
import com.cjlu.inventory.service.AuthService;
import com.cjlu.inventory.service.CatalogService;
import com.cjlu.inventory.service.ProductService;
import com.cjlu.inventory.service.PurchaseOrderService;
import com.cjlu.inventory.service.ReportService;
import com.cjlu.inventory.service.StockService;
import com.cjlu.inventory.service.strategy.DemandBasedReorderPolicy;
import com.cjlu.inventory.service.strategy.ReorderPolicy;
import com.cjlu.inventory.service.strategy.ThresholdReorderPolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Composition root of the Project 2 application.
 *
 * <p>Constructs the object graph once: the embedded Derby database, the JDBC
 * DAOs, the business services and the reorder strategies. The Swing GUI
 * receives this context (constructor injection) and only talks to services,
 * never to DAOs or JDBC. There is deliberately no service locator and no
 * mutable global state besides the database manager singleton.</p>
 */
public class ApplicationContext {

    private final DatabaseManager databaseManager;
    private final Path dataDirectory;
    private final Path exportDirectory;

    private final UserDao userDao;
    private final CategoryDao categoryDao;
    private final SupplierDao supplierDao;
    private final ProductDao productDao;
    private final TransactionDao transactionDao;
    private final PurchaseOrderDao purchaseOrderDao;
    private final PurchaseOrderLineDao purchaseOrderLineDao;

    private final AuthService authService;
    private final AlertHistoryService alertHistoryService;
    private final ProductService productService;
    private final CatalogService catalogService;
    private final StockService stockService;
    private final ReportService reportService;
    private final PurchaseOrderService purchaseOrderService;
    private final List<ReorderPolicy> reorderPolicies;

    private ApplicationContext(DatabaseManager databaseManager, Path dataDirectory,
                               UserDao userDao, CategoryDao categoryDao, SupplierDao supplierDao,
                               ProductDao productDao, TransactionDao transactionDao,
                               PurchaseOrderDao purchaseOrderDao,
                               PurchaseOrderLineDao purchaseOrderLineDao,
                               AuthService authService, AlertHistoryService alertHistoryService,
                               ProductService productService,
                               CatalogService catalogService, StockService stockService,
                               ReportService reportService,
                               PurchaseOrderService purchaseOrderService,
                               List<ReorderPolicy> reorderPolicies) {
        this.databaseManager = databaseManager;
        this.dataDirectory = dataDirectory;
        this.exportDirectory = dataDirectory.resolve("exports");
        this.userDao = userDao;
        this.categoryDao = categoryDao;
        this.supplierDao = supplierDao;
        this.productDao = productDao;
        this.transactionDao = transactionDao;
        this.purchaseOrderDao = purchaseOrderDao;
        this.purchaseOrderLineDao = purchaseOrderLineDao;
        this.authService = authService;
        this.alertHistoryService = alertHistoryService;
        this.productService = productService;
        this.catalogService = catalogService;
        this.stockService = stockService;
        this.reportService = reportService;
        this.purchaseOrderService = purchaseOrderService;
        this.reorderPolicies = reorderPolicies;
    }

    /**
     * Creates the default application context: embedded Derby database under
     * {@code data/inventorydb}, schema, demo seed on first start, DAOs and
     * services.
     */
    public static ApplicationContext createDefault() {
        Path dataDirectory = Path.of("data");
        try {
            Files.createDirectories(dataDirectory);
            Files.createDirectories(dataDirectory.resolve("exports"));
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the data directory: "
                    + dataDirectory.toAbsolutePath(), e);
        }

        DatabaseManager databaseManager = DatabaseManager.initialize(dataDirectory.resolve("inventorydb"));
        try {
            SchemaInitializer.ensureSchema(databaseManager);

            UserDao userDao = new JdbcUserDao(databaseManager);
            CategoryDao categoryDao = new JdbcCategoryDao(databaseManager);
            SupplierDao supplierDao = new JdbcSupplierDao(databaseManager);
            ProductDao productDao = new JdbcProductDao(databaseManager);
            TransactionDao transactionDao = new JdbcTransactionDao(databaseManager);
            PurchaseOrderDao purchaseOrderDao = new JdbcPurchaseOrderDao(databaseManager);
            PurchaseOrderLineDao purchaseOrderLineDao = new JdbcPurchaseOrderLineDao(databaseManager);

            AuthService authService = new AuthService(userDao);
            AlertHistoryService alertHistoryService = new AlertHistoryService();
            ProductService productService = new ProductService(productDao, categoryDao,
                    supplierDao, transactionDao, authService);
            CatalogService catalogService = new CatalogService(categoryDao, supplierDao,
                    productDao, authService);
            StockService stockService = new StockService(productDao, transactionDao, authService);
            stockService.addObserver(alertHistoryService);
            ReportService reportService = new ReportService(productDao, categoryDao,
                    transactionDao, stockService, dataDirectory);
            PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                    purchaseOrderDao, purchaseOrderLineDao, productDao, supplierDao,
                    stockService, authService, databaseManager);

            List<ReorderPolicy> reorderPolicies = List.of(
                    new ThresholdReorderPolicy(),
                    new DemandBasedReorderPolicy());

            ApplicationContext context = new ApplicationContext(databaseManager, dataDirectory,
                    userDao, categoryDao, supplierDao, productDao, transactionDao,
                    purchaseOrderDao, purchaseOrderLineDao, authService, alertHistoryService,
                    productService, catalogService, stockService, reportService,
                    purchaseOrderService, reorderPolicies);
            DataSeeder.seedIfEmpty(context);
            return context;
        } catch (RuntimeException e) {
            databaseManager.shutdown();
            throw e;
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public Path getExportDirectory() {
        return exportDirectory;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public CategoryDao getCategoryDao() {
        return categoryDao;
    }

    public SupplierDao getSupplierDao() {
        return supplierDao;
    }

    public ProductDao getProductDao() {
        return productDao;
    }

    public TransactionDao getTransactionDao() {
        return transactionDao;
    }

    public PurchaseOrderDao getPurchaseOrderDao() {
        return purchaseOrderDao;
    }

    public PurchaseOrderLineDao getPurchaseOrderLineDao() {
        return purchaseOrderLineDao;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public AlertHistoryService getAlertHistoryService() {
        return alertHistoryService;
    }

    public ProductService getProductService() {
        return productService;
    }

    public CatalogService getCatalogService() {
        return catalogService;
    }

    public StockService getStockService() {
        return stockService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public PurchaseOrderService getPurchaseOrderService() {
        return purchaseOrderService;
    }

    public List<ReorderPolicy> getReorderPolicies() {
        return reorderPolicies;
    }
}
