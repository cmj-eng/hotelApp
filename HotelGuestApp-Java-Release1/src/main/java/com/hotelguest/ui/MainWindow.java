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

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        GuestListTab guestList = new GuestListTab();
        AddEditGuestTab addGuest = new AddEditGuestTab(null, () -> guestList.refresh());
        FinanceTab finance = new FinanceTab();
        HistoryTab history = new HistoryTab();
        DataPortTab dataPort = new DataPortTab(() -> {
            guestList.refresh();
            finance.refresh();
            history.refresh();
        });

        Tab t1 = new Tab("👥  Guests",   guestList.getRoot());
        Tab t2 = new Tab("➕  New Guest", addGuest.getRoot());
        Tab t3 = new Tab("💰  Finance",   finance.getRoot());
        Tab t4 = new Tab("🕐  History",   history.getRoot());
        Tab t5 = new Tab("📂  Data",      dataPort.getRoot());

        tabs.getTabs().addAll(t1, t2, t3, t4, t5);

        // Refresh dependant tabs when switching to them
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> {
            if (now == t3) finance.refresh();
            if (now == t4) history.refresh();
        });

        Scene scene = new Scene(tabs, 1100, 720);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("Hotel Guest Manager");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(550);
        stage.show();
    }
}
