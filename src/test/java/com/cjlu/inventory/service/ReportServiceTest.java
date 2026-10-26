package com.cjlu.inventory.service;

import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.MovementLine;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.TransactionType;
import com.cjlu.inventory.model.ValuationLine;
import com.cjlu.inventory.support.AbstractDatabaseTest;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Report tests for {@link ReportService}: the per-product inventory valuation
 * with its grand total, and the stock-movement aggregation per product over a
 * fixed date range (stock-in and stock-out totals, net change, row count).
 */
public class ReportServiceTest extends AbstractDatabaseTest {

    @Test
    public void inventoryValuationReturnsOneLinePerProductWithTotal() {
        Category electronics = createCategory("Electronics");
        Category office = createCategory("Office Supplies");
        Product mouse = createProduct("ELE-001", "Wireless Mouse", electronics, null,
                "10.00", 5, 1);
        Product cable = createProduct("ELE-002", "USB-C Cable", electronics, null,
                "7.50", 2, 1);
        Product paper = createProduct("OFF-001", "A4 Copy Paper", office, null,
                "2.00", 3, 1);
        Product loose = createProduct("MSC-001", "Loose Item", null, null,
                "5.00", 4, 1);

        List<ValuationLine> lines = reportService.inventoryValuation();

        assertEquals("one row per product", 4, lines.size());
        assertEquals("rows are ordered by category name then SKU",
                List.of("ELE-001", "ELE-002", "OFF-001", "MSC-001"),
                lines.stream().map(ValuationLine::getProductSku).toList());
        assertEquals("Electronics", lines.get(0).getCategoryName());
        assertEquals("Office Supplies", lines.get(2).getCategoryName());
        assertEquals("Uncategorised", lines.get(3).getCategoryName());

        ValuationLine mouseLine = lines.get(0);
        assertEquals(5, mouseLine.getQuantity());
        assertMoneyEquals("10.00", mouseLine.getUnitPrice());
        assertMoneyEquals("50.00", mouseLine.getLineTotal());
        assertMoneyEquals("15.00", lines.get(1).getLineTotal());
        assertMoneyEquals("6.00", lines.get(2).getLineTotal());
        assertMoneyEquals("20.00", lines.get(3).getLineTotal());

        assertEquals("the grand total is the sum of all line totals",
                new BigDecimal("91.00"), reportService.totalInventoryValue());
        assertMoneyEquals("91.00", reportService.totalInventoryValue());
        assertEquals(4, productDao.count());
    }

    @Test
    public void stockMovementAggregatesMovementsWithinTheDateRange() {
        Product mouse = createProduct("ELE-001", "Wireless Mouse", null, null,
                "10.00", 0, 1);
        Product paper = createProduct("OFF-001", "A4 Copy Paper", null, null,
                "2.00", 0, 1);
        LocalDateTime from = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 1, 0, 0);
        recordTransaction(mouse, TransactionType.STOCK_IN, 20, LocalDateTime.of(2026, 3, 2, 10, 0));
        recordTransaction(mouse, TransactionType.STOCK_OUT, 5, LocalDateTime.of(2026, 3, 3, 11, 0));
        recordTransaction(mouse, TransactionType.ADJUSTMENT, -2, LocalDateTime.of(2026, 3, 4, 12, 0));
        recordTransaction(paper, TransactionType.STOCK_IN, 7, LocalDateTime.of(2026, 3, 5, 9, 0));
        recordTransaction(mouse, TransactionType.STOCK_IN, 100,
                LocalDateTime.of(2026, 4, 2, 10, 0));

        List<MovementLine> lines = reportService.stockMovement(from, to);

        assertEquals("only products with movements in the range are reported",
                2, lines.size());
        assertEquals("rows are ordered by product SKU",
                List.of("ELE-001", "OFF-001"),
                lines.stream().map(MovementLine::getProductSku).toList());

        MovementLine mouseMovements = lines.get(0);
        assertEquals(20, mouseMovements.getStockInQuantity());
        assertEquals(5, mouseMovements.getStockOutQuantity());
        assertEquals("the net change includes the signed adjustment",
                13, mouseMovements.getNetChange());
        assertEquals(3, mouseMovements.getTransactionCount());

        MovementLine paperMovements = lines.get(1);
        assertEquals(7, paperMovements.getStockInQuantity());
        assertEquals(0, paperMovements.getStockOutQuantity());
        assertEquals(7, paperMovements.getNetChange());
        assertEquals(1, paperMovements.getTransactionCount());

        assertTrue("transactions outside the half-open range are excluded "
                        + "(the April stock-in must not be counted)",
                reportService.stockMovement(from, from.plusHours(1)).isEmpty());
    }
}
