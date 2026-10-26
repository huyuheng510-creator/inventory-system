package com.cjlu.inventory.ui.view.dialog;

import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.ui.component.DisplayRenderer;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.List;

/**
 * Create / edit product form. The dialog validates the syntax of every field
 * and hands a typed {@link FormData} object to the controller, which performs
 * the actual service call and reports business errors.
 */
public class ProductDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    /** Typed form values collected by the dialog. */
    public record FormData(String sku, String name, String categoryId, String supplierId,
                           BigDecimal unitPrice, int quantity, int reorderLevel, String unit) {
    }

    private final boolean editMode;
    private final Product existing;
    private final JTextField skuField = new JTextField(22);
    private final JTextField nameField = new JTextField(22);
    private final JComboBox<Category> categoryCombo;
    private final JComboBox<Supplier> supplierCombo;
    private final JTextField unitPriceField = new JTextField(12);
    private final JTextField quantityField = new JTextField(12);
    private final JTextField reorderLevelField = new JTextField(12);
    private final JTextField unitField = new JTextField(12);

    public ProductDialog(Window owner, boolean editMode, Product existing,
                         List<Category> categories, List<Supplier> suppliers,
                         DialogHandler<FormData> handler) {
        super(owner, editMode ? "Edit Product" : "New Product");
        this.editMode = editMode;
        this.existing = existing;

        categoryCombo = new JComboBox<>(categories.toArray(new Category[0]));
        categoryCombo.setRenderer(new DisplayRenderer<Category>(
                category -> category == null ? "" : category.getName()));
        supplierCombo = new JComboBox<>(suppliers.toArray(new Supplier[0]));
        supplierCombo.setRenderer(new DisplayRenderer<Supplier>(
                supplier -> supplier == null ? "Unknown supplier" : supplier.getName()));

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);

        JPanel form = UiUtils.formPanel();
        UiUtils.addFormRow(form, "SKU *", skuField);
        UiUtils.addFormRow(form, "Name *", nameField);
        UiUtils.addFormRow(form, "Category *", categoryCombo);
        UiUtils.addFormRow(form, "Supplier *", supplierCombo);
        UiUtils.addFormRow(form, "Unit Price (¥) *", unitPriceField);
        if (editMode) {
            JLabel quantityLabel = UiUtils.mutedLabel(existing == null ? "-"
                    : existing.getQuantity() + " " + existing.getUnit()
                    + " (changed only through Stock operations)");
            UiUtils.addFormRow(form, "Current Quantity", quantityLabel);
        } else {
            UiUtils.addFormRow(form, "Initial Quantity *", quantityField);
        }
        UiUtils.addFormRow(form, "Reorder Level *", reorderLevelField);
        UiUtils.addFormRow(form, "Unit *", unitField);

        content.add(form, BorderLayout.CENTER);
        content.add(UiUtils.subtitleLabel(
                "Fields marked * are required. Changes are recorded against your user account."),
                BorderLayout.SOUTH);

        if (editMode && existing != null) {
            skuField.setText(existing.getSku());
            skuField.setEditable(false);
            nameField.setText(existing.getName());
            UiUtils.selectById(categoryCombo, existing.getCategoryId());
            UiUtils.selectById(supplierCombo, existing.getSupplierId());
            unitPriceField.setText(existing.getUnitPrice().toPlainString());
            reorderLevelField.setText(Integer.toString(existing.getReorderLevel()));
            unitField.setText(existing.getUnit());
        } else {
            unitField.setText("piece");
            if (categoryCombo.getItemCount() > 0) {
                categoryCombo.setSelectedIndex(0);
            }
            if (supplierCombo.getItemCount() > 0) {
                supplierCombo.setSelectedIndex(0);
            }
        }

        initialize(content, editMode ? "Save Changes" : "Create Product",
                () -> execute(editMode ? "update the product" : "create the product",
                        () -> handler.save(readForm())));
        centerOnOwner();
    }

    private FormData readForm() throws ValidationException {
        String sku = skuField.getText();
        String name = nameField.getText();
        Category category = (Category) categoryCombo.getSelectedItem();
        Supplier supplier = (Supplier) supplierCombo.getSelectedItem();
        if (category == null) {
            throw new ValidationException("Please select a category.");
        }
        if (supplier == null) {
            throw new ValidationException("Please select a supplier.");
        }
        BigDecimal unitPrice = UiUtils.parseDecimal(unitPriceField.getText(), "Unit price");
        int reorderLevel = UiUtils.parseInt(reorderLevelField.getText(), "Reorder level");
        int quantity;
        if (editMode) {
            quantity = existing == null ? 0 : existing.getQuantity();
        } else {
            quantity = UiUtils.parseInt(quantityField.getText(), "Initial quantity");
        }
        return new FormData(sku, name, category.getId(), supplier.getId(), unitPrice,
                quantity, reorderLevel, unitField.getText());
    }
}
