package com.hotelguest.ui;

import com.hotelguest.model.AppSettings;
import com.hotelguest.model.Guest;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

import static com.hotelguest.ui.MainWindow.store;

public class SettingsTab {

    private final ScrollPane root;
    private final VBox content = new VBox(14);
    private final ComboBox<String> cbCurrency = new ComboBox<>();
    private final Label lblRatesUpdated = new Label("Not yet fetched");
    private final Map<String, TextField> roomFields = new LinkedHashMap<>();
    private final Map<String, TextField> carFields  = new LinkedHashMap<>();
    private final Map<String, TextField> bfFields   = new LinkedHashMap<>();

    public SettingsTab() {
        content.setPadding(new Insets(20));
        buildCurrencySection();
        buildRoomSection();
        buildCarSection();
        buildBreakfastSection();

        Button saveBtn = new Button("💾  Save Settings");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> saveAll());
        content.getChildren().add(saveBtn);

        root = new ScrollPane(content);
        root.setFitToWidth(true);
        populate();
    }

    private void buildCurrencySection() {
        content.getChildren().add(sectionLabel("Currency"));

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8);
        ColumnConstraints c1 = new ColumnConstraints(180);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        grid.addRow(0, new Label("Display Currency:"), cbCurrency);
        grid.addRow(1, new Label("Base Currency:"), new Label("ZMW (Zambian Kwacha) — all prices stored in ZMW"));
        grid.addRow(2, new Label("Rates last updated:"), lblRatesUpdated);

        Button refreshBtn = new Button("⟳  Refresh Exchange Rates");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> {
            refreshBtn.setDisable(true); refreshBtn.setText("Fetching…");
            new Thread(() -> {
                store.fetchExchangeRatesIfNeeded();
                Platform.runLater(() -> {
                    refreshBtn.setText("⟳  Refresh Exchange Rates");
                    refreshBtn.setDisable(false);
                    lblRatesUpdated.setText(store.getSettings().getRatesUpdatedAt() != null
                        ? store.getSettings().getRatesUpdatedAt() : "Updated");
                    populateCurrencyCombo();
                });
            }).start();
        });
        grid.addRow(3, new Label(), refreshBtn);
        content.getChildren().add(grid);
    }

    private void buildRoomSection() {
        content.getChildren().add(sectionLabel("Standard Room Rates (ZMW / night)"));
        GridPane grid = rateGrid();
        int row = 0;
        for (Guest.RoomType rt : Guest.RoomType.values()) {
            TextField tf = new TextField(); tf.setPrefWidth(100);
            roomFields.put(rt.getLabel(), tf);
            grid.addRow(row++, new Label(rt.getLabel()), new Label("K"), tf);
        }
        content.getChildren().add(grid);
    }

    private void buildCarSection() {
        content.getChildren().add(sectionLabel("Standard Car Rates (ZMW / day)"));
        GridPane grid = rateGrid();
        int row = 0;
        for (Guest.CarType ct : Guest.CarType.values()) {
            TextField tf = new TextField(); tf.setPrefWidth(100);
            carFields.put(ct.getLabel(), tf);
            grid.addRow(row++, new Label(ct.getLabel()), new Label("K"), tf);
        }
        content.getChildren().add(grid);
    }

    private void buildBreakfastSection() {
        content.getChildren().add(sectionLabel("Breakfast Prices (ZMW / person / night)"));
        GridPane grid = rateGrid();
        int row = 0;
        for (Guest.BreakfastType bt : Guest.BreakfastType.values()) {
            if (bt == Guest.BreakfastType.NONE) continue;
            TextField tf = new TextField(); tf.setPrefWidth(100);
            bfFields.put(bt.getLabel(), tf);
            grid.addRow(row++, new Label(bt.getLabel()), new Label("K"), tf);
        }
        content.getChildren().add(grid);
    }

    private GridPane rateGrid() {
        GridPane g = new GridPane(); g.setHgap(8); g.setVgap(6);
        ColumnConstraints c1 = new ColumnConstraints(140);
        ColumnConstraints c2 = new ColumnConstraints(20);
        ColumnConstraints c3 = new ColumnConstraints(100);
        g.getColumnConstraints().addAll(c1, c2, c3);
        return g;
    }

    private void populateCurrencyCombo() {
        String prev = cbCurrency.getValue();
        cbCurrency.getItems().clear();
        Set<String> currencies = new LinkedHashSet<>();
        currencies.add("ZMW"); currencies.add("EUR"); currencies.add("USD");
        currencies.add("GBP"); currencies.add("ZAR");
        currencies.addAll(store.getSettings().supportedCurrencies());
        cbCurrency.getItems().addAll(currencies);
        cbCurrency.setValue(prev != null && currencies.contains(prev)
            ? prev : store.getSettings().getDisplayCurrency());
    }

    private void populate() {
        AppSettings s = store.getSettings();
        populateCurrencyCombo();
        if (s.getRatesUpdatedAt() != null) lblRatesUpdated.setText(s.getRatesUpdatedAt());
        s.getRoomRates().forEach((k,v) -> { if (roomFields.containsKey(k)) roomFields.get(k).setText(String.format("%.2f",v)); });
        s.getCarRates().forEach((k,v)  -> { if (carFields.containsKey(k))  carFields.get(k).setText(String.format("%.2f",v)); });
        s.getBreakfastPrices().forEach((k,v) -> { if (bfFields.containsKey(k)) bfFields.get(k).setText(String.format("%.2f",v)); });
    }

    private void saveAll() {
        AppSettings s = store.getSettings();
        s.setDisplayCurrency(cbCurrency.getValue());
        roomFields.forEach((k,tf) -> { try { s.getRoomRates().put(k, Double.parseDouble(tf.getText().replace(",","."))); } catch (Exception ignored){} });
        carFields.forEach((k,tf)  -> { try { s.getCarRates().put(k,  Double.parseDouble(tf.getText().replace(",","."))); } catch (Exception ignored){} });
        bfFields.forEach((k,tf)   -> { try { s.getBreakfastPrices().put(k, Double.parseDouble(tf.getText().replace(",","."))); } catch (Exception ignored){} });
        store.saveSettings();
        new Alert(Alert.AlertType.INFORMATION, "Settings saved.", ButtonType.OK).showAndWait();
    }

    private Label sectionLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#2c3e50;-fx-padding:10 0 4 0;");
        return l;
    }

    public void refresh() { populate(); }
    public Node getRoot() { return root; }
}
