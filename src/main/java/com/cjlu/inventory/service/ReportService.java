package com.cjlu.inventory.service;

import com.cjlu.inventory.exception.InventoryException;
import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.MovementLine;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.ReorderSuggestion;
import com.cjlu.inventory.model.StockTransaction;
import com.cjlu.inventory.model.ValuationLine;
import com.cjlu.inventory.dao.CategoryDao;
import com.cjlu.inventory.dao.ProductDao;
import com.cjlu.inventory.dao.TransactionDao;
import com.cjlu.inventory.service.strategy.ReorderPolicy;
import com.cjlu.inventory.util.CsvUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reporting use cases (FR-11, FR-12, FR-13): inventory valuation, stock
 * movement aggregation, reorder suggestions and CSV export of every report.
 *
 * <p>Report rows are immutable value objects ({@link ValuationLine},
 * {@link MovementLine}, {@link ReorderSuggestion}); the service never prints
 * anything, which keeps it reusable behind a GUI in Project 2.</p>
 */
public class ReportService {

    private static final DateTimeFormatter EXPORT_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int DEMAND_WINDOW_DAYS = 30;

    private final ProductDao ProductDao;
    private final CategoryDao CategoryDao;
    private final TransactionDao TransactionDao;
    private final StockService stockService;
    private final Path dataDirectory;

    public ReportService(ProductDao ProductDao,
                         CategoryDao CategoryDao,
                         TransactionDao TransactionDao,
                         StockService stockService,
                         Path dataDirectory) {
        this.ProductDao = ProductDao;
        this.CategoryDao = CategoryDao;
        this.TransactionDao = TransactionDao;
        this.stockService = stockService;
        this.dataDirectory = dataDirectory;
    }

    /** Per-product valuation rows with category names, grouped by category (FR-11). */
    public List<ValuationLine> inventoryValuation() {
        Map<String, String> categoryNames = new HashMap<>();
        for (Category category : CategoryDao.findAll()) {
            categoryNames.put(category.getId(), category.getName());
        }
        List<ValuationLine> lines = new ArrayList<>();
        for (Product product : ProductDao.findAll()) {
            String categoryName = categoryNames.getOrDefault(product.getCategoryId(), "Uncategorised");
            lines.add(new ValuationLine(product.getSku(), product.getName(), categoryName,
                    product.getQuantity(), product.getUnitPrice(), product.getInventoryValue()));
        }
        lines.sort(Comparator.comparing(ValuationLine::getCategoryName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ValuationLine::getProductSku));
        return lines;
    }

    /** Grand total of the inventory valuation (FR-11). */
    public BigDecimal totalInventoryValue() {
        BigDecimal total = BigDecimal.ZERO;
        for (ValuationLine line : inventoryValuation()) {
            total = total.add(line.getLineTotal());
        }
        return total;
    }

    /** Stock-in / stock-out / net aggregation per product over a date range (FR-12). */
    public List<MovementLine> stockMovement(LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        Map<String, int[]> aggregates = new LinkedHashMap<>();
        for (StockTransaction transaction : TransactionDao.findBetween(fromInclusive, toExclusive)) {
            int[] aggregate = aggregates.computeIfAbsent(transaction.getProductId(), key -> new int[4]);
            switch (transaction.getType()) {
                case STOCK_IN -> aggregate[0] += transaction.getQuantity();
                case STOCK_OUT -> aggregate[1] += transaction.getQuantity();
                case ADJUSTMENT -> {
                    // Adjustments only affect the net change; they are not purchases or sales.
                }
            }
            aggregate[2] += transaction.getSignedQuantity();
            aggregate[3]++;
        }
        List<MovementLine> lines = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : aggregates.entrySet()) {
            Product product = ProductDao.findById(entry.getKey()).orElse(null);
            if (product == null) {
                continue;
            }
            int[] aggregate = entry.getValue();
            lines.add(new MovementLine(product.getSku(), product.getName(),
                    aggregate[0], aggregate[1], aggregate[2], aggregate[3]));
        }
        lines.sort(Comparator.comparing(MovementLine::getProductSku));
        return lines;
    }

    /** Reorder suggestions for all low-stock products under the selected policy (FR-12). */
    public List<ReorderSuggestion> reorderSuggestions(ReorderPolicy policy) {
        Objects.requireNonNull(policy, "policy");
        LocalDateTime windowStart = LocalDateTime.now().minusDays(DEMAND_WINDOW_DAYS);
        List<ReorderSuggestion> suggestions = new ArrayList<>();
        for (Product product : stockService.lowStockProducts()) {
            List<StockTransaction> recentTransactions = TransactionDao
                    .findByProductId(product.getId()).stream()
                    .filter(transaction -> !transaction.getOccurredAt().isBefore(windowStart))
                    .toList();
            int suggested = Math.max(0, policy.suggestQuantity(product, recentTransactions));
            suggestions.add(new ReorderSuggestion(product.getSku(), product.getName(),
                    policy.getName(), product.getQuantity(), product.getReorderLevel(), suggested));
        }
        suggestions.sort(Comparator.comparingInt(ReorderSuggestion::getSuggestedQuantity).reversed()
                .thenComparing(ReorderSuggestion::getProductSku));
        return suggestions;
    }

    /** Exports the valuation report (FR-13). */
    public void exportValuation(Path file, List<ValuationLine> lines) throws InventoryException {
        List<String[]> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ValuationLine line : lines) {
            rows.add(new String[] {
                    line.getProductSku(), line.getProductName(), line.getCategoryName(),
                    Integer.toString(line.getQuantity()),
                    line.getUnitPrice().toPlainString(),
                    line.getLineTotal().toPlainString()
            });
            total = total.add(line.getLineTotal());
        }
        rows.add(new String[] {"TOTAL", "", "", "", "", total.toPlainString()});
        writeCsv(file, new String[] {"sku", "name", "category", "quantity", "unitPrice", "lineTotal"}, rows);
    }

    /** Exports the stock movement report (FR-13). */
    public void exportMovement(Path file, List<MovementLine> lines) throws InventoryException {
        List<String[]> rows = new ArrayList<>();
        for (MovementLine line : lines) {
            rows.add(new String[] {
                    line.getProductSku(), line.getProductName(),
                    Integer.toString(line.getStockInQuantity()),
                    Integer.toString(line.getStockOutQuantity()),
                    Integer.toString(line.getNetChange()),
                    Integer.toString(line.getTransactionCount())
            });
        }
        writeCsv(file, new String[] {"sku", "name", "stockIn", "stockOut", "netChange", "transactions"}, rows);
    }

    /** Exports the reorder suggestions (FR-13). */
    public void exportReorderSuggestions(Path file, List<ReorderSuggestion> suggestions)
            throws InventoryException {
        List<String[]> rows = new ArrayList<>();
        for (ReorderSuggestion suggestion : suggestions) {
            rows.add(new String[] {
                    suggestion.getProductSku(), suggestion.getProductName(), suggestion.getPolicyName(),
                    Integer.toString(suggestion.getCurrentQuantity()),
                    Integer.toString(suggestion.getReorderLevel()),
                    Integer.toString(suggestion.getSuggestedQuantity())
            });
        }
        writeCsv(file, new String[] {"sku", "name", "policy", "currentQuantity", "reorderLevel",
                "suggestedQuantity"}, rows);
    }

    /** Timestamped export location under {@code data/exports/} (FR-13). */
    public Path newExportPath(String prefix) {
        String stamp = LocalDateTime.now().format(EXPORT_STAMP);
        return dataDirectory.resolve("exports").resolve(prefix + "_" + stamp + ".csv");
    }

    private void writeCsv(Path file, String[] header, List<String[]> rows) throws InventoryException {
        try {
            CsvUtils.write(file, header, rows);
        } catch (IOException e) {
            throw new InventoryException("Could not write the report file: " + e.getMessage(), e);
        }
    }
}
