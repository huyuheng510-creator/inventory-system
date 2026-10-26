package com.cjlu.inventory.ui.component;

import com.cjlu.inventory.ui.util.Theme;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;
import java.util.Objects;
import java.util.function.Function;

/**
 * Generic combo-box renderer that displays domain objects with a readable
 * label instead of their default {@code toString()} representation.
 *
 * @param <T> item type
 */
public class DisplayRenderer<T> extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    private final Function<T, String> labeler;

    public DisplayRenderer(Function<T, String> labeler) {
        this.labeler = Objects.requireNonNull(labeler, "labeler");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setText(labeler.apply((T) value));
        setFont(Theme.FONT_BASE);
        return this;
    }
}
