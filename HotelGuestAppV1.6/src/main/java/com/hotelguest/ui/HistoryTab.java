package com.hotelguest.ui;

import com.hotelguest.model.Guest;
import com.hotelguest.model.HistoryEvent;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.hotelguest.ui.MainWindow.store;

public class HistoryTab {

    private final VBox root = new VBox(0);
    private final TableView<FlatEvent> table = new TableView<>();
    private final TextField searchField = new TextField();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    record FlatEvent(String guestName, HistoryEvent event) {}

    @SuppressWarnings("unchecked")
    public HistoryTab() {
        searchField.setPromptText("Search events or guest name…");
        searchField.setPrefWidth(300);
        searchField.textProperty().addListener((o, ov, nv) -> refresh());

        HBox toolbar = new HBox(12, searchField);
        toolbar.setPadding(new Insets(12, 16, 12, 16));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<FlatEvent,String> cDate  = new TableColumn<>("Date & Time");
        cDate.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().event().getDate() != null ? cd.getValue().event().getDate().format(FMT) : ""));
        cDate.setPrefWidth(160);

        TableColumn<FlatEvent,String> cGuest = new TableColumn<>("Guest");
        cGuest.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().guestName()));
        cGuest.setPrefWidth(170);

        TableColumn<FlatEvent,String> cType  = new TableColumn<>("Type");
        cType.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().event().getType() != null ? cd.getValue().event().getType().name() : ""));
        cType.setPrefWidth(110);

        TableColumn<FlatEvent,String> cDesc  = new TableColumn<>("Description");
        cDesc.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().event().getDescription()));

        table.getColumns().addAll(cDate, cGuest, cType, cDesc);

        // Colour rows by type
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(FlatEvent fe, boolean empty) {
                super.updateItem(fe, empty);
                setStyle("");
                if (fe == null || empty || fe.event().getType() == null) return;
                switch (fe.event().getType()) {
                    case PAYMENT    -> setStyle("-fx-background-color: #e8f8f0;");
                    case CREATED    -> setStyle("-fx-background-color: #eaf4ff;");
                    case UPDATED    -> setStyle("-fx-background-color: #fefae0;");
                    case DELETED    -> setStyle("-fx-background-color: #fdecea;");
                    case CHECKED_IN -> setStyle("-fx-background-color: #f0ffe8;");
                    default         -> {}
                }
            }
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(toolbar, table);
        refresh();
    }

    public void refresh() {
        String search = searchField.getText().toLowerCase(Locale.ROOT);
        List<FlatEvent> events = new ArrayList<>();
        // Active guest history
        for (Guest g : store.getGuests()) {
            for (HistoryEvent ev : g.getHistory()) {
                if (search.isEmpty()
                    || g.getName().toLowerCase().contains(search)
                    || (ev.getDescription() != null && ev.getDescription().toLowerCase().contains(search))) {
                    events.add(new FlatEvent(g.getName(), ev));
                }
            }
        }
        // Deletion log — guests no longer in the list
        for (HistoryEvent ev : store.getDeletionLog()) {
            if (search.isEmpty()
                || (ev.getDescription() != null && ev.getDescription().toLowerCase().contains(search))) {
                events.add(new FlatEvent("⚠ Deleted Record", ev));
            }
        }
        events.sort(Comparator.comparing(fe -> fe.event().getDate(), Comparator.nullsLast(Comparator.reverseOrder())));
        table.setItems(FXCollections.observableArrayList(events));
    }

    public Node getRoot() { return root; }
}
