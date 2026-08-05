package com.hotelguest.ui;

import com.hotelguest.model.GuestStore;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
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

        GuestListTab  guestList = new GuestListTab();
        AddEditGuestTab addGuest = new AddEditGuestTab(null, () -> guestList.refresh());
        FinanceTab    finance   = new FinanceTab();
        HistoryTab    history   = new HistoryTab();
        SettingsTab   settings  = new SettingsTab();
        DataPortTab   dataPort  = new DataPortTab(() -> {
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
            // Refresh currency conversion in forms whenever the user navigates to them
            // (in case they changed currency in Settings)
            if (now == t2) addGuest.refreshCurrency();
            if (now == t1) guestList.refresh();
        });

        Scene scene = new Scene(tabs, 1150, 740);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("Hotel Guest Manager — Nduyaka");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(580);
        stage.show();
    }
}
