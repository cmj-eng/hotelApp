package com.hotelguest;

import com.hotelguest.model.GuestStore;
import com.hotelguest.ui.MainFrame;

import javax.swing.*;

public class Main {
    public static GuestStore store;

    public static void main(String[] args) {
        // Use system look and feel (Aqua on macOS, Windows L&F on Windows)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            store = new GuestStore();
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
