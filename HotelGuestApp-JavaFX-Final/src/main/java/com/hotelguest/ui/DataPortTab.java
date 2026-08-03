package com.hotelguest.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.hotelguest.ui.MainWindow.store;

public class DataPortTab {

    private final VBox root = new VBox(24);
    private final Runnable onImported;
    private final Label lblStats = new Label();

    public DataPortTab(Runnable onImported) {
        this.onImported = onImported;
        root.setPadding(new Insets(30));
        root.setMaxWidth(680);

        buildExportSection();
        buildImportSection();
        buildStatsSection();
        refreshStats();
    }

    private void buildExportSection() {
        Label title = sectionTitle("📤  Export Data");
        Label desc = new Label(
            "Export all guest records, billing data, and history to a JSON file.\n" +
            "This file is cross-platform and can be imported on macOS, Windows, or iOS.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #555; -fx-font-size: 13px;");

        Button exportBtn = new Button("Export to JSON…");
        exportBtn.getStyleClass().add("btn-primary");
        exportBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Export Guest Data");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            fc.setInitialFileName("HotelGuests_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".json");
            File file = fc.showSaveDialog(root.getScene().getWindow());
            if (file == null) return;
            try {
                store.exportToFile(file);
                showInfo("Export Successful",
                    "Data exported to:\n" + file.getAbsolutePath() +
                    "\n\n" + store.getGuests().size() + " guest record(s) saved.");
            } catch (Exception ex) {
                showError("Export Failed", ex.getMessage());
            }
        });

        Label note = new Label("ℹ Format: JSON v1  ·  Compatible with iOS and desktop versions");
        note.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        root.getChildren().addAll(title, desc, exportBtn, note);
    }

    private void buildImportSection() {
        Separator sep = new Separator();

        Label title = sectionTitle("📥  Import Data");
        Label desc = new Label(
            "Import a JSON file exported from any version of this app (iOS or desktop).\n" +
            "⚠ This will OVERWRITE all current data on this computer.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #555; -fx-font-size: 13px;");

        Button importBtn = new Button("Import from JSON…");
        importBtn.getStyleClass().add("btn-danger");
        importBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Import Guest Data");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            File file = fc.showOpenDialog(root.getScene().getWindow());
            if (file == null) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Importing will replace ALL existing guest records.\nThis cannot be undone.\n\nProceed?",
                ButtonType.YES, ButtonType.CANCEL);
            confirm.setTitle("Confirm Import");
            confirm.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> {
                try {
                    store.importFromFile(file);
                    refreshStats();
                    if (onImported != null) onImported.run();
                    showInfo("Import Successful",
                        store.getGuests().size() + " guest record(s) imported from:\n" + file.getAbsolutePath());
                } catch (Exception ex) {
                    showError("Import Failed",
                        "Could not parse the file. Make sure it was exported from this app.\n\nError: " + ex.getMessage());
                }
            });
        });

        root.getChildren().addAll(sep, title, desc, importBtn);
    }

    private void buildStatsSection() {
        Separator sep = new Separator();
        Label title = sectionTitle("ℹ  Current Data Summary");
        root.getChildren().addAll(sep, title, lblStats);
    }

    private void refreshStats() {
        lblStats.setText(String.format(
            "Guests: %d     Total Revenue: $%.2f     Open Bills: $%.2f     Format: JSON v1",
            store.getGuests().size(), store.getTotalRevenue(), store.getTotalOpenBills()));
        lblStats.setStyle("-fx-text-fill: #444; -fx-font-size: 13px;");
    }

    private Label sectionTitle(String t) {
        Label l = new Label(t);
        l.setFont(Font.font("System", FontWeight.BOLD, 16));
        return l;
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }

    public Node getRoot() { return root; }
}
