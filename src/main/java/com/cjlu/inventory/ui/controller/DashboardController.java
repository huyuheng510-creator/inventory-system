package com.cjlu.inventory.ui.controller;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockStatus;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.model.ValuationLine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads the aggregated data of the dashboard from the services: KPI figures,
 * the low-stock products and the inventory value per category used by the
 * self-drawn bar chart.
 */
public final class DashboardController {

    private final ApplicationContext context;

    public DashboardController(ApplicationContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /** Loads one consistent snapshot of the dashboard data. */
    public DashboardData load() {
        List<Product> products = context.getProductService().listProducts();

        int lowStockCount = 0;
        int outOfStockCount = 0;
        for (Product product : products) {
            if (product.getStockStatus() == StockStatus.OUT_OF_STOCK) {
                outOfStockCount++;
                lowStockCount++;
            } else if (product.getStockStatus() == StockStatus.LOW_STOCK) {
                lowStockCount++;
            }
        }

        List<ValuationLine> valuation = context.getReportService().inventoryValuation();
        BigDecimal totalValue = BigDecimal.ZERO;
        Map<String, BigDecimal> valueByCategory = new HashMap<>();
        for (ValuationLine line : valuation) {
            totalValue = totalValue.add(line.getLineTotal());
            valueByCategory.merge(line.getCategoryName(), line.getLineTotal(), BigDecimal::add);
        }
        List<Map.Entry<String, BigDecimal>> sortedCategories = new ArrayList<>(valueByCategory.entrySet());
        sortedCategories.sort(Comparator.comparing(
                (Map.Entry<String, BigDecimal> entry) -> entry.getValue()).reversed());
        Map<String, BigDecimal> chartData = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : sortedCategories) {
            chartData.put(entry.getKey(), entry.getValue());
        }

        Map<String, String> categoryNames = new HashMap<>();
        for (Category category : context.getCatalogService().listCategories()) {
            categoryNames.put(category.getId(), category.getName());
        }
        Map<String, String> supplierNames = new HashMap<>();
        for (Supplier supplier : context.getCatalogService().listSuppliers()) {
            supplierNames.put(supplier.getId(), supplier.getName());
        }

        List<Product> lowStockProducts = products.stream()
                .filter(Product::isLowStock)
                .sorted(Comparator.comparingInt(Product::getQuantity)
                        .thenComparing(Product::getSku))
                .toList();

        return new DashboardData(products.size(), lowStockCount, outOfStockCount,
                context.getPurchaseOrderService().openOrderCount(), totalValue,
                lowStockProducts, chartData, categoryNames, supplierNames);
    }

    /** Immutable dashboard snapshot consumed by the view. */
    public record DashboardData(int productCount, int lowStockCount, int outOfStockCount,
                                int openOrderCount, BigDecimal totalValue,
                                List<Product> lowStockProducts,
                                Map<String, BigDecimal> valueByCategory,
                                Map<String, String> categoryNames,
                                Map<String, String> supplierNames) {
    }
}
