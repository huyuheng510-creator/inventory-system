package com.cjlu.inventory.ui.component;

import com.cjlu.inventory.ui.util.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

/**
 * Bottom status bar of the main window: a left-aligned status message (stock
 * alerts, confirmations) and a right-aligned environment hint.
 */
public class StatusBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel messageLabel = new JLabel("Ready");
    private final JLabel infoLabel = new JLabel();

    public StatusBar() {
        setLayout(new BorderLayout(12, 0));
        setOpaque(true);
        setBackground(Theme.STATUS_BAR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        setPreferredSize(new Dimension(10, 30));
        messageLabel.setFont(Theme.FONT_SMALL);
        messageLabel.setForeground(Theme.TEXT);
        infoLabel.setFont(Theme.FONT_SMALL);
        infoLabel.setForeground(Theme.MUTED);
        add(messageLabel, BorderLayout.CENTER);
        add(infoLabel, BorderLayout.EAST);
    }

    /** Shows a neutral status message. */
    public void setMessage(String message) {
        setMessage(message, Theme.TEXT);
    }

    /** Shows a coloured status message. */
    public void setMessage(String message, Color color) {
        messageLabel.setText(message == null ? "" : message);
        messageLabel.setForeground(color == null ? Theme.TEXT : color);
    }

    /** Sets the permanent right-aligned info text. */
    public void setInfo(String text) {
        infoLabel.setText(text == null ? "" : text);
    }
}
