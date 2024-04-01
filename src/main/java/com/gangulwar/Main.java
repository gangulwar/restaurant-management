package com.gangulwar;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Restaurant Order");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        OrderPanel orderPanel = new OrderPanel();
        frame.getContentPane().add(orderPanel);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}


