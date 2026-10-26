package com.cjlu.inventory.ui.component;

import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Self-drawn bar chart (plain {@link JPanel#paintComponent(Graphics)}, no
 * third-party library) that shows the inventory value per category on the
 * dashboard.
 */
public class BarChartPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int LEFT_MARGIN = 64;
    private static final int RIGHT_MARGIN = 18;
    private static final int TOP_MARGIN = 22;
    private static final int BOTTOM_MARGIN = 48;
    private static final int Y_GRID_LINES = 4;

    private Map<String, BigDecimal> data = new LinkedHashMap<>();

    public BarChartPanel() {
        setOpaque(true);
        setBackground(Theme.CARD);
        setPreferredSize(new java.awt.Dimension(420, 250));
    }

    /** Replaces the chart data and repaints. */
    public void setData(Map<String, BigDecimal> values) {
        this.data = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
        repaint();
    }

    /** Current chart data (unmodifiable view). */
    public Map<String, BigDecimal> getData() {
        return Map.copyOf(data);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            int chartWidth = width - LEFT_MARGIN - RIGHT_MARGIN;
            int chartHeight = height - TOP_MARGIN - BOTTOM_MARGIN;
            if (chartWidth <= 20 || chartHeight <= 20) {
                return;
            }
            if (data.isEmpty()) {
                g2.setColor(Theme.MUTED);
                g2.setFont(Theme.FONT_BASE);
                String message = "No category data available";
                FontMetrics metrics = g2.getFontMetrics();
                g2.drawString(message, (width - metrics.stringWidth(message)) / 2, height / 2);
                return;
            }

            BigDecimal maximum = data.values().stream()
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ONE);
            double maxValue = Math.max(maximum.doubleValue(), 1.0);

            drawGrid(g2, chartWidth, chartHeight, maximum);

            List<Map.Entry<String, BigDecimal>> entries = List.copyOf(data.entrySet());
            int slotWidth = chartWidth / entries.size();
            int barWidth = Math.max(14, Math.min(56, (int) (slotWidth * 0.55)));
            Font labelFont = Theme.FONT_SMALL.deriveFont(Font.PLAIN, 10.5f);
            Font valueFont = Theme.FONT_SMALL_BOLD.deriveFont(Font.PLAIN, 10.5f);
            for (int index = 0; index < entries.size(); index++) {
                Map.Entry<String, BigDecimal> entry = entries.get(index);
                BigDecimal value = entry.getValue() == null ? BigDecimal.ZERO : entry.getValue();
                int barHeight = (int) Math.round(value.doubleValue() / maxValue * chartHeight);
                if (value.signum() > 0 && barHeight < 3) {
                    barHeight = 3;
                }
                int x = LEFT_MARGIN + index * slotWidth + (slotWidth - barWidth) / 2;
                int y = TOP_MARGIN + chartHeight - barHeight;

                Color base = Theme.CHART_COLORS[index % Theme.CHART_COLORS.length];
                g2.setPaint(new GradientPaint(x, y, base.brighter(), x, y + barHeight, base));
                g2.fillRoundRect(x, y, barWidth, Math.max(barHeight, 1), 6, 6);

                // Value above the bar.
                g2.setFont(valueFont);
                g2.setColor(Theme.TEXT);
                String valueText = UiUtils.formatCompactMoney(value);
                int valueWidth = g2.getFontMetrics().stringWidth(valueText);
                g2.drawString(valueText, x + (barWidth - valueWidth) / 2, Math.max(y - 6, 12));

                // Category label below the axis, ellipsised to the slot width.
                g2.setFont(labelFont);
                g2.setColor(Theme.MUTED);
                String label = ellipsise(g2, entry.getKey(), slotWidth - 6);
                int labelWidth = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, LEFT_MARGIN + index * slotWidth + (slotWidth - labelWidth) / 2,
                        TOP_MARGIN + chartHeight + 18);
            }
        } finally {
            g2.dispose();
        }
    }

    private void drawGrid(Graphics2D g2, int chartWidth, int chartHeight, BigDecimal maximum) {
        g2.setFont(Theme.FONT_SMALL.deriveFont(Font.PLAIN, 10f));
        g2.setStroke(new BasicStroke(1f));
        for (int step = 0; step <= Y_GRID_LINES; step++) {
            int y = TOP_MARGIN + chartHeight - chartHeight * step / Y_GRID_LINES;
            g2.setColor(new Color(0xE6EBF1));
            g2.drawLine(LEFT_MARGIN, y, LEFT_MARGIN + chartWidth, y);
            if (step > 0) {
                BigDecimal value = maximum.multiply(BigDecimal.valueOf(step))
                        .divide(BigDecimal.valueOf(Y_GRID_LINES), 0, java.math.RoundingMode.HALF_UP);
                g2.setColor(Theme.MUTED);
                String label = UiUtils.formatCompactMoney(value);
                FontMetrics metrics = g2.getFontMetrics();
                g2.drawString(label, LEFT_MARGIN - 8 - metrics.stringWidth(label), y + 4);
            }
        }
        g2.setColor(Theme.BORDER);
        BasicStroke axisStroke = new BasicStroke(1.4f);
        g2.setStroke(axisStroke);
        g2.drawLine(LEFT_MARGIN, TOP_MARGIN + chartHeight, LEFT_MARGIN + chartWidth,
                TOP_MARGIN + chartHeight);
    }

    private String ellipsise(Graphics2D g2, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        FontMetrics metrics = g2.getFontMetrics();
        if (metrics.stringWidth(text) <= maxWidth || maxWidth <= 0) {
            return text;
        }
        String candidate = text;
        while (candidate.length() > 1
                && metrics.stringWidth(candidate + "...") > maxWidth) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate + "...";
    }
}
