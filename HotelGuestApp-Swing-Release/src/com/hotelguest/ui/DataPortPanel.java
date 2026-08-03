package com.hotelguest.ui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.hotelguest.Main.store;

public class DataPortPanel extends JPanel {

    private final Runnable onImported;
    private final JLabel lblStats = new JLabel();

    public DataPortPanel(Runnable onImported) {
        this.onImported = onImported;
        setLayout(new BorderLayout());
        setBackground(UI.BG);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UI.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

        // Export
        JLabel expTitle = new JLabel("Export Data");
        expTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        expTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(expTitle);
        content.add(Box.createVerticalStrut(8));

        JTextArea expDesc = infoText(
            "Export all guest records, billing data, and history to a JSON file.\n" +
            "This file can be imported into the iOS version or any other computer\n" +
            "running this app.");
        content.add(expDesc);
        content.add(Box.createVerticalStrut(10));

        JButton exportBtn = UI.primaryButton("  Export to JSON…  ");
        exportBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportBtn.addActionListener(e -> doExport());
        content.add(exportBtn);
        content.add(Box.createVerticalStrut(6));
        content.add(infoText("Format: JSON v1  ·  Compatible with iOS and all desktop platforms."));
        content.add(Box.createVerticalStrut(24));
        content.add(new JSeparator()); content.add(Box.createVerticalStrut(24));

        // Import
        JLabel impTitle = new JLabel("Import Data");
        impTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        impTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(impTitle);
        content.add(Box.createVerticalStrut(8));

        JTextArea impDesc = infoText(
            "Import a JSON file exported from the iOS app or another computer.\n" +
            "⚠  This will OVERWRITE all current guest data on this computer.");
        content.add(impDesc);
        content.add(Box.createVerticalStrut(10));

        JButton importBtn = UI.dangerButton("  Import from JSON…  ");
        importBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        importBtn.addActionListener(e -> doImport());
        content.add(importBtn);
        content.add(Box.createVerticalStrut(24));
        content.add(new JSeparator()); content.add(Box.createVerticalStrut(24));

        // Stats
        JLabel statsTitle = new JLabel("Current Data");
        statsTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        statsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(statsTitle);
        content.add(Box.createVerticalStrut(8));
        lblStats.setFont(UI.FONT);
        lblStats.setForeground(UI.TEXT_MUTED);
        lblStats.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(lblStats);

        refreshStats();

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll);
    }

    private void doExport() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Guest Data");
        fc.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        fc.setSelectedFile(new File("HotelGuests_" +
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".json"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile();
        if (!file.getName().endsWith(".json")) file = new File(file.getPath() + ".json");
        try {
            store.exportToFile(file);
            JOptionPane.showMessageDialog(this,
                "Data exported to:\n" + file.getAbsolutePath() +
                "\n\n" + store.getGuests().size() + " guest record(s) saved.",
                "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doImport() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import Guest Data");
        fc.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile();
        int r = JOptionPane.showConfirmDialog(this,
            "Importing will replace ALL current guest records.\nThis cannot be undone.\n\nProceed?",
            "Confirm Import", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        try {
            store.importFromFile(file);
            refreshStats();
            if (onImported != null) onImported.run();
            JOptionPane.showMessageDialog(this,
                store.getGuests().size() + " guest record(s) imported successfully.",
                "Import Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Import failed. Make sure the file was exported from this app.\n\nError: " + ex.getMessage(),
                "Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshStats() {
        lblStats.setText(String.format(
            "Guests: %d      Total Revenue: $%.2f      Open Bills: $%.2f      Format: JSON v1",
            store.getGuests().size(), store.getTotalRevenue(), store.getTotalOpenBills()));
    }

    private JTextArea infoText(String text) {
        JTextArea ta = new JTextArea(text);
        ta.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        ta.setForeground(UI.TEXT_MUTED);
        ta.setBackground(UI.WHITE);
        ta.setEditable(false);
        ta.setFocusable(false);
        ta.setLineWrap(true); ta.setWrapStyleWord(true);
        ta.setAlignmentX(Component.LEFT_ALIGNMENT);
        return ta;
    }
}
