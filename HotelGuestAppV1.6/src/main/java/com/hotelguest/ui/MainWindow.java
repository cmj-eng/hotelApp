package com.hotelguest.ui;

import com.hotelguest.model.GuestStore;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MainWindow extends Application {

    public static GuestStore store;

    @Override
    public void start(Stage stage) {
        store = new GuestStore();

        // App icon
        try {
            Image icon = new Image(getClass().getResourceAsStream("/icon.png"));
            stage.getIcons().add(icon);
        } catch (Exception ignored) {}

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        GuestListTab    guestList = new GuestListTab();
        AddEditGuestTab addGuest  = new AddEditGuestTab(null, () -> guestList.refresh());
        FinanceTab      finance   = new FinanceTab();
        HistoryTab      history   = new HistoryTab();
        SettingsTab     settings  = new SettingsTab();
        DataPortTab     dataPort  = new DataPortTab(() -> {
            guestList.refresh(); finance.refresh(); history.refresh(); settings.refresh();
        });

        Tab t1 = new Tab("👥  Guests",    guestList.getRoot());
        Tab t2 = new Tab("➕  New Guest",  addGuest.getRoot());
        Tab t3 = new Tab("💰  Finance",    finance.getRoot());
        Tab t4 = new Tab("🕐  History",    history.getRoot());
        Tab t5 = new Tab("⚙  Settings",   settings.getRoot());
        Tab t6 = new Tab("📂  Data",       dataPort.getRoot());

        tabs.getTabs().addAll(t1, t2, t3, t4, t5, t6);

        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> {
            if (now == t3) finance.refresh();
            if (now == t4) history.refresh();
            if (now == t5) settings.refresh();
            if (now == t2) addGuest.refreshCurrency();
            if (now == t1) guestList.refresh();
        });

        // ── Sync status bar ────────────────────────────────────────────────────
        Label syncLabel = new Label("Sync: ready");
        syncLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#888;");
        HBox statusBar = new HBox(syncLabel);
        statusBar.setPadding(new Insets(3, 12, 3, 12));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color:#f5f5f5;-fx-border-color:#ddd;-fx-border-width:1 0 0 0;");

        // Wire the store sync listener to update the status bar label
        store.setSyncStatusListener(msg -> {
            syncLabel.setText("Sync: " + msg);
            // Fade back to "ready" after 5 seconds
            new Thread(() -> {
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> syncLabel.setText("Sync: ready"));
            }).start();
        });

        BorderPane root = new BorderPane();
        root.setCenter(tabs);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1200, 760);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("Hotel Guest Manager — Nduyaka");
        stage.setScene(scene);
        stage.setMinWidth(940);
        stage.setMinHeight(600);
        stage.show();
    }
}
