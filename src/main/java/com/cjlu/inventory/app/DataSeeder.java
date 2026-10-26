package com.cjlu.inventory.app;

import com.cjlu.inventory.dao.CategoryDao;
import com.cjlu.inventory.dao.ProductDao;
import com.cjlu.inventory.dao.PurchaseOrderDao;
import com.cjlu.inventory.dao.PurchaseOrderLineDao;
import com.cjlu.inventory.dao.SupplierDao;
import com.cjlu.inventory.dao.TransactionDao;
import com.cjlu.inventory.dao.UserDao;
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
import com.cjlu.inventory.util.PasswordHasher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Seeds the demonstration data on the very first start.
 *
 * <p>The seed writes through the DAOs inside one transaction, deliberately
 * bypassing the authorisation and validation services because the dataset is
 * trusted program data, not user input. It runs only when the database has no
 * users yet, so deleting {@code data/} restores exactly this demo state.</p>
 */
public final class DataSeeder {

    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private DataSeeder() {
        // utility class
    }

    /** Inserts the demo dataset when the database is empty. */
    public static void seedIfEmpty(ApplicationContext context) {
        if (context.getUserDao().count() > 0) {
            return;
        }
        context.getDatabaseManager().runInTransaction(() -> {
            seedUsers(context.getUserDao());
            seedCatalogue(context.getCategoryDao(), context.getSupplierDao(), context.getProductDao());
            seedTransactions(context.getTransactionDao(), context.getProductDao());
            seedPurchaseOrders(context);
        });
    }

    private static void seedUsers(UserDao users) {
        users.save(new User("admin", PasswordHasher.hash("admin123"),
                "System Administrator", UserRole.ADMIN));
        users.save(new User("staff", PasswordHasher.hash("staff123"),
                "Warehouse Staff", UserRole.STAFF));
    }

    private static void seedCatalogue(CategoryDao categories, SupplierDao suppliers, ProductDao products) {
        Category electronics = new Category("Electronics",
                "Consumer electronics and computer accessories");
        Category office = new Category("Office Supplies", "Stationery and paper products");
        Category food = new Category("Food & Beverage", "Drinks and pantry supplies");
        Category cleaning = new Category("Cleaning Supplies", "Cleaning and hygiene products");
        Category packaging = new Category("Packaging", "Shipping and storage materials");
        categories.save(electronics);
        categories.save(office);
        categories.save(food);
        categories.save(cleaning);
        categories.save(packaging);

        Supplier shenzhen = new Supplier("Shenzhen Electronics Co.", "Li Wei",
                "13800138001", "sales@sz-elec.example");
        Supplier hangzhou = new Supplier("Hangzhou Office Depot", "Wang Fang",
                "13900139002", "wang@hzofficedepot.example");
        Supplier ningbo = new Supplier("Ningbo Fresh Foods", "Chen Min",
                "13700137003", "chen@nbfresh.example");
        Supplier jiaxing = new Supplier("Jiaxing CleanPro", "Zhao Lei",
                "13600136004", "zhao@jxclean.example");
        suppliers.save(shenzhen);
        suppliers.save(hangzhou);
        suppliers.save(ningbo);
        suppliers.save(jiaxing);

        products.save(product("ELE-001", "Wireless Mouse", electronics, shenzhen,
                "89.00", 42, 10, "piece"));
        products.save(product("ELE-002", "USB-C Cable 2m", electronics, shenzhen,
                "25.50", 6, 15, "piece"));
        products.save(product("ELE-003", "Bluetooth Keyboard", electronics, shenzhen,
                "159.00", 0, 8, "piece"));
        products.save(product("OFF-001", "A4 Copy Paper 500 Sheets", office, hangzhou,
                "28.90", 120, 30, "ream"));
        products.save(product("OFF-002", "Gel Pen 0.5mm", office, hangzhou,
                "3.50", 18, 40, "piece"));
        products.save(product("FNB-001", "Instant Coffee 200g", food, ningbo,
                "58.00", 25, 10, "jar"));
        products.save(product("FNB-002", "Bottled Water 550ml", food, ningbo,
                "2.00", 210, 60, "bottle"));
        products.save(product("CLN-001", "Multi-surface Cleaner 1L", cleaning, jiaxing,
                "19.90", 9, 12, "bottle"));
        products.save(product("PKG-001", "Cardboard Box Medium", packaging, jiaxing,
                "4.20", 160, 50, "piece"));
        products.save(product("PKG-002", "Bubble Wrap Roll 50m", packaging, jiaxing,
                "45.00", 14, 8, "roll"));
    }

    private static void seedTransactions(TransactionDao transactions, ProductDao products) {
        LocalDate today = LocalDate.now();

        seedTransaction(transactions, products, "ELE-001", TransactionType.STOCK_IN, 20,
                "89.00", "admin", "Initial stock intake", daysAgo(today, 30, 9, 30));
        seedTransaction(transactions, products, "ELE-001", TransactionType.STOCK_OUT, 8,
                "89.00", "staff", "Customer dispatch", daysAgo(today, 25, 11, 15));
        seedTransaction(transactions, products, "ELE-001", TransactionType.STOCK_OUT, 5,
                "89.00", "staff", "Customer dispatch", daysAgo(today, 12, 14, 40));
        seedTransaction(transactions, products, "ELE-001", TransactionType.STOCK_IN, 10,
                "89.00", "admin", "Monthly replenishment", daysAgo(today, 5, 10, 5));
        seedTransaction(transactions, products, "ELE-001", TransactionType.STOCK_OUT, 2,
                "89.00", "staff", "Sample dispatch", daysAgo(today, 2, 16, 20));

        seedTransaction(transactions, products, "ELE-002", TransactionType.STOCK_IN, 20,
                "25.50", "admin", "Initial stock intake", daysAgo(today, 40, 8, 45));
        seedTransaction(transactions, products, "ELE-002", TransactionType.STOCK_OUT, 6,
                "25.50", "staff", "Customer dispatch", daysAgo(today, 20, 15, 10));
        seedTransaction(transactions, products, "ELE-002", TransactionType.STOCK_OUT, 8,
                "25.50", "staff", "Online order dispatch", daysAgo(today, 10, 13, 25));

        seedTransaction(transactions, products, "OFF-001", TransactionType.STOCK_IN, 100,
                "28.90", "admin", "Bulk purchase", daysAgo(today, 35, 9, 0));
        seedTransaction(transactions, products, "OFF-001", TransactionType.STOCK_OUT, 30,
                "28.90", "staff", "Department issue", daysAgo(today, 18, 10, 30));
        seedTransaction(transactions, products, "OFF-001", TransactionType.STOCK_OUT, 20,
                "28.90", "staff", "Department issue", daysAgo(today, 6, 16, 0));

        seedTransaction(transactions, products, "OFF-002", TransactionType.STOCK_IN, 60,
                "3.50", "admin", "Initial stock intake", daysAgo(today, 33, 9, 10));
        seedTransaction(transactions, products, "OFF-002", TransactionType.ADJUSTMENT, -2,
                "3.50", "admin", "Cycle count correction", daysAgo(today, 4, 15, 20));

        seedTransaction(transactions, products, "FNB-002", TransactionType.STOCK_IN, 300,
                "2.00", "admin", "Monthly replenishment", daysAgo(today, 28, 8, 30));
        seedTransaction(transactions, products, "FNB-002", TransactionType.STOCK_OUT, 90,
                "2.00", "staff", "Customer dispatch", daysAgo(today, 9, 12, 15));

        seedTransaction(transactions, products, "PKG-001", TransactionType.STOCK_IN, 200,
                "4.20", "admin", "Bulk purchase", daysAgo(today, 45, 9, 0));
        seedTransaction(transactions, products, "PKG-001", TransactionType.STOCK_OUT, 40,
                "4.20", "staff", "Customer dispatch", daysAgo(today, 7, 10, 0));

        seedTransaction(transactions, products, "PKG-002", TransactionType.STOCK_IN, 20,
                "45.00", "admin", "Initial stock intake", daysAgo(today, 22, 9, 50));
    }

    /**
     * Creates two demo purchase orders: one ordered replenishment for the
     * low-stock electronics and one draft proposal for the stationery supplier.
     */
    private static void seedPurchaseOrders(ApplicationContext context) {
        PurchaseOrderDao orders = context.getPurchaseOrderDao();
        PurchaseOrderLineDao lines = context.getPurchaseOrderLineDao();
        ProductDao products = context.getProductDao();
        SupplierDao suppliers = context.getSupplierDao();

        String prefix = "PO-" + LocalDate.now().format(ORDER_DATE) + "-";
        Supplier shenzhen = suppliers.findByName("Shenzhen Electronics Co.").orElseThrow();
        Supplier hangzhou = suppliers.findByName("Hangzhou Office Depot").orElseThrow();
        Product cable = products.findBySku("ELE-002").orElseThrow();
        Product keyboard = products.findBySku("ELE-003").orElseThrow();
        Product pens = products.findBySku("OFF-002").orElseThrow();

        PurchaseOrder electronicsOrder = new PurchaseOrder(orders.nextOrderNumber(prefix),
                shenzhen.getId(), "admin", LocalDate.now().plusDays(7),
                "Replenish low-stock electronics");
        orders.save(electronicsOrder);
        lines.save(new PurchaseOrderLine(electronicsOrder.getId(), cable.getId(), 30, cable.getUnitPrice()));
        lines.save(new PurchaseOrderLine(electronicsOrder.getId(), keyboard.getId(), 12, keyboard.getUnitPrice()));
        electronicsOrder.setStatus(PurchaseOrderStatus.ORDERED);
        orders.save(electronicsOrder);

        PurchaseOrder stationeryOrder = new PurchaseOrder(orders.nextOrderNumber(prefix),
                hangzhou.getId(), "staff", LocalDate.now().plusDays(10),
                "Stationery reorder proposal");
        orders.save(stationeryOrder);
        lines.save(new PurchaseOrderLine(stationeryOrder.getId(), pens.getId(), 60, pens.getUnitPrice()));
    }

    private static Product product(String sku, String name, Category category, Supplier supplier,
                                   String unitPrice, int quantity, int reorderLevel, String unit) {
        return new Product(sku, name, category.getId(), supplier.getId(),
                new BigDecimal(unitPrice), quantity, reorderLevel, unit);
    }

    private static void seedTransaction(TransactionDao transactions, ProductDao products, String sku,
                                        TransactionType type, int quantity, String unitPrice,
                                        String operator, String note, LocalDateTime occurredAt) {
        Product product = products.findBySku(sku).orElseThrow();
        transactions.save(new StockTransaction(product.getId(), type, quantity,
                new BigDecimal(unitPrice), operator, note, occurredAt));
    }

    private static LocalDateTime daysAgo(LocalDate today, int days, int hour, int minute) {
        return today.minusDays(days).atTime(hour, minute);
    }
}
