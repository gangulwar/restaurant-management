package com.gangulwar;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class OrderPanel extends JPanel {
    private JComboBox<String> selectProduct;
    private JSpinner spinner;
    private JButton addButton;
    private JTable orderTable;
    private JLabel perProductPrice;
    private Connection con;

    public OrderPanel() {
        setLayout(new BorderLayout());

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1522:xe", "system", "system");
            System.out.println("Connected to database.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel addProductPanel = new JPanel(new GridBagLayout());
        addProductPanel.setBorder(BorderFactory.createTitledBorder("Add Product"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel productLabel = new JLabel("Product:");
        selectProduct = new JComboBox<>();
        populateProductDropdown();

        JLabel countLabel = new JLabel("Count:");
        spinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));

        addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addProduct();
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        addProductPanel.add(productLabel, gbc);
        gbc.gridx = 1;
        addProductPanel.add(selectProduct, gbc);
        gbc.gridx = 2;
        addProductPanel.add(new JPanel(), gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        addProductPanel.add(countLabel, gbc);
        gbc.gridx = 1;
        addProductPanel.add(spinner, gbc);
        gbc.gridx = 2;
        addProductPanel.add(new JPanel(), gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        addProductPanel.add(addButton, gbc);

        tabbedPane.addTab("Add Product", addProductPanel);

        JPanel addNewItemPanel = new JPanel(new BorderLayout());
        tabbedPane.addTab("Add New Item", addNewItemPanel);


        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (tabbedPane.getSelectedIndex() == 1) {
                    addNewItem();
                    tabbedPane.setSelectedIndex(0);
                }
            }
        });

        add(tabbedPane, BorderLayout.NORTH);

        JPanel currentOrderPanel = new JPanel(new BorderLayout());
        currentOrderPanel.setBorder(BorderFactory.createTitledBorder("Current Orders"));

        DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Product Name", "Count", "Price"}, 0);
        orderTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(orderTable);
        currentOrderPanel.add(scrollPane, BorderLayout.CENTER);

        perProductPrice = new JLabel("");
        currentOrderPanel.add(perProductPrice, BorderLayout.SOUTH);

        add(currentOrderPanel, BorderLayout.CENTER);

        JButton placeOrderButton = new JButton("Place Order");
        placeOrderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placeOrder();
            }
        });

        add(placeOrderButton, BorderLayout.SOUTH);
    }

    private void populateProductDropdown() {
        try {
            String query = "SELECT ITEM FROM items";
            PreparedStatement pst = con.prepareStatement(query);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                selectProduct.addItem(rs.getString("ITEM"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addProduct() {
        String product = (String) selectProduct.getSelectedItem();
        int count = (int) spinner.getValue();
        double price = getProductPrice(product) * count;
        updateOrderTable(product, count, price);
    }

    private double getProductPrice(String productName) {
        double price = 0.0;
        try {
            String query = "SELECT PRICE FROM items WHERE ITEM = ?";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, productName);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                price = rs.getDouble("PRICE");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return price;
    }

    private void updateOrderTable(String product, int count, double price) {
        DefaultTableModel model = (DefaultTableModel) orderTable.getModel();
        model.addRow(new Object[]{product, count, price});
        updateTotalPrice();
    }

    private void updateTotalPrice() {
        double totalPrice = 0.0;
        DefaultTableModel model = (DefaultTableModel) orderTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            totalPrice += (double) model.getValueAt(i, 2);
        }
        perProductPrice.setText("Total Price: ₹" + totalPrice);
    }

    private void addNewItem() {
        JTextField itemNameField = new JTextField();
        JTextField itemPriceField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Item Name:"));
        panel.add(itemNameField);
        panel.add(new JLabel("Item Price:"));
        panel.add(itemPriceField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Add New Item", JOptionPane.OK_CANCEL_OPTION);

        try {
            if (result == JOptionPane.OK_OPTION) {
                String itemName = itemNameField.getText();
                double itemPrice = Double.parseDouble(itemPriceField.getText());
                insertNewItem(itemName, itemPrice);
                selectProduct.addItem(itemName);
            }
        } catch (NumberFormatException e) {
            System.out.println("Exception: No input given");
        }

    }

    private void insertNewItem(String itemName, double itemPrice) {
        try {
            String query = "INSERT INTO items (ITEM, PRICE) VALUES (?, ?)";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, itemName);
            pst.setDouble(2, itemPrice);
            pst.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void placeOrder() {
        double totalPrice = calculateTotalPrice();
        JOptionPane.showMessageDialog(null, "Total Bill: ₹" + totalPrice, "Order Placed", JOptionPane.INFORMATION_MESSAGE);
        resetOrder();
    }

    private double calculateTotalPrice() {
        double totalPrice = 0.0;
        DefaultTableModel model = (DefaultTableModel) orderTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            totalPrice += (double) model.getValueAt(i, 2);
        }
        return totalPrice;
    }

    private void resetOrder() {
        DefaultTableModel model = (DefaultTableModel) orderTable.getModel();
        model.setRowCount(0);
        perProductPrice.setText("");
    }
}
