package com.cjlu.inventory.ui.controller;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.exception.InventoryException;
import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.MovementLine;
import com.cjlu.inventory.model.ReorderSuggestion;
import com.cjlu.inventory.model.ValuationLine;
import com.cjlu.inventory.service.strategy.ReorderPolicy;
import com.cjlu.inventory.ui.util.UiUtils;
import com.cjlu.inventory.ui.view.ReportsPanel;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Reporting controller: loads the valuation, movement and reorder-suggestion
 * reports from {@code ReportService} and exports them to CSV files chosen with
 * a {@link JFileChooser} that starts in {@code data/exports}.
 */
public final class ReportController {

    private final ApplicationContext context;
    private final ReportsPanel panel;

    public ReportController(ApplicationContext context, ReportsPanel panel) {
        this.context = Objects.requireNonNull(context, "context");
        this.panel = Objects.requireNonNull(panel, "panel");
    }

    /** Initial load of all three reports. */
    public void initialize() {
        refreshAll();
    }

    /** Reloads every report with the currently selected dates/policy. */
    public void refreshAll() {
        refreshValuation();
        refreshMovement();
        refreshReorders();
    }

    /** Loads the inventory valuation report and its grand total. */
    public void refreshValuation() {
        try {
            List<ValuationLine> lines = context.getReportService().inventoryValuation();
            BigDecimal total = lines.stream()
                    .map(ValuationLine::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            panel.setValuation(lines, total);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "load the inventory valuation report", failure);
        }
    }

    /** Loads the stock movement report for the dates currently entered. */
    public void refreshMovement() {
        try {
            panel.setMovement(loadMovementLines());
        } catch (InventoryException failure) {
            UiUtils.handle(panel, "load the stock movement report", failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "load the stock movement report", failure);
        }
    }

    /** Loads the reorder suggestions for the selected strategy. */
    public void refreshReorders() {
        try {
            ReorderPolicy policy = panel.getSelectedPolicy();
            if (policy == null) {
                UiUtils.info(panel, "Select a reorder policy first.");
                return;
            }
            panel.setSuggestions(context.getReportService().reorderSuggestions(policy));
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "load the reorder suggestions", failure);
        }
    }

    /** Exports the valuation table as CSV. */
    public void exportValuation() {
        try {
            List<ValuationLine> lines = panel.getValuationLines();
            Path file = chooseExportFile("inventory_valuation");
            if (file == null) {
                return;
            }
            context.getReportService().exportValuation(file, lines);
            UiUtils.info(panel, "Valuation report exported to:\n" + file.toAbsolutePath());
        } catch (InventoryException failure) {
            UiUtils.handle(panel, "export the valuation report", failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "export the valuation report", failure);
        }
    }

    /** Exports the stock movement report as CSV. */
    public void exportMovement() {
        try {
            List<MovementLine> lines = loadMovementLines();
            Path file = chooseExportFile("stock_movement");
            if (file == null) {
                return;
            }
            context.getReportService().exportMovement(file, lines);
            UiUtils.info(panel, "Stock movement report exported to:\n" + file.toAbsolutePath());
        } catch (InventoryException failure) {
            UiUtils.handle(panel, "export the stock movement report", failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "export the stock movement report", failure);
        }
    }

    /** Exports the reorder suggestions as CSV. */
    public void exportReorderSuggestions() {
        try {
            ReorderPolicy policy = panel.getSelectedPolicy();
            if (policy == null) {
                UiUtils.info(panel, "Select a reorder policy first.");
                return;
            }
            List<ReorderSuggestion> suggestions = context.getReportService()
                    .reorderSuggestions(policy);
            Path file = chooseExportFile("reorder_suggestions");
            if (file == null) {
                return;
            }
            context.getReportService().exportReorderSuggestions(file, suggestions);
            UiUtils.info(panel, "Reorder suggestions exported to:\n" + file.toAbsolutePath());
        } catch (InventoryException failure) {
            UiUtils.handle(panel, "export the reorder suggestions", failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "export the reorder suggestions", failure);
        }
    }

    private List<MovementLine> loadMovementLines() throws InventoryException {
        LocalDate from = UiUtils.parseDate(panel.getMovementFromText(), "From date");
        LocalDate to = UiUtils.parseDate(panel.getMovementToText(), "To date");
        if (from.isAfter(to)) {
            throw new ValidationException("The From date must be on or before the To date.");
        }
        return context.getReportService().stockMovement(from.atStartOfDay(),
                to.plusDays(1).atStartOfDay());
    }

    private Path chooseExportFile(String prefix) {
        Path defaultPath = context.getReportService().newExportPath(prefix);
        try {
            Files.createDirectories(context.getExportDirectory());
        } catch (IOException ignored) {
            // The file chooser still works; the export reports write failures itself.
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export CSV Report");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setCurrentDirectory(context.getExportDirectory().toFile());
        chooser.setSelectedFile(defaultPath.toFile());
        if (chooser.showSaveDialog(panel) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        Path chosen = chooser.getSelectedFile().toPath();
        if (!chosen.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            chosen = chosen.resolveSibling(chosen.getFileName() + ".csv");
        }
        if (Files.exists(chosen) && !UiUtils.confirm(panel,
                "The file already exists:\n" + chosen.toAbsolutePath() + "\n\nOverwrite it?")) {
            return null;
        }
        return chosen;
    }
}
