package com.cjlu.inventory.ui.mediator;

/**
 * The kinds of cross-panel change a view can report to the
 * {@link ViewMediator}.
 *
 * <p>Deliberately coarse-grained: a panel states the business fact ("stock
 * changed"), not which other panel should refresh. The mapping from fact to
 * affected panels is the mediator's job, so this enum stays stable when panels
 * are added or removed.</p>
 */
public enum ViewEvent {

    /** A stock-in, stock-out or adjustment was committed. */
    STOCK_CHANGED,

    /** A product was created, edited, deleted or bulk-imported. */
    CATALOGUE_CHANGED,

    /** A purchase order was created, submitted, received or cancelled. */
    PURCHASE_ORDER_CHANGED,

    /** A category, supplier or user account was created or edited. */
    MASTER_DATA_CHANGED
}
