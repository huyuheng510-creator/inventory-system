package com.cjlu.inventory.ui.mediator;

/**
 * Mediator pattern: the single point through which the GUI panels communicate.
 *
 * <p>Without a mediator every panel would need a reference to every other panel
 * it can affect — a stock movement changes the dashboard KPIs, the product
 * table and the reorder suggestions; receiving a purchase order changes the
 * stock table and the dashboard. That is an O(n²) web of direct references
 * between sibling views.</p>
 *
 * <p>Instead, each panel reports <em>what happened</em> ({@link ViewEvent}) to
 * this mediator and the mediator decides <em>who needs to know</em>. Panels
 * therefore have no knowledge of each other, and a new panel can be added by
 * registering it with the mediator rather than by editing every existing
 * panel.</p>
 *
 * <p>{@code MainFrame} implements this interface because it is the component
 * that actually owns all the panels; the panels only ever see the interface.</p>
 */
public interface ViewMediator {

    /**
     * Something changed in one panel that other panels may need to react to.
     *
     * @param source the panel reporting the change (never notified back)
     * @param event  what kind of change occurred
     */
    void notifyChange(Object source, ViewEvent event);

    /**
     * Shows a message in the application status bar.
     *
     * @param message the text to display
     */
    void showStatus(String message);
}
