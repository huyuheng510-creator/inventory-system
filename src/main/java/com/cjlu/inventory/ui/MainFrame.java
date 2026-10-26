package com.cjlu.inventory.ui;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.model.User;
import com.cjlu.inventory.ui.component.StatusBar;
import com.cjlu.inventory.ui.controller.GuiStockObserver;
import com.cjlu.inventory.ui.controller.MainController;
import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;
import com.cjlu.inventory.ui.view.AdministrationPanel;
import com.cjlu.inventory.ui.view.DashboardPanel;
import com.cjlu.inventory.ui.view.ProductPanel;
import com.cjlu.inventory.ui.view.PurchaseOrderPanel;
import com.cjlu.inventory.ui.view.ReportsPanel;
import com.cjlu.inventory.ui.view.StockPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main application window: branded header with the signed-in user, the
 * role-dependent tabbed workspace, the status bar and the registered stock
 * observer.
 *
 * <p>The window title is fixed by the project specification. Closing the
 * window asks for confirmation, shuts the embedded database down and exits the
 * JVM.</p>
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final ApplicationContext context;
    private final DashboardPanel dashboardPanel;
    private final ProductPanel productPanel;
    private final StockPanel stockPanel;
    private final PurchaseOrderPanel purchaseOrderPanel;
    private final ReportsPanel reportsPanel;
    private final AdministrationPanel administrationPanel;
    private final JTabbedPane tabs = new JTabbedPane();
    private final StatusBar statusBar = new StatusBar();
    private final MainController controller;
    private final GuiStockObserver stockObserver;

    public MainFrame(ApplicationContext context) {
        super("StockFlow Inventory Management System - Project 2");
        Theme.install();
        this.context = context;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1280, 800);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BACKGROUND);

        dashboardPanel = new DashboardPanel(context);
        productPanel = new ProductPanel(context);
        stockPanel = new StockPanel(context);
        purchaseOrderPanel = new PurchaseOrderPanel(context);
        reportsPanel = new ReportsPanel(context);
        administrationPanel = context.getAuthService().isAdmin()
                ? new AdministrationPanel(context)
                : null;

        controller = new MainController(context, this);

        buildMenuBar();
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        statusBar.setMessage("Welcome to StockFlow. Use the tabs to manage the inventory.");
        statusBar.setInfo(buildUserInfo());

        stockObserver = new GuiStockObserver(
                (message, color) -> statusBar.setMessage(message, color),
                this::refreshAfterStockChange);
        context.getStockService().addObserver(stockObserver);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                controller.exitApplication();
            }
        });
    }

    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem changePasswordItem = new JMenuItem("Change Password...");
        changePasswordItem.addActionListener(action -> controller.changePassword());
        JMenuItem signOutItem = new JMenuItem("Sign Out");
        signOutItem.addActionListener(action -> controller.signOut());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(action -> controller.exitApplication());
        fileMenu.add(changePasswordItem);
        fileMenu.addSeparator();
        fileMenu.add(signOutItem);
        fileMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About StockFlow");
        aboutItem.addActionListener(action -> controller.showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(true);
        header.setBackground(Theme.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 16));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel appName = new JLabel("StockFlow");
        appName.setFont(Theme.FONT_BASE.deriveFont(Font.BOLD, 22f));
        appName.setForeground(Color.WHITE);
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("Inventory Management System - Project 2");
        subtitle.setFont(Theme.FONT_SMALL);
        subtitle.setForeground(new Color(0xC7D8E8));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(appName);
        titles.add(Box.createVerticalStrut(2));
        titles.add(subtitle);
        header.add(titles, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        User user = context.getAuthService().getCurrentUser();
        JLabel userLabel = new JLabel(user == null ? "Not signed in" : user.getDisplayName());
        userLabel.setFont(Theme.FONT_BOLD);
        userLabel.setForeground(Color.WHITE);
        userLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel roleLabel = new JLabel(user == null ? ""
                : user.getRole().getLabel() + "  -  " + user.getUsername());
        roleLabel.setFont(Theme.FONT_SMALL);
        roleLabel.setForeground(new Color(0xC7D8E8));
        roleLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        buttons.setOpaque(false);
        JButton changePasswordButton = UiUtils.toolbarButton("Change Password");
        JButton signOutButton = UiUtils.toolbarButton("Sign Out");
        JButton exitButton = UiUtils.toolbarButton("Exit");
        changePasswordButton.addActionListener(action -> controller.changePassword());
        signOutButton.addActionListener(action -> controller.signOut());
        exitButton.addActionListener(action -> controller.exitApplication());
        buttons.add(changePasswordButton);
        buttons.add(signOutButton);
        buttons.add(exitButton);

        right.add(userLabel);
        right.add(roleLabel);
        right.add(Box.createVerticalStrut(2));
        right.add(buttons);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane buildTabs() {
        tabs.setFont(Theme.FONT_BOLD);
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 14, 0, 14));
        tabs.addTab("Dashboard", dashboardPanel);
        tabs.addTab("Products", productPanel);
        tabs.addTab("Stock", stockPanel);
        tabs.addTab("Purchase Orders", purchaseOrderPanel);
        tabs.addTab("Reports", reportsPanel);
        if (administrationPanel != null) {
            tabs.addTab("Administration", administrationPanel);
        }
        tabs.addChangeListener(event -> refreshSelectedTab());
        return tabs;
    }

    private String buildUserInfo() {
        User user = context.getAuthService().getCurrentUser();
        String role = user == null ? "No session" : user.getRole().getLabel();
        return "Embedded Apache Derby  |  " + role
                + "  |  Press F10 to open the menu";
    }

    /** Refreshes the currently visible tab so that cross-tab changes are visible. */
    private void refreshSelectedTab() {
        Component selected = tabs.getSelectedComponent();
        if (selected == null) {
            return;
        }
        if (selected == dashboardPanel) {
            dashboardPanel.refresh();
        } else if (selected == productPanel) {
            productPanel.refresh();
        } else if (selected == stockPanel) {
            stockPanel.refresh();
        } else if (selected == purchaseOrderPanel) {
            purchaseOrderPanel.refresh();
        } else if (selected == reportsPanel) {
            reportsPanel.refresh();
        } else if (administrationPanel != null && selected == administrationPanel) {
            administrationPanel.refresh();
        }
    }

    /** Observer callback: refresh the dashboard and the product table. */
    private void refreshAfterStockChange() {
        try {
            dashboardPanel.refresh();
            productPanel.refresh();
        } catch (RuntimeException ignored) {
            // The panels already report their own failures; never break the EDT.
        }
    }

    /** Stops the registered stock observer (called before logout or exit). */
    public void deactivateObserver() {
        stockObserver.deactivate();
    }

    /** Shows a status-bar message. */
    public void setStatus(String message) {
        statusBar.setMessage(message);
    }

    /** Shows a coloured status-bar message. */
    public void setStatus(String message, Color color) {
        statusBar.setMessage(message, color);
    }
}
