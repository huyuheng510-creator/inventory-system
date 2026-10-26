package com.cjlu.inventory.ui.controller;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.exception.InventoryException;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.PurchaseOrder;
import com.cjlu.inventory.model.PurchaseOrderStatus;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.ui.util.UiUtils;
import com.cjlu.inventory.ui.view.PurchaseOrderPanel;
import com.cjlu.inventory.ui.view.dialog.PurchaseOrderDialog;
import com.cjlu.inventory.ui.view.dialog.ReceiveDialog;

import javax.swing.JOptionPane;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Purchase-order controller: list/filter, create drafts (manual or generated
 * from low-stock products), submit and cancel (administrator), receive
 * deliveries (including partial receipts) and the order-detail panel.
 */
public final class PurchaseOrderController {

    private final ApplicationContext context;
    private final PurchaseOrderPanel panel;
    private Map<String, String> supplierNames = new HashMap<>();
    private Map<String, String> productSkus = new HashMap<>();
    private Map<String, String> productNames = new HashMap<>();

    public PurchaseOrderController(ApplicationContext context, PurchaseOrderPanel panel) {
        this.context = Objects.requireNonNull(context, "context");
        this.panel = Objects.requireNonNull(panel, "panel");
    }

    /** Initial load of the order table. */
    public void initialize() {
        refresh();
    }

    /** Reloads lookups and the purchase orders for the current status filter. */
    public void refresh() {
        try {
            reloadLookups();
            PurchaseOrderStatus status = panel.getStatusFilter();
            List<PurchaseOrder> orders = status == null
                    ? context.getPurchaseOrderService().listOrders()
                    : context.getPurchaseOrderService().listOrders(status);
            panel.setOrders(orders, supplierNames);
            if (orders.isEmpty()) {
                panel.clearOrderDetails();
            } else {
                panel.selectOrderById(orders.get(0).getId());
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "load the purchase orders", failure);
        }
    }

    /** Opens the manual draft-creation dialog. */
    public void createDraft() {
        try {
            List<Supplier> suppliers = context.getCatalogService().listSuppliers();
            List<Product> products = context.getProductService().listProducts();
            if (suppliers.isEmpty() || products.isEmpty()) {
                UiUtils.info(panel, "Create at least one supplier and one product in the "
                        + "Administration / Products tabs before creating a purchase order.");
                return;
            }
            PurchaseOrderDialog dialog = new PurchaseOrderDialog(UiUtils.windowOf(panel),
                    suppliers, products, data -> context.getPurchaseOrderService().createDraft(
                            data.supplierId(), data.expectedDate(), data.note(), data.lines()));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refresh();
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "open the purchase-order draft dialog", failure);
        }
    }

    /** Generates a draft from all low-stock products of a chosen supplier. */
    public void generateFromLowStock() {
        try {
            List<Supplier> suppliers = context.getCatalogService().listSuppliers();
            if (suppliers.isEmpty()) {
                UiUtils.info(panel, "Create at least one supplier first (Administration tab).");
                return;
            }
            String[] options = suppliers.stream().map(Supplier::getName).toArray(String[]::new);
            String choice = (String) JOptionPane.showInputDialog(panel,
                    "Select the supplier whose low-stock products should be ordered:",
                    "Generate Draft from Low Stock", JOptionPane.QUESTION_MESSAGE, null,
                    options, options[0]);
            if (choice == null) {
                return;
            }
            Supplier supplier = suppliers.stream()
                    .filter(candidate -> candidate.getName().equals(choice))
                    .findFirst()
                    .orElse(null);
            if (supplier == null) {
                return;
            }
            PurchaseOrder order = context.getPurchaseOrderService()
                    .createReplenishmentDraft(supplier.getId());
            refresh();
            panel.selectOrderById(order.getId());
            UiUtils.info(panel, "Draft " + order.getOrderNumber() + " created for " + choice
                    + " with " + order.getLines().size() + " product line(s).");
        } catch (InventoryException failure) {
            UiUtils.handle(panel, "generate the replenishment draft", failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "generate the replenishment draft", failure);
        }
    }

    /** Submits the selected draft to the supplier (administrator only). */
    public void submitSelected() {
        PurchaseOrder order = requireSelectedOrder();
        if (order == null || !requireAdmin()) {
            return;
        }
        if (!UiUtils.confirm(panel, "Submit purchase order " + order.getOrderNumber()
                + " to the supplier?")) {
            return;
        }
        try {
            context.getPurchaseOrderService().submit(order.getId());
            refresh();
            panel.selectOrderById(order.getId());
        } catch (InventoryException failure) {
            UiUtils.handle(panel, "submit the purchase order", failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "submit the purchase order", failure);
        }
    }

    /** Cancels the selected draft or ordered purchase order (administrator only). */
    public void cancelSelected() {
        PurchaseOrder order = requireSelectedOrder();
        if (order == null || !requireAdmin()) {
            return;
        }
        if (!UiUtils.confirm(panel, "Cancel purchase order " + order.getOrderNumber() + "?")) {
            return;
        }
        try {
            context.getPurchaseOrderService().cancel(order.getId());
            refresh();
            panel.selectOrderById(order.getId());
        } catch (InventoryException failure) {
            UiUtils.handle(panel, "cancel the purchase order", failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "cancel the purchase order", failure);
        }
    }

    /** Receives the selected order, fully or partially. */
    public void receiveSelected() {
        PurchaseOrder selected = requireSelectedOrder();
        if (selected == null) {
            return;
        }
        try {
            PurchaseOrder order = context.getPurchaseOrderService().requireOrder(selected.getId());
            if (order.getStatus() != PurchaseOrderStatus.ORDERED
                    && order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
                UiUtils.info(panel, "Only ordered or partially received purchase orders can be "
                        + "received. Submit the draft first.");
                return;
            }
            reloadLookups();
            ReceiveDialog dialog = new ReceiveDialog(UiUtils.windowOf(panel), order,
                    supplierNames.getOrDefault(order.getSupplierId(), ""), productSkus,
                    productNames, quantities -> context.getPurchaseOrderService()
                            .receive(order.getId(), quantities));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refresh();
                panel.selectOrderById(order.getId());
            }
        } catch (InventoryException failure) {
            UiUtils.handle(panel, "receive the purchase order", failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "receive the purchase order", failure);
        }
    }

    /** Called by the view whenever another order row is selected. */
    public void onOrderSelected(PurchaseOrder order) {
        if (order == null) {
            panel.clearOrderDetails();
            return;
        }
        try {
            panel.setOrderDetails(order, productSkus, productNames);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "show the purchase-order details", failure);
        }
    }

    private void reloadLookups() {
        List<Supplier> suppliers = context.getCatalogService().listSuppliers();
        Map<String, String> newSupplierNames = new HashMap<>();
        for (Supplier supplier : suppliers) {
            newSupplierNames.put(supplier.getId(), supplier.getName());
        }
        supplierNames = newSupplierNames;

        List<Product> products = context.getProductService().listProducts();
        Map<String, String> newProductSkus = new HashMap<>();
        Map<String, String> newProductNames = new HashMap<>();
        for (Product product : products) {
            newProductSkus.put(product.getId(), product.getSku());
            newProductNames.put(product.getId(), product.getName());
        }
        productSkus = newProductSkus;
        productNames = newProductNames;
    }

    private boolean requireAdmin() {
        if (!context.getAuthService().isAdmin()) {
            UiUtils.warn(panel, "This action requires administrator privileges.");
            return false;
        }
        return true;
    }

    private PurchaseOrder requireSelectedOrder() {
        PurchaseOrder selected = panel.getSelectedOrder();
        if (selected == null) {
            UiUtils.info(panel, "Select a purchase order in the table first.");
        }
        return selected;
    }
}
