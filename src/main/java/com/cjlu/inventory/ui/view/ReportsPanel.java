package com.cjlu.inventory.ui.view;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.model.MovementLine;
import com.cjlu.inventory.model.ReorderSuggestion;
import com.cjlu.inventory.model.ValuationLine;
import com.cjlu.inventory.service.strategy.ReorderPolicy;
import com.cjlu.inventory.ui.component.DisplayRenderer;
import com.cjlu.inventory.ui.controller.ReportController;
import com.cjlu.inventory.ui.table.MovementTableModel;
import com.cjlu.inventory.ui.table.ReorderSuggestionTableModel;
import com.cjlu.inventory.ui.table.ValuationTableModel;
import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;

/**
 * Reports tab: inventory valuation with grand total, stock movement
 * aggregation over a date range and reorder suggestions computed with a
 * runtime-selectable {@code ReorderPolicy} strategy. Each report can be
 * exported to CSV through a file chooser defaulting to {@code data/exports}.
 */
public class ReportsPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ApplicationContext context;
    private final ReportController controller;
    private final ValuationTableModel valuationModel = new ValuationTableModel();
    private final MovementTableModel movementModel = new MovementTableModel();
    private final ReorderSuggestionTableModel reorderModel = new ReorderSuggestionTableModel();
    private final JTable valuationTable = new JTable(valuationModel);
    private final JTable movementTable = new JTable(movementModel);
    private final JTable reorderTable = new JTable(reorderModel);
    private final JLabel valuationTotalLabel = UiUtils.subtitleLabel("Total inventory value: -");
    private final JTextField fromDateField = new JTextField(10);
    private final JTextField toDateField = new JTextField(10);
    private final JComboBox<ReorderPolicy> policyCombo = new JComboBox<>();

    public ReportsPanel(ApplicationContext context) {
        this.context = context;
        setLayout(new BorderLayout(0, 12));
        setOpaque(true);
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        styleReportTable(valuationTable, new int[]{90, 230, 150, 70, 110, 120});
        styleReportTable(movementTable, new int[]{90, 260, 90, 90, 100, 110});
        styleReportTable(reorderTable, new int[]{90, 260, 140, 100, 110, 110});

        LocalDate today = LocalDate.now();
        fromDateField.setText(UiUtils.formatDate(today.minusDays(30)));
        toDateField.setText(UiUtils.formatDate(today));

        for (ReorderPolicy policy : context.getReorderPolicies()) {
            policyCombo.addItem(policy);
        }
        policyCombo.setRenderer(new DisplayRenderer<ReorderPolicy>(
                policy -> policy == null ? "" : policy.getName()));
        if (policyCombo.getItemCount() > 0) {
            policyCombo.setSelectedIndex(0);
        }
        policyCombo.setPreferredSize(new Dimension(180, 28));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);

        controller = new ReportController(context, this);
        controller.initialize();
    }

    private void styleReportTable(JTable table, int[] widths) {
        UiUtils.styleTable(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        for (int index = 0; index < widths.length; index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(UiUtils.titleLabel("Reports"));
        titles.add(Box.createVerticalStrut(2));
        titles.add(UiUtils.subtitleLabel(
                "Inventory valuation, stock movement and reorder suggestions with CSV export."));
        header.add(titles, BorderLayout.WEST);
        JButton refreshButton = UiUtils.primaryButton("Refresh All");
        refreshButton.addActionListener(action -> controller.refreshAll());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshButton);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT_BOLD);
        tabs.addTab("Valuation", buildValuationTab());
        tabs.addTab("Movement", buildMovementTab());
        tabs.addTab("Reorder Suggestions", buildReorderTab());
        return tabs;
    }

    private JPanel buildValuationTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JButton exportButton = UiUtils.button("Export CSV...");
        exportButton.addActionListener(action -> controller.exportValuation());
        valuationTotalLabel.setFont(Theme.FONT_BOLD);
        valuationTotalLabel.setForeground(Theme.PRIMARY);
        toolbar.add(valuationTotalLabel, BorderLayout.WEST);
        toolbar.add(exportButton, BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(UiUtils.titledCard("Inventory Valuation", UiUtils.scroll(valuationTable)),
                BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildMovementTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        JButton applyButton = UiUtils.toolbarButton("Apply Date Range");
        applyButton.addActionListener(action -> controller.refreshMovement());
        JLabel hint = UiUtils.subtitleLabel("Stock-in, stock-out and net change per product");
        filters.add(UiUtils.mutedLabel("From (yyyy-MM-dd):"));
        filters.add(fromDateField);
        filters.add(UiUtils.mutedLabel("To (yyyy-MM-dd):"));
        filters.add(toDateField);
        filters.add(applyButton);
        filters.add(hint);
        JButton exportButton = UiUtils.button("Export CSV...");
        exportButton.addActionListener(action -> controller.exportMovement());
        toolbar.add(filters, BorderLayout.WEST);
        toolbar.add(exportButton, BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(UiUtils.titledCard("Stock Movement", UiUtils.scroll(movementTable)),
                BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildReorderTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        JButton applyButton = UiUtils.toolbarButton("Compute Suggestions");
        applyButton.addActionListener(action -> controller.refreshReorders());
        filters.add(UiUtils.mutedLabel("Reorder policy:"));
        filters.add(policyCombo);
        filters.add(applyButton);
        JButton exportButton = UiUtils.button("Export CSV...");
        exportButton.addActionListener(action -> controller.exportReorderSuggestions());
        toolbar.add(filters, BorderLayout.WEST);
        toolbar.add(exportButton, BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(UiUtils.titledCard("Reorder Suggestions", UiUtils.scroll(reorderTable)),
                BorderLayout.CENTER);
        panel.add(UiUtils.subtitleLabel(
                "Only products at or below their reorder level are listed. The suggested quantity "
                        + "is calculated by the selected strategy."), BorderLayout.SOUTH);
        return panel;
    }

    /** Reloads all three reports. */
    public void refresh() {
        controller.refreshAll();
    }

    // ------------------------------------------------- view API for controller

    public void setValuation(List<ValuationLine> lines, java.math.BigDecimal total) {
        valuationModel.setLines(lines);
        valuationTotalLabel.setText("Total inventory value: " + UiUtils.formatMoney(total)
                + "   (" + lines.size() + " product(s))");
    }

    public void setMovement(List<MovementLine> lines) {
        movementModel.setLines(lines);
    }

    public void setSuggestions(List<ReorderSuggestion> suggestions) {
        reorderModel.setSuggestions(suggestions);
    }

    public List<ValuationLine> getValuationLines() {
        return valuationModel.getLines();
    }

    public String getMovementFromText() {
        return fromDateField.getText();
    }

    public String getMovementToText() {
        return toDateField.getText();
    }

    public ReorderPolicy getSelectedPolicy() {
        Object selected = policyCombo.getSelectedItem();
        return selected instanceof ReorderPolicy policy ? policy : null;
    }
}
