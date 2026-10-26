package com.cjlu.inventory.ui.view.dialog;

import com.cjlu.inventory.service.ProductService;
import com.cjlu.inventory.service.ProductService.ImportResult;
import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Dialog for bulk-importing products from a CSV file (FR-2 extension).
 *
 * <p>The user browses for a CSV file; the import runs row-by-row and the
 * results — successes and failures — are shown in a table. Each bad row is
 * skipped without rolling back the good ones already committed.</p>
 *
 * <p>Expected CSV columns (with header row): sku, name, category, supplier,
 * unit_price, quantity, reorder_level, unit.</p>
 */
public class CsvImportDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final ProductService productService;
    private final JTextField fileField = new JTextField();
    private final ResultTableModel resultModel = new ResultTableModel();
    private final JTable resultTable = new JTable(resultModel);
    private final JLabel summaryLabel = UiUtils.subtitleLabel("No file imported yet.");
    private boolean imported = false;

    public CsvImportDialog(Window owner, ProductService productService) {
        super(owner, "Import Products from CSV", ModalityType.APPLICATION_MODAL);
        this.productService = Objects.requireNonNull(productService, "productService");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(680, 460));
        setSize(740, 500);
        setLocationRelativeTo(owner);

        getContentPane().setBackground(Theme.BACKGROUND);
        getContentPane().setLayout(new BorderLayout(0, 12));
        ((JPanel) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(16, 18, 14, 18));

        getContentPane().add(buildFileRow(), BorderLayout.NORTH);
        getContentPane().add(buildResultPanel(), BorderLayout.CENTER);
        getContentPane().add(buildFooter(), BorderLayout.SOUTH);
    }

    /** Returns {@code true} when at least one product was imported successfully. */
    public boolean isImported() {
        return imported;
    }

    // ------------------------------------------------------------- UI build

    private JPanel buildFileRow() {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JLabel hint = new JLabel("CSV file (header: sku, name, category, supplier, "
                + "unit_price, quantity, reorder_level, unit):");
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.MUTED);
        row.add(hint, BorderLayout.NORTH);

        JPanel fileRow = new JPanel(new BorderLayout(6, 0));
        fileRow.setOpaque(false);
        fileRow.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        fileField.setEditable(false);
        fileField.setFont(Theme.FONT_BASE);
        fileField.setPreferredSize(new Dimension(400, 28));
        fileRow.add(fileField, BorderLayout.CENTER);

        JButton browseButton = UiUtils.button("Browse...");
        browseButton.addActionListener(e -> browseForFile());

        JButton importButton = UiUtils.primaryButton("Import");
        importButton.addActionListener(e -> runImport());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(browseButton);
        buttons.add(importButton);
        fileRow.add(buttons, BorderLayout.EAST);
        row.add(fileRow, BorderLayout.CENTER);

        return row;
    }

    private JPanel buildResultPanel() {
        UiUtils.styleTable(resultTable);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        int[] widths = {50, 100, 200, 50, 280};
        for (int i = 0; i < widths.length; i++) {
            resultTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(UiUtils.sectionTitle("Import Results"), BorderLayout.NORTH);
        panel.add(UiUtils.scroll(resultTable), BorderLayout.CENTER);
        panel.add(summaryLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildFooter() {
        JButton downloadTemplateButton = UiUtils.button("Download Template");
        downloadTemplateButton.setToolTipText("Save a blank CSV template to your exports folder");
        downloadTemplateButton.addActionListener(e -> saveTemplate());

        JButton closeButton = UiUtils.button("Close");
        closeButton.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        footer.add(downloadTemplateButton);
        footer.add(closeButton);
        return footer;
    }

    // ------------------------------------------------------------ behaviour

    private void browseForFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a CSV file to import");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fileField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void runImport() {
        String pathText = fileField.getText().trim();
        if (pathText.isEmpty()) {
            UiUtils.warn(this, "Please select a CSV file first.");
            return;
        }
        Path csvFile = Path.of(pathText);
        try {
            List<ImportResult> results = productService.importFromCsv(csvFile);
            resultModel.setResults(results);
            long ok = results.stream().filter(ImportResult::isSuccess).count();
            long fail = results.size() - ok;
            summaryLabel.setText(ok + " product(s) imported successfully, "
                    + fail + " row(s) skipped.");
            if (ok > 0) {
                imported = true;
            }
            if (results.isEmpty()) {
                UiUtils.info(this, "The file appears to be empty (no data rows found).");
            }
        } catch (com.cjlu.inventory.exception.AuthenticationException authEx) {
            UiUtils.error(this, "Administrator privileges are required to import products.");
        } catch (IOException ioEx) {
            UiUtils.error(this, "Could not read the file:\n" + ioEx.getMessage());
        } catch (RuntimeException failure) {
            UiUtils.handle(this, "import the CSV file", failure);
        }
    }

    private void saveTemplate() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save CSV Template");
        chooser.setSelectedFile(new java.io.File("products_template.csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = chooser.getSelectedFile().toPath();
        try {
            com.cjlu.inventory.util.CsvUtils.write(target,
                    ProductService.csvTemplateHeader(),
                    List.<String[]>of(new String[]{"SKU-001", "Example Product", "Electronics",
                            "Supplier Name", "99.00", "100", "10", "pcs"}));
            UiUtils.info(this, "Template saved to:\n" + target);
        } catch (IOException ioEx) {
            UiUtils.error(this, "Could not save the template:\n" + ioEx.getMessage());
        }
    }

    // -------------------------------------------------------- table model

    private static final class ResultTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNS = {"Row", "SKU", "Name / Detail", "OK?", "Message"};

        private List<ImportResult> results = List.of();

        void setResults(List<ImportResult> results) {
            this.results = List.copyOf(results);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return results.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }
        @Override public Class<?> getColumnClass(int col) {
            return col == 3 ? Boolean.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int col) {
            ImportResult r = results.get(row);
            return switch (col) {
                case 0 -> String.valueOf(r.rowNumber());
                case 1 -> r.sku();
                case 2 -> r.isSuccess() ? r.product().getName() : "";
                case 3 -> r.isSuccess();
                case 4 -> r.isSuccess() ? "Imported" : r.error();
                default -> "";
            };
        }
    }
}
