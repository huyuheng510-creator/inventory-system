package com.cjlu.inventory.ui.view.dialog;

import com.cjlu.inventory.exception.InventoryException;
import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;

/**
 * Common base for the modal input dialogs of the GUI.
 *
 * <p>The base class builds the standard padded layout with a Cancel/Confirm
 * button row, wires Escape to cancel and Enter to confirm, and turns checked
 * business failures into user-facing message dialogs while keeping the form
 * open so the user can correct the input.</p>
 */
public abstract class BaseDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private boolean saved;
    private JButton confirmButton;
    private JButton cancelButton;

    protected BaseDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    /**
     * Builds the standard dialog layout. Subclasses call this once at the end
     * of their constructor.
     *
     * @param content     main form content
     * @param confirmText label of the confirm button
     * @param onConfirm   action invoked by the confirm button
     */
    protected final void initialize(JComponent content, String confirmText, Runnable onConfirm) {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Theme.BACKGROUND);
        root.setOpaque(true);
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        root.add(content, BorderLayout.CENTER);

        confirmButton = UiUtils.primaryButton(confirmText);
        cancelButton = UiUtils.button("Cancel");
        confirmButton.addActionListener(action -> onConfirm.run());
        cancelButton.addActionListener(action -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(confirmButton);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(confirmButton);
        getRootPane().registerKeyboardAction(action -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Runs a business action, shows any failure and closes on success. */
    protected final void execute(String actionPhrase, BusinessTask task) {
        confirmButton.setEnabled(false);
        try {
            task.run();
            saved = true;
            dispose();
        } catch (InventoryException failure) {
            UiUtils.handle(this, actionPhrase, failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(this, actionPhrase, failure);
        } finally {
            confirmButton.setEnabled(true);
        }
    }

    /** Applies the given size and centres the dialog on its owner. */
    protected final void centerWithSize(int width, int height) {
        setSize(width, height);
        setLocationRelativeTo(getOwner());
    }

    /** Packs the dialog and centres it on its owner. */
    protected final void centerOnOwner() {
        pack();
        setLocationRelativeTo(getOwner());
    }

    /** True when the form was submitted and persisted successfully. */
    public final boolean isSaved() {
        return saved;
    }

    /** Confirm button, exposed for subclasses that need custom wiring. */
    protected final JButton confirmButton() {
        return confirmButton;
    }

    /** Cancel button, exposed for subclasses that need custom wiring. */
    protected final JButton cancelButton() {
        return cancelButton;
    }

    /** A business action that may fail with a user-facing error. */
    @FunctionalInterface
    protected interface BusinessTask {
        void run() throws InventoryException;
    }
}
