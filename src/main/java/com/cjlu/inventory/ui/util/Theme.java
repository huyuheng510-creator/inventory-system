package com.cjlu.inventory.ui.util;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

/**
 * Central visual theme of the Swing GUI: colours, fonts and the one-time
 * Look-and-Feel installation.
 *
 * <p>The application uses the Nimbus Look-and-Feel when it is available and
 * falls back to the platform Look-and-Feel otherwise. All views read their
 * colours and fonts from this class so that the application keeps a single,
 * consistent visual identity (NFR: usability / consistent presentation).</p>
 */
public final class Theme {

    /** Primary brand colour (header, titles). */
    public static final Color PRIMARY = new Color(0x1F4E79);
    /** Lighter brand colour used for accents and selection. */
    public static final Color PRIMARY_LIGHT = new Color(0x2E75B6);
    /** Very light blue used behind tables and forms. */
    public static final Color BACKGROUND = new Color(0xF2F5F9);
    /** Card / table background. */
    public static final Color CARD = Color.WHITE;
    /** Border colour for cards and tables. */
    public static final Color BORDER = new Color(0xD5DDE5);
    /** Main text colour. */
    public static final Color TEXT = new Color(0x1F2933);
    /** Secondary / hint text colour. */
    public static final Color MUTED = new Color(0x5F6B7A);
    /** Positive colour (in stock, success messages). */
    public static final Color SUCCESS = new Color(0x2E7D32);
    /** Warning colour (low stock). */
    public static final Color WARNING = new Color(0xB26A00);
    /** Danger colour (out of stock, errors). */
    public static final Color DANGER = new Color(0xC62828);
    /** Alternating table row colour. */
    public static final Color TABLE_ROW_ALT = new Color(0xF2F6FA);
    /** Status bar background. */
    public static final Color STATUS_BAR = new Color(0xE8EDF3);

    /** Palette used by the bar chart. */
    public static final Color[] CHART_COLORS = {
            new Color(0x1F4E79),
            new Color(0x2E75B6),
            new Color(0x4E9BD6),
            new Color(0x70B8E8),
            new Color(0x8CC7B5),
            new Color(0xC9A227)
    };

    public static final Font FONT_BASE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = FONT_BASE.deriveFont(Font.PLAIN, 11.5f);
    public static final Font FONT_BOLD = FONT_BASE.deriveFont(Font.BOLD);
    public static final Font FONT_SMALL_BOLD = FONT_SMALL.deriveFont(Font.BOLD);
    public static final Font FONT_SECTION = FONT_BASE.deriveFont(Font.BOLD, 15f);
    public static final Font FONT_TITLE = FONT_BASE.deriveFont(Font.BOLD, 20f);
    public static final Font FONT_HEADER = FONT_BASE.deriveFont(Font.BOLD, 24f);
    public static final Font FONT_KPI = FONT_BASE.deriveFont(Font.BOLD, 25f);

    private static boolean installed;

    private Theme() {
        // utility class
    }

    /** Installs Nimbus (falling back to the system theme) and the base fonts. */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception nimbusMissing) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception systemMissing) {
                // Keep the default cross-platform Look-and-Feel.
            }
        }
        installFonts();
        installed = true;
    }

    private static void installFonts() {
        put("Label.font", FONT_BASE);
        put("Button.font", FONT_BASE);
        put("ToggleButton.font", FONT_BASE);
        put("CheckBox.font", FONT_BASE);
        put("RadioButton.font", FONT_BASE);
        put("TextField.font", FONT_BASE);
        put("PasswordField.font", FONT_BASE);
        put("FormattedTextField.font", FONT_BASE);
        put("TextArea.font", FONT_BASE);
        put("TextPane.font", FONT_BASE);
        put("ComboBox.font", FONT_BASE);
        put("List.font", FONT_BASE);
        put("Table.font", FONT_BASE);
        put("TableHeader.font", FONT_SMALL_BOLD);
        put("TabbedPane.font", FONT_BOLD);
        put("TitledBorder.font", FONT_BOLD);
        put("Spinner.font", FONT_BASE);
        put("MenuBar.font", FONT_BASE);
        put("Menu.font", FONT_BASE);
        put("MenuItem.font", FONT_BASE);
        put("OptionPane.messageFont", FONT_BASE);
        put("OptionPane.buttonFont", FONT_BASE);
        put("ToolTip.font", FONT_SMALL);
        put("TableHeader.background", CARD);
        put("TableHeader.foreground", PRIMARY);
        put("Table.selectionBackground", new Color(0xD6E6F5));
        put("Table.selectionForeground", TEXT);
        put("ScrollPane.background", CARD);
        put("Viewport.background", CARD);
    }

    private static void put(String key, Object value) {
        UIManager.put(key, value);
    }
}
