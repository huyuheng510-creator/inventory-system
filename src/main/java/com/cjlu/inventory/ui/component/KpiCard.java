package com.cjlu.inventory.ui.component;

import com.cjlu.inventory.ui.util.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

/**
 * A dashboard KPI tile: a coloured accent strip, a caption, a large value and
 * a small explanatory hint.
 */
public class KpiCard extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel valueLabel;
    private final JLabel hintLabel;

    /**
     * @param caption short KPI name (upper-cased automatically)
     * @param value   initial value
     * @param hint    one-line explanation
     * @param accent  accent colour of the left strip and the value
     */
    public KpiCard(String caption, String value, String hint, Color accent) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        JPanel accentStrip = new JPanel();
        accentStrip.setBackground(accent);
        accentStrip.setPreferredSize(new Dimension(5, 10));
        add(accentStrip, BorderLayout.WEST);

        JLabel captionLabel = new JLabel(caption.toUpperCase());
        captionLabel.setFont(Theme.FONT_SMALL_BOLD);
        captionLabel.setForeground(Theme.MUTED);

        valueLabel = new JLabel(value);
        valueLabel.setFont(Theme.FONT_KPI);
        valueLabel.setForeground(accent);

        hintLabel = new JLabel(hint);
        hintLabel.setFont(Theme.FONT_SMALL);
        hintLabel.setForeground(Theme.MUTED);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 12));
        content.add(captionLabel);
        content.add(Box.createVerticalStrut(4));
        content.add(valueLabel);
        content.add(Box.createVerticalStrut(2));
        content.add(hintLabel);
        add(content, BorderLayout.CENTER);

        setPreferredSize(new Dimension(230, 96));
        setMinimumSize(new Dimension(180, 96));
    }

    /** Updates the displayed KPI value. */
    public void setValue(String value) {
        valueLabel.setText(value == null ? "-" : value);
    }

    /** Updates the explanatory hint. */
    public void setHint(String hint) {
        hintLabel.setText(hint == null ? "" : hint);
    }
}
