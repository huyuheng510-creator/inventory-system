package com.cjlu.inventory.service;

import com.cjlu.inventory.exception.AuthenticationException;
import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.service.ProductService.ImportResult;
import com.cjlu.inventory.support.AbstractDatabaseTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the CSV bulk-import feature
 * ({@link ProductService#importFromCsv(Path)}).
 *
 * <p>The import resolves the category and the supplier by <em>name</em> rather
 * than by id, because a hand-written spreadsheet cannot be expected to contain
 * UUIDs. Each row is committed independently, so a broken row must not undo
 * the rows before it — that is the behaviour these tests pin down.</p>
 */
public class CsvImportServiceTest extends AbstractDatabaseTest {

    private Path csvFile;
    private Category category;
    private Supplier supplier;

    /** Writes the given CSV text to a temporary file and returns its path. */
    private Path writeCsv(String... lines) throws IOException {
        Path file = Files.createTempFile("inventory-import", ".csv");
        Files.write(file, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        csvFile = file;
        return file;
    }

    @Before
    public void createMasterData() {
        category = createCategory("Hardware");
        supplier = createSupplier("Acme Supplies");
    }

    @Test
    public void validRowsAreImportedAndTheHeaderIsSkipped() throws Exception {
        Path file = writeCsv(
                "sku,name,category,supplier,unit_price,quantity,reorder_level,unit",
                "CSV-001,Blue Bolt,Hardware,Acme Supplies,1.25,40,10,piece",
                "CSV-002,Red Nut,Hardware,Acme Supplies,0.50,100,20,piece");
        loginAsAdmin();

        List<ImportResult> results = productService.importFromCsv(file);

        assertEquals("both data rows are reported", 2, results.size());
        assertTrue("the first row succeeded", results.get(0).isSuccess());
        assertTrue("the second row succeeded", results.get(1).isSuccess());
        assertNull("a successful row carries no error message", results.get(0).error());
        assertEquals("the row numbers count data rows, not the header",
                1, results.get(0).rowNumber());
        assertEquals(2, results.get(1).rowNumber());

        Product imported = productDao.findBySku("CSV-001").orElseThrow();
        assertEquals("Blue Bolt", imported.getName());
        assertEquals("1.25", imported.getUnitPrice().toPlainString());
        assertEquals(40, imported.getQuantity());
        assertEquals(10, imported.getReorderLevel());
        assertEquals("piece", imported.getUnit());
        assertEquals("the category is resolved by name", category.getId(),
                imported.getCategoryId());
        assertEquals("the supplier is resolved by name", supplier.getId(),
                imported.getSupplierId());
    }

    @Test
    public void aHeaderlessFileIsImportedFromItsFirstRow() throws Exception {
        Path file = writeCsv("CSV-100,No Header Widget,Hardware,Acme Supplies,2.00,5,1,box");
        loginAsAdmin();

        List<ImportResult> results = productService.importFromCsv(file);

        assertEquals(1, results.size());
        assertTrue("a file without a header row still imports", results.get(0).isSuccess());
        assertEquals(1, results.get(0).rowNumber());
        assertNotNull(productDao.findBySku("CSV-100").orElse(null));
    }

    @Test
    public void aBrokenRowIsSkippedAndTheOtherRowsStillImport() throws Exception {
        Path file = writeCsv(
                "sku,name,category,supplier,unit_price,quantity,reorder_level,unit",
                "CSV-201,Good One,Hardware,Acme Supplies,1.00,10,2,piece",
                "CSV-202,Bad Price,Hardware,Acme Supplies,not-a-number,10,2,piece",
                "CSV-203,Good Two,Hardware,Acme Supplies,2.00,20,4,piece");
        loginAsAdmin();

        List<ImportResult> results = productService.importFromCsv(file);

        assertEquals("all three rows are reported", 3, results.size());
        assertTrue("row 1 succeeds", results.get(0).isSuccess());
        assertFalse("row 2 fails", results.get(1).isSuccess());
        assertTrue("row 3 succeeds", results.get(2).isSuccess());
        assertTrue("the error message names the offending field: "
                        + results.get(1).error(),
                results.get(1).error().contains("Invalid number"));

        assertEquals("the good rows are committed", 2, productDao.count());
        assertNotNull("the first good row survives", productDao.findBySku("CSV-201").orElse(null));
        assertNotNull("the second good row survives", productDao.findBySku("CSV-203").orElse(null));
        assertNull("the broken row is not committed", productDao.findBySku("CSV-202").orElse(null));
    }

    @Test
    public void anUnknownCategoryFailsTheRowWithoutStoppingTheImport() throws Exception {
        Path file = writeCsv(
                "sku,name,category,supplier,unit_price,quantity,reorder_level,unit",
                "CSV-301,Wrong Category,Nonexistent,Acme Supplies,1.00,10,2,piece",
                "CSV-302,Right Category,Hardware,Acme Supplies,1.00,10,2,piece");
        loginAsAdmin();

        List<ImportResult> results = productService.importFromCsv(file);

        assertFalse("the unknown category fails the row", results.get(0).isSuccess());
        assertTrue("the error names the missing category: " + results.get(0).error(),
                results.get(0).error().contains("Category"));
        assertTrue("the following row still imports", results.get(1).isSuccess());
        assertEquals(1, productDao.count());
    }

    @Test
    public void anUnknownSupplierFailsTheRow() throws Exception {
        Path file = writeCsv(
                "CSV-401,Wrong Supplier,Hardware,Nonexistent Supplier,1.00,10,2,piece");
        loginAsAdmin();

        List<ImportResult> results = productService.importFromCsv(file);

        assertEquals(1, results.size());
        assertFalse("the unknown supplier fails the row", results.get(0).isSuccess());
        assertTrue("the error names the missing supplier: " + results.get(0).error(),
                results.get(0).error().contains("Supplier"));
        assertEquals("nothing was committed", 0, productDao.count());
    }

    @Test
    public void aRowWithTooFewColumnsIsReportedRatherThanCrashing() throws Exception {
        Path file = writeCsv(
                "sku,name,category,supplier,unit_price,quantity,reorder_level,unit",
                "CSV-501,Truncated Row,Hardware");
        loginAsAdmin();

        List<ImportResult> results = productService.importFromCsv(file);

        assertEquals(1, results.size());
        assertFalse("the short row fails", results.get(0).isSuccess());
        assertTrue("the error states the column count: " + results.get(0).error(),
                results.get(0).error().contains("column"));
        assertEquals(0, productDao.count());
    }

    @Test
    public void aDuplicateSkuInTheFileFailsTheSecondRow() throws Exception {
        Path file = writeCsv(
                "sku,name,category,supplier,unit_price,quantity,reorder_level,unit",
                "CSV-601,First Copy,Hardware,Acme Supplies,1.00,10,2,piece",
                "CSV-601,Second Copy,Hardware,Acme Supplies,1.00,10,2,piece");
        loginAsAdmin();

        List<ImportResult> results = productService.importFromCsv(file);

        assertTrue("the first copy is imported", results.get(0).isSuccess());
        assertFalse("the duplicate is rejected", results.get(1).isSuccess());
        assertTrue("the error mentions the duplicate SKU: " + results.get(1).error(),
                results.get(1).error().contains("CSV-601"));
        assertEquals("only one product exists for that SKU", 1, productDao.count());
    }

    @Test
    public void anEmptyFileProducesNoResults() throws Exception {
        Path file = writeCsv();
        loginAsAdmin();

        List<ImportResult> results = productService.importFromCsv(file);

        assertTrue("an empty file yields an empty result list", results.isEmpty());
    }

    @Test
    public void theImportRequiresAdministratorPrivileges() throws Exception {
        Path file = writeCsv(
                "sku,name,category,supplier,unit_price,quantity,reorder_level,unit",
                "CSV-701,Staff Attempt,Hardware,Acme Supplies,1.00,10,2,piece");
        loginAsStaff();

        assertThrows("staff must not bulk-import products",
                AuthenticationException.class,
                () -> productService.importFromCsv(file));
        assertEquals("nothing was imported", 0, productDao.count());
    }

    @Test
    public void theTemplateHeaderMatchesTheExpectedColumnOrder() {
        assertEquals("the template matches the parser's column order",
                List.of("sku", "name", "category", "supplier", "unit_price",
                        "quantity", "reorder_level", "unit"),
                List.of(ProductService.csvTemplateHeader()));
    }
}
